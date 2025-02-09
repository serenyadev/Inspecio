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
import io.github.queerbric.inspecio.tooltip.StatusEffectTooltipComponent;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.fabricmc.api.EnvType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(PotionItem.class)
public abstract class PotionItemMixin extends Item {
	@Unique
	private final ThreadLocal<Integer> inspecio$oldTooltipLength = new ThreadLocal<>(); // ThreadLocal as REI workaround

	public PotionItemMixin(Properties settings) {
		super(settings);
	}

	@Inject(method = "appendHoverText", at = @At("HEAD"))
	private void onAppendTooltipPre(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo ci) {
		this.inspecio$oldTooltipLength.set(tooltip.size());
	}

	@Inject(method = "appendHoverText", at = @At("RETURN"))
	private void onAppendTooltipPost(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo info) {
		if (Inspecio.getConfig().getEffectsConfig().hasPotions() && !PotionUtils.getMobEffects(stack).isEmpty()) {
			Inspecio.removeVanillaTooltips(tooltip, this.inspecio$oldTooltipLength.get());
		}
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (!Inspecio.getConfig().getEffectsConfig().hasPotions()) return super.getTooltipImage(stack);
		return Optional.of(new StatusEffectTooltipComponent(PotionUtils.getMobEffects(stack), 1.f));
	}
}
