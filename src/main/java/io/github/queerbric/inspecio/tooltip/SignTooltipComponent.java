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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.SignTooltipMode;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Matrix4f;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public abstract class SignTooltipComponent<M extends Model> implements ConvertibleTooltipData, ClientTooltipComponent {
	protected static final Minecraft CLIENT = Minecraft.getInstance();
	private final SignTooltipMode tooltipMode = Inspecio.getConfig().getSignTooltipMode();
	protected final WoodType type;
	private final SignText front;
	private final SignText back;
	protected final M model;

	public SignTooltipComponent(WoodType type, SignText front, SignText back, M model) {
		this.type = type;
		this.front = front;
		this.back = back;
		this.model = model;
	}

	public static Optional<TooltipComponent> fromItemStack(ItemStack stack) {
		if (!Inspecio.getConfig().getSignTooltipMode().isEnabled())
			return Optional.empty();

		if (stack.getItem() instanceof HangingSignItem signItem) {
			var block = signItem.getBlock();
			var nbt = BlockItem.getBlockEntityData(stack);
			if (nbt != null) return Optional.ofNullable(fromTag(SignBlock.getWoodType(block), nbt, true));
		} else if (stack.getItem() instanceof SignItem signItem) {
			var block = signItem.getBlock();
			var nbt = BlockItem.getBlockEntityData(stack);
			if (nbt != null) return Optional.ofNullable(fromTag(SignBlock.getWoodType(block), nbt, false));
		}
		return Optional.empty();
	}

	public static SignTooltipComponent<?> fromTag(WoodType type, CompoundTag nbt, boolean hanging) {
		Optional<SignText> front = Optional.empty();
		Optional<SignText> back = Optional.empty();

		if (nbt.contains("front_text")) {
			front = SignText.DIRECT_CODEC
					.parse(NbtOps.INSTANCE, nbt.getCompound("front_text"))
					.resultOrPartial(s -> {})
					.map(SignTooltipComponent::parseLines);
		}

		if (nbt.contains("back_text")) {
			back = SignText.DIRECT_CODEC
					.parse(NbtOps.INSTANCE, nbt.getCompound("back_text"))
					.resultOrPartial(s -> {})
					.map(SignTooltipComponent::parseLines);
		}

		if (front.isEmpty() && back.isEmpty()) {
			return null;
		} else if (hanging) {
			return new HangingSign(type, front.orElse(null), back.orElse(null));
		} else {
			return new Sign(type, front.orElse(null), back.orElse(null));
		}
	}

	private static SignText parseLines(SignText text) {
		for (int line = 0; line < 4; line++) {
			Component unfilteredMessage = text.getMessage(line, false);
			Component filteredMessage = text.getMessage(line, true);
			text = text.setMessage(line, unfilteredMessage, filteredMessage);
		}

		return text;
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	protected boolean shouldShowBack() {
		return this.front == null || (this.back != null && Screen.hasControlDown());
	}

	private SignText getText() {
		if (this.shouldShowBack()) return this.back;
		else return this.front;
	}

	private Component[] getMessages() {
		return this.getText().getMessages(Minecraft.getInstance().isTextFilteringEnabled());
	}

	private FormattedCharSequence[] getOrderedMessages() {
		return this.getText().getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(), Component::getVisualOrderText);
	}

	@Override
	public int getHeight() {
		if (this.tooltipMode == SignTooltipMode.FANCY)
			return this.getFancyHeight();
		return this.getMessages().length * 10;
	}

	protected abstract int getFancyHeight();

	@Override
	public int getWidth(Font textRenderer) {
		if (this.tooltipMode == SignTooltipMode.FANCY)
			return this.getFancyWidth();
		return Arrays.stream(this.getMessages()).map(textRenderer::width).max(Comparator.naturalOrder()).orElse(94);
	}

	protected abstract int getFancyWidth();

	@Override
	public void renderText(Font textRenderer, int x, int y, Matrix4f matrix4f, MultiBufferSource.BufferSource immediate) {
		if (this.tooltipMode != SignTooltipMode.FAST)
			return;

		this.drawTextAt(textRenderer, x, y, matrix4f, immediate, false);
	}

	public void drawTextAt(Font textRenderer, int x, int y, Matrix4f matrix4f, MultiBufferSource.BufferSource immediate, boolean center) {
		int signColor = this.getText().getColor().getTextColor();
		var messages = this.getOrderedMessages();

		if (this.getText().hasGlowingText()) {
			int outlineColor;
			if (this.getText().getColor() == DyeColor.BLACK) {
				outlineColor = -988212;
			} else {
				int r = (int) (((signColor >> 24) & 255) * 0.4);
				int g = (int) (((signColor >> 16) & 255) * 0.4);
				int b = (int) (((signColor >> 8) & 255) * 0.4);

				outlineColor = (b >> 8) | (g >> 16) | (r >> 24);
			}

			for (int i = 0; i < messages.length; i++) {
				var text = messages[i];
				float textX = center ? (45 - textRenderer.width(text) / 2.f) : x;
				textRenderer.drawInBatch8xOutline(text, textX, y + i * 10, signColor, outlineColor, matrix4f, immediate,
						LightTexture.FULL_BRIGHT
				);
			}
		} else {
			if (!center && this.getText().getColor() == DyeColor.BLACK) {
				signColor = 0xffffffff;
			}

			for (int i = 0; i < messages.length; i++) {
				var text = messages[i];
				float textX = center ? (45 - textRenderer.width(text) / 2.f) : x;
				textRenderer.drawInBatch(
						text, textX, y + i * 10, signColor, false, matrix4f, immediate, Font.DisplayMode.NORMAL,
						0, LightTexture.FULL_BRIGHT
				);
			}
		}
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.tooltipMode != SignTooltipMode.FANCY)
			return;

		Lighting.setupForFlatItems();
		PoseStack matrices = graphics.pose();
		matrices.pushPose();
		matrices.translate(x + 2, y, 0);

		matrices.pushPose();
		var immediate = CLIENT.renderBuffers().bufferSource();
		var spriteIdentifier = this.getSignTextureId();
		var vertexConsumer = spriteIdentifier != null ? spriteIdentifier.buffer(immediate, this.model::renderType) : null;
		this.renderModel(graphics, vertexConsumer);
		immediate.endBatch();
		matrices.popPose();

		matrices.translate(0, this.getTextOffset(), 10);

		var messages = this.getOrderedMessages();
		for (int i = 0; i < messages.length; i++) {
			var text = messages[i];
			graphics.drawString(textRenderer, text, (int) (45 - textRenderer.width(text) / 2.f), i * 10,
					this.getText().getColor().getTextColor(), false
			);
		}
		matrices.popPose();

		Lighting.setupFor3DItems();
	}

	public abstract Material getSignTextureId();

	public abstract void renderModel(GuiGraphics graphics, VertexConsumer vertexConsumer);

	/**
	 * {@return the vertical offset between the start of the component and where the text lines should be drawn}
	 */
	protected abstract int getTextOffset();

	public static class Sign extends SignTooltipComponent<SignRenderer.SignModel> {

		public Sign(WoodType type, SignText front, SignText back) {
			super(type, front, back, SignRenderer.createSignModel(CLIENT.getEntityModels(), type));
		}

		@Override
		protected int getFancyHeight() {
			return 52;
		}

		@Override
		protected int getFancyWidth() {
			return 94;
		}

		@Override
		public Material getSignTextureId() {
			return Sheets.getSignMaterial(this.type);
		}

		@Override
		public void renderModel(GuiGraphics graphics, VertexConsumer vertexConsumer) {
			graphics.pose().translate(45, 56, 0);

			if (this.shouldShowBack()) {
				graphics.pose().mulPose(Axis.YP.rotationDegrees(180));
			}

			graphics.pose().scale(65, 65, -65);
			this.model.stick.visible = false;
			this.model.root.visible = true;
			this.model.root.render(graphics.pose(), vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		}

		@Override
		protected int getTextOffset() {
			return 4;
		}
	}

	public static class HangingSign extends SignTooltipComponent<HangingSignRenderer.HangingSignModel> {
		private final ResourceLocation textureId = new ResourceLocation("textures/gui/hanging_signs/" + this.type.name() + ".png");

		public HangingSign(WoodType type, SignText front, SignText back) {
			super(type, front, back, null);
		}

		@Override
		protected int getFancyHeight() {
			return 68;
		}

		@Override
		protected int getFancyWidth() {
			return 94;
		}

		@Override
		public Material getSignTextureId() {
			return null;
		}

		@Override
		public void renderModel(GuiGraphics graphics, VertexConsumer vertexConsumer) {
			graphics.pose().translate(44.5, 32, 0);
			RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
			graphics.pose().scale(4.f, 4.f, 1.f);
			graphics.blit(this.textureId, -8, -8, 0.f, 0.f, 16, 16, 16, 16);
		}

		@Override
		protected int getTextOffset() {
			return 26;
		}
	}
}
