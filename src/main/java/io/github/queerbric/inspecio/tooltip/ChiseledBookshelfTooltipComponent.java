/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>, Emi
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

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import io.github.queerbric.inspecio.api.InventoryProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import java.util.Optional;

/**
 * Represents the chiseled bookshelf tooltip component.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.7.0
 */
@Environment(EnvType.CLIENT)
public class ChiseledBookshelfTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private static final Minecraft CLIENT = Minecraft.getInstance();
	private final BlockState state;

	public ChiseledBookshelfTooltipComponent(BlockState state) {
		this.state = state;
	}

	public static Optional<TooltipComponent> of(ItemStack stack) {
		var config = Inspecio.getConfig().getContainersConfig().getChiseledBookshelfConfig();
		if (!config.isEnabled()) {
			return Optional.empty();
		}

		var nbt = BlockItem.getBlockEntityData(stack);
		if (nbt == null)
			return Optional.empty();

		var inventory = Inspecio.readInventory(nbt, 6);

		if (inventory == null)
			return Optional.empty();

		if (!config.hasBlockRender()) {
			return InventoryTooltipComponent.of(stack, config.isCompact(), new InventoryProvider.Context(inventory, 3));
		}

		var state = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
		for (int slot = 0; slot < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); slot++) {
			state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot), !inventory.get(slot).isEmpty());
		}

		return Optional.of(new ChiseledBookshelfTooltipComponent(state));
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return 24;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 24;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		Lighting.setupForEntityInInventory();
		PoseStack matrices = graphics.pose();
		matrices.translate(x, y, 0);
		matrices.scale(-1, -1, 1);
		matrices.translate(-20, -20, 0);
		matrices.scale(20, 20, 1);
		var vertexConsumer = CLIENT.renderBuffers().bufferSource();
		CLIENT.getBlockRenderer().renderSingleBlock(this.state, matrices, vertexConsumer,
				LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
		);
		vertexConsumer.endBatch();
		Lighting.setupFor3DItems();
	}
}
