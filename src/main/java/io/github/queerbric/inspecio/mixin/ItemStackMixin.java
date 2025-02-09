/*
 * Copyright (c) 2020 LambdAurora <email@lambdaurora.dev>, Emi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.queerbric.inspecio.mixin;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.tooltip.*;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract int getBaseRepairCost();

	@Shadow
	public abstract Item getItem();

	@Shadow
	@Nullable
	public abstract CompoundTag getTag();

	@Unique
	private final ThreadLocal<List<Component>> inspecio$tooltipList = new ThreadLocal<>();

	@Inject(
			method = "getTooltipLines",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasCustomHoverName()Z"),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onGetTooltipBeing(Player player, TooltipFlag context, CallbackInfoReturnable<List<Component>> cir, List<Component> list) {
		this.inspecio$tooltipList.set(list);
	}

	@Inject(
			method = "getTooltipLines",
			at = @At(value = "RETURN")
	)
	private void onGetTooltip(Player player, TooltipFlag context, CallbackInfoReturnable<List<Component>> cir) {
		var tooltip = this.inspecio$tooltipList.get();
		InspecioConfig.AdvancedTooltipsConfig advancedTooltipsConfig = Inspecio.getConfig().getAdvancedTooltipsConfig();

		if (advancedTooltipsConfig.hasLodestoneCoords() && this.getItem() instanceof CompassItem && CompassItem.isLodestoneCompass((ItemStack) (Object) this)) {
			var nbt = this.getTag();
			assert nbt != null; // Should not be null since hasLodestone returns true.

			GlobalPos globalPos = CompassItem.getLodestonePosition(nbt);

			if (globalPos != null) {
				BlockPos pos = globalPos.pos();
				var posText = Component.literal(String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()))
						.withStyle(ChatFormatting.GOLD);

				tooltip.add(Component.translatable("inspecio.tooltip.lodestone_compass.target", posText).withStyle(ChatFormatting.GRAY));
				tooltip.add(Component.translatable("inspecio.tooltip.lodestone_compass.dimension",
								Component.literal(globalPos.dimension().location().toString()).withStyle(ChatFormatting.GOLD))
						.withStyle(ChatFormatting.GRAY));
			}
		}

		int repairCost;
		if (advancedTooltipsConfig.hasRepairCost() && (repairCost = this.getBaseRepairCost()) != 0) {
			tooltip.add(Component.translatable("inspecio.tooltip.repair_cost", repairCost)
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void getTooltipData(CallbackInfoReturnable<Optional<TooltipComponent>> info) {
		// Data is the plural and datum is the singular actually, but no one cares
		var datas = new ArrayList<TooltipComponent>();
		info.getReturnValue().ifPresent(datas::add);

		var config = Inspecio.getConfig();
		var stack = (ItemStack) (Object) this;

		if (stack.isEdible()) {
			var comp = stack.getItem().getFoodProperties();

			if (config.getFoodConfig().isEnabled()) {
				datas.add(new FoodTooltipComponent(comp));
			}

			if (config.getEffectsConfig().hasPotions()) {
				if (stack.is(Inspecio.HIDDEN_EFFECTS_TAG)) {
					datas.add(new StatusEffectTooltipComponent());
				} else {
					if (comp.getEffects().size() > 0) {
						datas.add(new StatusEffectTooltipComponent(comp.getEffects()));
					} else if (stack.getItem() instanceof SuspiciousStewItem) {
						var effects = new ArrayList<MobEffectInstance>();
						SuspiciousStewItemAccessor.invokeConsumeStatusEffects(stack, effects::add);

						if (effects.size() != 0) {
							datas.add(new StatusEffectTooltipComponent(effects, 1.f));
						}
					} else {
						datas.add(new StatusEffectTooltipComponent(PotionUtils.getMobEffects(stack), 1.f));
					}
				}
			}
		}

		if (stack.getItem() instanceof ArmorItem) {
			ArmorTooltipComponent.of(stack).ifPresent(datas::add);
		}

		if (stack.getItem() instanceof HangingEntityItem) {
			PaintingTooltipComponent.of(stack).ifPresent(datas::add);
		}

		if (datas.size() == 1) {
			info.setReturnValue(Optional.of(datas.get(0)));
		} else if (datas.size() > 1) {
			var comp = new CompoundTooltipComponent();
			for (var data : datas) {
				ClientTooltipComponent component = TooltipComponentCallback.EVENT.invoker().getComponent(data);
				if (component != null)
					comp.addComponent(component);
			}
			info.setReturnValue(Optional.of(comp));
		}
	}
}
