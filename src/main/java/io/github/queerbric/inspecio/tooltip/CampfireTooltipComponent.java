/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>, Emi
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

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

/**
 * Represents a campfire tooltip. Displays a campfire inventory and the flame if lit.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.1.0
 */
public class CampfireTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private static final ResourceLocation ATLAS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");

	private final NonNullList<ItemStack> inventory;
	private final ResourceLocation fireTexture;

	public CampfireTooltipComponent(NonNullList<ItemStack> inventory, ResourceLocation fireTexture) {
		this.inventory = inventory;
		this.fireTexture = fireTexture;
	}

	public static Optional<TooltipComponent> of(ItemStack stack) {
		if (!Inspecio.getConfig().getContainersConfig().isCampfireEnabled())
			return Optional.empty();

		var nbt = BlockItem.getBlockEntityData(stack);
		if (nbt == null)
			return Optional.empty();

		var inventory = Inspecio.readInventory(nbt, 4);

		if (inventory == null)
			return Optional.empty();

		var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		var fireId = new ResourceLocation(itemId.getNamespace(), "block/" + itemId.getPath() + "_fire");

		var stateNbt = stack.getTagElement(BlockItem.BLOCK_STATE_TAG);
		if (stateNbt != null && stateNbt.contains("lit")) {
			if (stateNbt.get("lit").getAsString().equals("false"))
				fireId = null;
		}

		return Optional.of(new CampfireTooltipComponent(inventory, fireId));
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return 3 * 18 + 2;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 3 * 18 + 2;
	}

	@Override
	public void renderImage(Font textRenderer, int xOffset, int yOffset, GuiGraphics graphics) {
		int x = 1 + 18 * 2;
		int y = 1 + 18 * 2;

		for (int i = 0; i < this.inventory.size(); i++) {
			var stack = this.inventory.get(i);

			InventoryTooltipComponent.drawSlot(graphics, x + xOffset - 1, y + yOffset - 1, 0, null);
			graphics.renderItem(stack, xOffset + x, yOffset + y);
			graphics.renderItemDecorations(textRenderer, stack, xOffset + x, yOffset + y);

			if (i == 1)
				y -= 18 * 2;
			else if (i == 0)
				x -= 18 * 2;
			else if (i == 2)
				x += 18 * 2;
		}

		if (this.fireTexture != null) {
			RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

			var sprite = Minecraft.getInstance().getTextureAtlas(ATLAS_TEXTURE).apply(this.fireTexture);
			if (sprite != null)
				graphics.blit(xOffset + 19, yOffset + 19, 0, 16, 16, sprite);
		}
	}
}
