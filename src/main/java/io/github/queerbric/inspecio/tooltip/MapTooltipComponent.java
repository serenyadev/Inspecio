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

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import java.util.Optional;

public class MapTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private final Minecraft client = Minecraft.getInstance();
	public int map;

	public MapTooltipComponent(int map) {
		this.map = map;
	}

	public static Optional<TooltipComponent> of(ItemStack stack) {
		if (!Inspecio.getConfig().getFilledMapConfig().isEnabled()) return Optional.empty();
		var map = MapItem.getMapId(stack);
		return map == null ? Optional.empty() : Optional.of(new MapTooltipComponent(map));
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return 128 + 2;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 128;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		var vertices = this.client.renderBuffers().bufferSource();
		var map = this.client.gameRenderer.getMapRenderer();
		var state = MapItem.getSavedData(this.map, this.client.level);
		if (state == null) return;
		PoseStack matrices = graphics.pose();
		matrices.pushPose();
		matrices.translate(x, y, 0);
		matrices.scale(1, 1, 0);
		map.render(matrices, vertices, this.map, state, !Inspecio.getConfig().getFilledMapConfig().shouldShowPlayerIcon(),
				LightTexture.FULL_BRIGHT);
		vertices.endBatch();
		matrices.popPose();
	}
}
