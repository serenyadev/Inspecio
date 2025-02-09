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

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.mixin.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.Optional;

/**
 * Represents an armor stand tooltip. Displays an armor stand and its armor.
 *
 * @author Zailer43
 * @version 1.8.0
 * @since 1.1.0
 */
public class ArmorStandTooltipComponent extends EntityTooltipComponent<InspecioConfig.EntityConfig> {
	private final Entity entity;

	public ArmorStandTooltipComponent(InspecioConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipComponent> of(CompoundTag itemNbt) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		var entityType = EntityType.ARMOR_STAND;
		if (!entitiesConfig.getArmorStandConfig().isEnabled())
			return Optional.empty();

		var client = Minecraft.getInstance();
		var entity = entityType.create(client.level);
		assert entity != null;
		adjustEntity(entity, itemNbt, entitiesConfig);
		var itemEntityNbt = itemNbt.getCompound("EntityTag").copy();
		var entityTag = entity.saveWithoutId(new CompoundTag());
		var uuid = entity.getUUID();
		entityTag.merge(itemEntityNbt);
		entity.setUUID(uuid);
		entity.load(entityTag);
		return Optional.of(new ArmorStandTooltipComponent(entitiesConfig.getArmorStandConfig(), entity));
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.shouldRender()) {
			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(30, 0, 0);
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setDeltaMovement(1.f, 1.f, 1.f);
			this.renderEntity(matrices, x + 20, y + 12, this.entity, 0, this.config.shouldSpin(), true, 180.f);
			matrices.popPose();
		}
	}

	@Override
	public int getHeight() {
		return super.getHeight() + 16;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 128;
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.entity.hasCustomName() && (this.config.shouldAlwaysShowName() || Screen.hasControlDown());
	}
}
