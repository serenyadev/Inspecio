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

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import java.util.Optional;

public class BannerTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private final Minecraft client = Minecraft.getInstance();
	private final ListTag pattern;
	private final ModelPart bannerField;

	private BannerTooltipComponent(ListTag pattern) {
		this.pattern = pattern;
		this.bannerField = this.client.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
	}

	public static Optional<TooltipComponent> of(TagKey<BannerPattern> pattern) {
		if (!Inspecio.getConfig().hasBannerPattern())
			return Optional.empty();

		var patternList = BuiltInRegistries.BANNER_PATTERN.getTag(pattern).map(ImmutableList::copyOf).orElse(ImmutableList.of());
		var patterns = new BannerPattern.Builder();

		for (var p : patternList) {
			patterns.addPattern(p, DyeColor.WHITE);
		}

		return Optional.of(new BannerTooltipComponent(patterns.toListTag()));
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return 32;
	}

	@Override
	public int getWidth(Font textRenderer) {
		return 16;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		Lighting.setupForFlatItems();
		PoseStack matrices = graphics.pose();
		matrices.pushPose();
		matrices.translate(x + 8, y + 8, 0);
		matrices.pushPose();
		matrices.translate(0.5, 16, 0);
		matrices.scale(6, -6, 1);
		matrices.scale(2, -2, -2);
		var immediate = this.client.renderBuffers().bufferSource();
		this.bannerField.xRot = 0.f;
		this.bannerField.y = -32.f;
		var list = BannerBlockEntity.createPatterns(DyeColor.GRAY, this.pattern);
		BannerRenderer.renderPatterns(matrices, immediate, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				this.bannerField, ModelBakery.BANNER_BASE, true, list);
		matrices.popPose();
		immediate.endBatch();
		matrices.popPose();
		Lighting.setupFor3DItems();
	}
}
