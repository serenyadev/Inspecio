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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import java.util.Optional;

public class SpawnEntityTooltipComponent extends EntityTooltipComponent<InspecioConfig.EntityConfig> {
	private final Entity entity;

	public SpawnEntityTooltipComponent(InspecioConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipComponent> of(EntityType<?> entityType, CompoundTag itemNbt) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getSpawnEggConfig().isEnabled() || entityType == null)
			return Optional.empty();

		var client = Minecraft.getInstance();
		var entity = entityType.create(client.level);
		if (entity != null) {
			adjustEntity(entity, itemNbt, entitiesConfig);
			var itemEntityNbt = itemNbt.getCompound("EntityTag").copy();

			if (!itemEntityNbt.contains("VillagerData")) {
				var villagerData = new CompoundTag();
				villagerData.putString("profession", "minecraft:none");
				villagerData.putInt("level", 1);
				villagerData.putString("type", "minecraft:plains");
				itemEntityNbt.put("VillagerData", villagerData);
			}

			if (itemEntityNbt.contains(Entity.ID_TAG, Tag.TAG_STRING)) { // The spawn egg specifies its own entity type.
				var id = itemEntityNbt.getString(Entity.ID_TAG);
				if (id.startsWith("minecraft:")) {
					id = id.substring(10);
				}
				if (id.replaceAll("[^a-z0-9/._-]", "").matches(id)) {
					itemEntityNbt.putString(Entity.ID_TAG, id);
					Optional<EntityType<?>> specifiedEntityType = EntityType.by(itemEntityNbt);
					if (specifiedEntityType.isPresent()) {
						var actualEntity = specifiedEntityType.get().create(client.level);
						if (actualEntity != null) {
							entity = actualEntity;
							adjustEntity(entity, itemNbt, entitiesConfig);
						}
					}
				}
			}

			var entityTag = entity.saveWithoutId(new CompoundTag());
			var uuid = entity.getUUID();
			entityTag.merge(itemEntityNbt);
			entity.setUUID(uuid);
			entity.load(entityTag);
			return Optional.of(new SpawnEntityTooltipComponent(entitiesConfig.getSpawnEggConfig(), entity));
		}

		return Optional.empty();
	}

	public static Optional<TooltipComponent> ofMobSpawner(ItemStack stack) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getMobSpawnerConfig().isEnabled())
			return Optional.empty();

		var nbt = BlockItem.getBlockEntityData(stack);
		if (nbt == null)
			return Optional.empty();

		var client = Minecraft.getInstance();

		var logic = new BaseSpawner() {
			@Override
			public void broadcastEvent(Level world, BlockPos pos, int eventType) {
			}
		};
		logic.load(client.level, client.player.blockPosition(), nbt);

		var entity = logic.getOrCreateDisplayEntity(client.level, Inspecio.COMMON_RANDOM, client.player.blockPosition());
		if (entity != null) {
			return Optional.of(new SpawnEntityTooltipComponent(entitiesConfig.getMobSpawnerConfig(), entity));
		}

		return Optional.empty();
	}

	@Override
	public int getHeight() {
		return super.getHeight() + 36;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 128;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.shouldRender()) {
			PoseStack matrices = graphics.pose();
			matrices.pushPose();
			matrices.translate(30, 0, 0);
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setDeltaMovement(1.f, 1.f, 1.f);
			this.renderEntity(matrices, x + 20, y + 20, this.entity, 0, this.config.shouldSpin(), true, 90.f);
			matrices.popPose();
		}
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
