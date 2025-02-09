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

import io.github.queerbric.inspecio.tooltip.SignTooltipComponent;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.fabricmc.api.EnvType;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(value = {SignItem.class, HangingSignItem.class})
public class SignItemMixin extends StandingAndWallBlockItem {
	public SignItemMixin(Block standingBlock, Block wallBlock, Properties settings, Direction direction) {
		super(standingBlock, wallBlock, settings, direction);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		return SignTooltipComponent.fromItemStack(stack).or(() -> super.getTooltipImage(stack));
	}
}
