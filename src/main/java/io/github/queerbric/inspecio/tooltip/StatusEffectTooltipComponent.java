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

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.github.queerbric.inspecio.HiddenEffectMode;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.joml.Matrix4f;

import java.util.List;

public class StatusEffectTooltipComponent implements ConvertibleTooltipData, ClientTooltipComponent {
	private static final ResourceLocation MYSTERY_TEXTURE = new ResourceLocation(Inspecio.NAMESPACE, "textures/mob_effects/mystery.png");
	private List<MobEffectInstance> list = Lists.newArrayList();
	private final FloatList chances = new FloatArrayList();
	private boolean hidden = false;
	private float multiplier;

	public StatusEffectTooltipComponent(List<MobEffectInstance> list, float multiplier) {
		this.list = list;
		this.multiplier = multiplier;
	}

	public StatusEffectTooltipComponent(List<Pair<MobEffectInstance, Float>> list) {
		for (var pair : list) {
			this.list.add(pair.getFirst());
			this.chances.add(pair.getSecond().floatValue());
		}
		this.multiplier = 1.f;
	}

	public StatusEffectTooltipComponent() {
		this.hidden = true;
	}

	private Component getHiddenText() {
		var effectsConfig = Inspecio.getConfig().getEffectsConfig();
		boolean hiddenMotion = effectsConfig.hasHiddenMotion();
		HiddenEffectMode hiddenEffectMode = effectsConfig.getHiddenEffectMode();

		return hiddenEffectMode.stylize(Component.literal(hiddenEffectMode.getText(true, hiddenMotion)), hiddenMotion);
	}

	private Component getHiddenTime() {
		var effectsConfig = Inspecio.getConfig().getEffectsConfig();
		boolean hiddenMotion = effectsConfig.hasHiddenMotion();
		HiddenEffectMode hiddenEffectMode = effectsConfig.getHiddenEffectMode();

		String timeColon = hiddenEffectMode == HiddenEffectMode.ENCHANTMENT && hiddenMotion ? "i" : ":";

		MutableComponent minutes = hiddenEffectMode.stylize(Component.literal(hiddenEffectMode.getText(false, hiddenMotion)), hiddenMotion);
		Component seconds = minutes.copy();

		return Component.empty().append(minutes)
				.append(hiddenEffectMode.stylize(Component.literal(timeColon), false))
				.append(seconds);
	}

	@Override
	public ClientTooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		if (this.hidden) {
			return 20;
		}
		return this.list.size() * 20;
	}

	@Override
	public int getWidth(Font textRenderer) {
		if (this.hidden) {
			return 26 + textRenderer.width(this.getHiddenText());
		}

		int max = 64;
		for (int i = 0; i < this.list.size(); i++) {
			MobEffectInstance statusEffectInstance = this.list.get(i);
			String statusEffectName = this.getStatusEffectName(statusEffectInstance);

			if (statusEffectInstance.getDuration() > 1) {
				var duration = this.getDuration(i, statusEffectInstance);
				max = Math.max(max, 26 + textRenderer.width(duration));
			} else if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
				String string2 = (int) (this.chances.getFloat(i) * 100f) + "%";
				max = Math.max(max, 26 + textRenderer.width(string2));
			}
			max = Math.max(max, 26 + textRenderer.width(statusEffectName));
		}
		return max;
	}

	@Override
	public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
		if (this.hidden) {
			graphics.blit(MYSTERY_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
		} else {
			Minecraft client = Minecraft.getInstance();
			MobEffectTextureManager statusEffectSpriteManager = client.getMobEffectTextures();
			for (int i = 0; i < list.size(); i++) {
				MobEffectInstance statusEffectInstance = list.get(i);
				MobEffect statusEffect = statusEffectInstance.getEffect();
				var sprite = statusEffectSpriteManager.get(statusEffect);
				graphics.blit(x, y + i * 20, 0, 18, 18, sprite);
			}
		}
	}

	@Override
	public void renderText(Font textRenderer, int x, int y, Matrix4f model, BufferSource immediate) {
		if (this.hidden) {
			textRenderer.drawInBatch(this.getHiddenText(), x + 24, y, 8355711, true,
					model, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
			textRenderer.drawInBatch(this.getHiddenTime(), x + 24, y + 10, 8355711, true,
					model, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		} else {
			for (int i = 0; i < this.list.size(); i++) {
				MobEffectInstance statusEffectInstance = this.list.get(i);
				String statusEffectName = this.getStatusEffectName(statusEffectInstance);

				int off = 0;
				if (statusEffectInstance.getDuration() <= 1) {
					off += 5;
				}

				Integer color = statusEffectInstance.getEffect().getCategory().getTooltipFormatting().getColor();
				textRenderer.drawInBatch(statusEffectName, x + 24, y + i * 20 + off, color != null ? color : 16777215,
						true, model, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
				if (statusEffectInstance.getDuration() > 1) {
					var duration = this.getDuration(i, statusEffectInstance);
					textRenderer.drawInBatch(duration, x + 24, y + i * 20 + 10, 8355711, true,
							model, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
				} else if (this.chances.size() > i && this.chances.getFloat(i) < 1f) {
					String chance = (int) (this.chances.getFloat(i) * 100f) + "%";
					textRenderer.drawInBatch(chance, x + 24, y + i * 20 + 10, 8355711, true,
							model, immediate, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
				}
			}
		}
	}

	private String getStatusEffectName(MobEffectInstance statusEffectInstance) {
		String statusEffectName = I18n.get(statusEffectInstance.getEffect().getDescriptionId());

		if (statusEffectInstance.getAmplifier() >= 1 && statusEffectInstance.getAmplifier() <= 9) {
			statusEffectName = statusEffectName + ' ' + I18n.get("enchantment.level." + (statusEffectInstance.getAmplifier() + 1));
		}

		return statusEffectName;
	}

	private Component getDuration(int index, MobEffectInstance statusEffect) {
		var duration = MobEffectUtil.formatDuration(statusEffect, multiplier);

		if (this.chances.size() > index && this.chances.getFloat(index) < 1f) {
			duration = duration.copy().append(" - " + (int) (this.chances.getFloat(index) * 100f) + "%");
		}

		return duration;
	}
}
