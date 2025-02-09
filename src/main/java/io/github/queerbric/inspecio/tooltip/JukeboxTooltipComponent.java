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

package io.github.queerbric.inspecio.tooltip;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.JukeboxTooltipMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * Represents a jukebox tooltip component. Displays the inserted disc description and an inventory slot with the disc in fancy mode.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class JukeboxTooltipComponent extends InventoryTooltipComponent {
	private final InspecioConfig config = Inspecio.getConfig();
	private final RecordItem disc;

	public JukeboxTooltipComponent(ItemStack discStack) {
		super(NonNullList.withSize(1, discStack), 1, null);
		this.disc = (RecordItem) discStack.getItem();
	}

	public static Optional<TooltipComponent> of(ItemStack stack) {
		if (!Inspecio.getConfig().getJukeboxTooltipMode().isEnabled()) return Optional.empty();
		var nbt = BlockItem.getBlockEntityData(stack);
		if (nbt != null && nbt.contains("RecordItem")) {
			var discStack = ItemStack.of(nbt.getCompound("RecordItem"));
			if (discStack.getItem() instanceof RecordItem)
				return Optional.of(new JukeboxTooltipComponent(discStack));
		}
		return Optional.empty();
	}

	@Override
	public int getHeight() {
		int height = 10;
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			height += 20;
		return height;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return textRenderer.width(this.disc.getDisplayName());
	}

	@Override
	public void renderText(Font textRenderer, int x, int y, Matrix4f matrix4f, MultiBufferSource.BufferSource immediate) {
		textRenderer.drawInBatch(this.disc.getDisplayName(), x, y, 11184810, true, matrix4f, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			super.renderImage(textRenderer, x, y + 10, graphics);
	}
}
