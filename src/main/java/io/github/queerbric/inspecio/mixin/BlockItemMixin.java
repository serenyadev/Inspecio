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
import io.github.queerbric.inspecio.api.InventoryProvider;
import io.github.queerbric.inspecio.tooltip.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
	@Shadow
	public abstract Block getBlock();

	public BlockItemMixin(Properties settings) {
		super(settings);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		var inspecioConfig = Inspecio.getConfig();
		var containersConfig = inspecioConfig.getContainersConfig();
		var effectsConfig = inspecioConfig.getEffectsConfig();

		if (effectsConfig.hasBeacon() && this.getBlock() instanceof BeaconBlock) {
			var blockEntityTag = BlockItem.getBlockEntityData(stack);
			var effectsList = new ArrayList<MobEffectInstance>();
			var primary = Inspecio.getRawEffectFromTag(blockEntityTag, "Primary");
			var secondary = Inspecio.getRawEffectFromTag(blockEntityTag, "Secondary");

			if (primary != null && primary.equals(secondary)) {
				primary = new MobEffectInstance(primary.getEffect(), 200, 1);
				secondary = null;
			}
			if (primary != null)
				effectsList.add(primary);
			if (secondary != null)
				effectsList.add(secondary);

			return Optional.of(new StatusEffectTooltipComponent(effectsList, 1F));
		} else if (this.getBlock() instanceof BeehiveBlock) {
			var data = BeesTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof CampfireBlock) {
			var data = CampfireTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof JukeboxBlock) {
			var data = JukeboxTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof SpawnerBlock) {
			var data = SpawnEntityTooltipComponent.ofMobSpawner(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof ChiseledBookShelfBlock) {
			var data = ChiseledBookshelfTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else {
			InspecioConfig.StorageContainerConfig config = containersConfig.forBlock(this.getBlock());
			InventoryProvider.Context context = InventoryProvider.searchInventoryContextOf(stack, config);

			if (config == null) {
				config = containersConfig.getStorageConfig();
			}

			if (context != null) {
				return InventoryTooltipComponent.of(stack, config.isCompact(), context);
			}
		}

		return super.getTooltipImage(stack);
	}

	@Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
	private void onAppendTooltip(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo ci) {
		if (this.getBlock() instanceof ShulkerBoxBlock && !Screen.hasControlDown()) {
			Inspecio.appendBlockItemTooltip(stack, this.getBlock(), tooltip);
			ci.cancel();
		}
	}

	@Inject(method = "appendHoverText", at = @At("TAIL"))
	private void onAppendTooltipEnd(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo ci) {
		Inspecio.appendBlockItemTooltip(stack, this.getBlock(), tooltip);
	}
}
