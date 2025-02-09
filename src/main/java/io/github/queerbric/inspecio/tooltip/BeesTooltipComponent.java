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
import io.github.queerbric.inspecio.InspecioConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a tooltip component which displays bees from a beehive.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class BeesTooltipComponent extends EntityTooltipComponent<InspecioConfig.BeeEntityConfig> {
	private static final ResourceLocation HONEY_LEVEL_TEXTURE = new ResourceLocation(Inspecio.NAMESPACE, "textures/tooltips/honey_level.png");

	private final List<Bee> bees = new ArrayList<>();
	private final int honeyLevel;

	public BeesTooltipComponent(InspecioConfig.BeeEntityConfig config, int honeyLevel, ListTag bees) {
		super(config);
		this.honeyLevel = honeyLevel;

		bees.stream().map(nbt -> (CompoundTag) nbt).forEach(nbt -> {
			var bee = nbt.getCompound("EntityData");
			bee.remove("UUID");
			bee.remove("Passengers");
			bee.remove("Leash");
			var entity = EntityType.loadEntityRecursive(bee, this.client.level, Function.identity());
			if (entity != null) {
				this.bees.add(new Bee(nbt.getInt("TicksInHive"), entity));
			}
		});
	}

	public static Optional<TooltipComponent> of(ItemStack stack) {
		var config = Inspecio.getConfig().getEntitiesConfig().getBeeConfig();
		if (!config.isEnabled() && !config.shouldShowHoney())
			return Optional.empty();

		int honeyLevel = 0;

		var stateNbt = stack.getTagElement(BlockItem.BLOCK_STATE_TAG);
		if (stateNbt != null) {
			Tag honeyLevelNbt = stateNbt.get(BeehiveBlock.HONEY_LEVEL.getName());

			if (honeyLevelNbt instanceof IntTag nbtInt) {
				honeyLevel = nbtInt.getAsInt();
			} else if (honeyLevelNbt instanceof StringTag nbtString) {
				try {
					honeyLevel = Integer.parseInt(nbtString.getAsString());
				} catch (NumberFormatException e) {
					// ignored
				}
			}
		}

		var nbt = BlockItem.getBlockEntityData(stack);
		if ((nbt == null || !nbt.contains(BeehiveBlockEntity.BEES, Tag.TAG_LIST)) && !config.shouldShowHoney())
			return Optional.empty();

		var bees = nbt == null || !config.isEnabled() ? new ListTag() : nbt.getList(BeehiveBlockEntity.BEES, Tag.TAG_COMPOUND);
		if (!bees.isEmpty() || config.shouldShowHoney())
			return Optional.of(new BeesTooltipComponent(config, honeyLevel, bees));

		return Optional.empty();
	}

	@Override
	public int getHeight() {
		if (this.bees.isEmpty()) {
			return this.config.shouldShowHoney() ? 12 : 0;
		} else {
			return (this.shouldRenderCustomNames() ? 32 : 24) + (this.config.shouldShowHoney() ? 16 : 0);
		}
	}

	@Override
	public int getWidth(Font textRenderer) {
		return Math.max(this.bees.size() * 26, (this.config.shouldShowHoney() ? 52 : 0));
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		PoseStack matrices = graphics.pose();
		matrices.pushPose();

		if (!this.bees.isEmpty()) {
			matrices.translate(2, 4, 0);

			int xOffset = x;
			for (var bee : this.bees) {
				this.renderEntity(matrices, xOffset, y + (this.shouldRenderCustomNames() ? 8 : 0), bee.bee(), bee.ticksInHive(),
						this.config.shouldSpin(), true);
				xOffset += 26;
			}
		}

		if (config.shouldShowHoney()) {
			matrices.translate(x, y + (this.bees.isEmpty() ? 0 : (this.shouldRenderCustomNames() ? 32 : 24)), 0);
			matrices.scale(2, 2, 1);

			graphics.blit(HONEY_LEVEL_TEXTURE, 0, 0, 0, 0, 0, 26, 5, 32, 16);

			if (honeyLevel != 0) {
				graphics.blit(HONEY_LEVEL_TEXTURE, 0, 0, 0, 0, 5, Math.min(25, honeyLevel * 5 + 1), 6, 32, 16);
			}
		}

		matrices.popPose();
	}

	@Override
	protected boolean shouldRender() {
		return !this.bees.isEmpty();
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.bees.stream().map(bee -> bee.bee().hasCustomName()).reduce(false, (first, second) -> first || second)
				&& (this.config.shouldAlwaysShowName() || Screen.hasControlDown());
	}

	record Bee(int ticksInHive, Entity bee) {
	}
}
