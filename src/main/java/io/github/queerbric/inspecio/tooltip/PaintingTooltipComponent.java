/*
 * Copyright (c) 2023 LambdAurora <email@lambdaurora.dev>, Emi
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

package io.github.queerbric.inspecio.tooltip;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import io.github.queerbric.inspecio.mixin.DecorationItemAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import java.util.Optional;

/**
 * Represents a painting tooltip for painting items with a known variant.
 *
 * @param painting the painting variant
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.8.0
 */
@Environment(EnvType.CLIENT)
public record PaintingTooltipComponent(PaintingVariant painting) implements ConvertibleTooltipData, ClientTooltipComponent {
	public static Optional<TooltipComponent> of(ItemStack stack) {
		if (!Inspecio.getConfig().hasPainting())
			return Optional.empty();

		CompoundTag nbt = stack.getTag();

		if (nbt != null
				&& stack.getItem() instanceof DecorationItemAccessor decorationItem
				&& decorationItem.getType() == EntityType.PAINTING
		) {
			var entityNbt = nbt.getCompound("EntityTag");

			if (entityNbt != null) {
				return Painting.loadVariant(entityNbt)
						.map(Holder::value)
						.map(PaintingTooltipComponent::new);
			}
		}

		return Optional.empty();
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return this.painting.getHeight();
	}

	@Override
	public int getWidth(Font textRenderer) {
		return this.painting.getWidth();
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		PaintingTextureManager paintingManager = Minecraft.getInstance().getPaintingTextures();
		TextureAtlasSprite sprite = paintingManager.get(this.painting);
		graphics.blit(x, y - 2, 0, this.getWidth(textRenderer), this.getHeight(), sprite);
	}
}
