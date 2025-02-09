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

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import io.github.queerbric.inspecio.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Represents a tooltip component for entities.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.0.0
 */
public abstract class EntityTooltipComponent<C extends InspecioConfig.EntityConfig> implements ConvertibleTooltipData, ClientTooltipComponent {
	protected final Minecraft client = Minecraft.getInstance();
	protected final C config;

	protected EntityTooltipComponent(C config) {
		this.config = config;
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return !this.shouldRender() ? 0 : (this.shouldRenderCustomNames() ? 32 : 24);
	}

	@Override
	public int getWidth(Font textRenderer) {
		return this.shouldRender() ? 24 : 0;
	}

	protected void renderEntity(PoseStack matrices, int x, int y, Entity entity, int ageOffset, boolean spin, boolean allowCustomName) {
		this.renderEntity(matrices, x, y, entity, ageOffset, spin, allowCustomName, 180.f);
	}

	protected void renderEntity(PoseStack matrices, int x, int y, Entity entity, int ageOffset, boolean spin, boolean allowCustomName, float defaultYaw) {
		float size = 24;
		if (Math.max(entity.getBbWidth(), entity.getBbHeight()) > 1.0) {
			size /= Math.max(entity.getBbWidth(), entity.getBbHeight());
		}
		Lighting.setupForFlatItems();
		matrices.pushPose();
		int yOffset = 16;
		if (entity instanceof Squid) {
			size = 16;
			yOffset = 2;
		} else if (entity instanceof ItemEntity) {
			size = 48;
			yOffset = 28;
		}
		if (entity instanceof LivingEntity living && living.isBaby()) {
			size /= 1.7;
		}
		matrices.translate(x + 10, y + yOffset, 1050);
		matrices.scale(1f, 1f, -1);
		matrices.translate(0, 0, 1000);
		matrices.scale(size, size, size);

		var quaternion = Axis.ZP.rotationDegrees(180.f);
		var quaternion2 = Axis.XP.rotationDegrees(-10.f);
		quaternion.mul(quaternion2);
		matrices.mulPose(quaternion);

		if (this.client.cameraEntity != null) {
			entity.setPosRaw(this.client.cameraEntity.getX(), this.client.cameraEntity.getY(), this.client.cameraEntity.getZ());
		}
		this.setupAngles(entity, this.client.player.tickCount, ageOffset, spin, defaultYaw);

		var entityRenderDispatcher = this.client.getEntityRenderDispatcher();
		quaternion2.conjugate();
		((CameraAccessor) entityRenderDispatcher.camera).setYaw(0f);
		entity.setRemainingFireTicks(((EntityAccessor) entity).getHasVisualFire() ? 1 : entity.getRemainingFireTicks());
		entityRenderDispatcher.overrideCameraOrientation(quaternion2);

		entityRenderDispatcher.setRenderShadow(false);

		var immediate = this.client.renderBuffers().bufferSource();
		entity.setCustomNameVisible(allowCustomName && entity.hasCustomName() && (this.config.shouldAlwaysShowName() || Screen.hasControlDown()));

		entityRenderDispatcher.render(entity, 0, 0, 0, 0.f, 1.f, matrices, immediate,
				LightTexture.FULL_BRIGHT
		);
		immediate.endBatch();

		entityRenderDispatcher.setRenderShadow(true);
		matrices.popPose();
		Lighting.setupFor3DItems();
	}

	protected void setupAngles(Entity entity, int age, int ageOffset, boolean spin, float defaultYaw) {
		entity.tickCount = age + ageOffset;

		float yaw = spin ? (float) (((System.currentTimeMillis() / 10) + ageOffset) % 360) : defaultYaw;
		entity.setYRot(yaw);
		entity.setYHeadRot(yaw);
		entity.setXRot(0.f);
		if (entity instanceof LivingEntity living) {
			if (living instanceof Goat) living.yHeadRot = yaw;
			else if (living instanceof WitherEntityAccessor wither) {
				wither.getSideHeadYaws()[0] = wither.getSideHeadYaws()[1] = yaw;
			}
			living.yBodyRot = yaw;
		} else if (entity instanceof ItemEntityAccessor itemEntity) {
			itemEntity.setAge(entity.tickCount);
			itemEntity.setUniqueOffset(0.f);
		} else if (entity instanceof EndCrystal endCrystal) {
			endCrystal.time = endCrystal.tickCount;
		}
	}

	protected abstract boolean shouldRender();

	protected abstract boolean shouldRenderCustomNames();

	protected static void adjustEntity(Entity entity, CompoundTag itemNbt, InspecioConfig.EntitiesConfig config) {
		if (entity instanceof Bucketable bucketable) {
			bucketable.loadFromBucketTag(itemNbt);
			if (entity instanceof Pufferfish pufferfish) {
				pufferfish.setPuffState(config.getPufferFishPuffState());
			} else if (entity instanceof TropicalFishEntityAccessor tropicalFish) {
				if (itemNbt.contains("BucketVariantTag", Tag.TAG_INT)) {
					tropicalFish.invokeSetVariantId(itemNbt.getInt(TropicalFish.BUCKET_VARIANT_TAG));
				}
			}
		}
	}
}
