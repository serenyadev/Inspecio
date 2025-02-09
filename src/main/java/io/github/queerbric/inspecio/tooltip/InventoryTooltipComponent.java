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

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import io.github.queerbric.inspecio.api.InventoryProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the inventory tooltip component.
 *
 * @author LambdAurora
 * @version 1.8.1
 * @since 1.0.0
 */
public class InventoryTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private static final ResourceLocation STATS_ICONS_TEXTURE = new ResourceLocation("textures/gui/container/stats_icons.png");
	private final List<ItemStack> inventory;
	private final int columns;
	private final DyeColor color;

	public InventoryTooltipComponent(List<ItemStack> inventory, int columns, @Nullable DyeColor color) {
		this.inventory = inventory;
		this.columns = columns == 0 ? inventory.size() / 3 : columns;
		this.color = color;
	}

	public static Optional<TooltipComponent> of(ItemStack stack, boolean compact, @Nullable InventoryProvider.Context context) {
		if (context == null) {
			return Optional.empty();
		}

		List<ItemStack> inventory = context.inventory();
		var blockEntityNbt = BlockItem.getBlockEntityData(stack);
		if (blockEntityNbt == null)
			return Optional.empty();

		if (inventory.stream().allMatch(ItemStack::isEmpty))
			return Optional.empty();

		int columns = Math.min(context.columns(), 9);

		if (compact) {
			var compactedInventory = new ArrayList<ItemStack>();
			inventory.forEach(invStack -> {
				if (invStack.isEmpty())
					return;
				compactedInventory.stream().filter(other -> ItemStack.isSameItemSameTags(other, invStack))
						.findFirst()
						.ifPresentOrElse(
								s -> s.grow(invStack.getCount()),
								() -> compactedInventory.add(invStack)
						);
			});

			inventory = compactedInventory;
			columns = 9;
		}

		return Optional.of(new InventoryTooltipComponent(inventory, columns, context.color()));
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		int rows = this.inventory.size() / this.getColumns();
		if (this.inventory.size() % this.getColumns() != 0)
			rows++;
		return 18 * rows + 3;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return this.getColumns() * 18;
	}

	@Override
	public void renderImage(Font textRenderer, int xOffset, int yOffset, GuiGraphics graphics) {
		int x = 1;
		int y = 1;
		int lines = this.getColumns();

		for (var stack : this.inventory) {
			drawSlot(graphics, x + xOffset - 1, y + yOffset - 1, 0, this.color == null ? null : color.getTextureDiffuseColors());
			graphics.renderItem(stack, xOffset + x, yOffset + y);
			graphics.renderItemDecorations(textRenderer, stack, xOffset + x, yOffset + y);
			x += 18;
			if (x >= 18 * lines) {
				x = 1;
				y += 18;
			}
		}
	}

	public static void drawSlot(GuiGraphics graphics, int x, int y, int z, float[] color) {
		if (color == null)
			color = new float[]{1.f, 1.f, 1.f};
		RenderSystem.setShaderColor(color[0], color[1], color[2], 1.f);
		graphics.blit(STATS_ICONS_TEXTURE, x, y, z, 0.f, 0.f, 18, 18, 128, 128);
		RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
	}

	protected int getColumns() {
		return this.columns;
	}
}
