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
import io.github.queerbric.inspecio.mixin.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.Optional;

/**
 * Represents a tooltip component which displays bees from a beehive.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class EntityBucketTooltipComponent extends EntityTooltipComponent<InspecioConfig.EntityConfig> {
	private final Entity entity;

	private EntityBucketTooltipComponent(InspecioConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipComponent> of(EntityType<?> type, CompoundTag itemNbt) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getFishBucketConfig().isEnabled())
			return Optional.empty();

		var client = Minecraft.getInstance();
		var entity = type.create(client.level);
		if (entity != null) {
			EntityType.updateCustomEntityTag(client.level, null, entity, itemNbt);
			adjustEntity(entity, itemNbt, entitiesConfig);
			return Optional.of(new EntityBucketTooltipComponent(entitiesConfig.getFishBucketConfig(), entity));
		}
		return Optional.empty();
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.shouldRender()) {
			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(2, 2, 0);
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setDeltaMovement(1.f, 1.f, 1.f);
			this.renderEntity(matrices, x + 16, y, this.entity, 0, this.config.shouldSpin(), false, 90.f);
			matrices.popPose();
		}
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return false;
	}
}
