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

package io.github.queerbric.inspecio;

import io.github.queerbric.inspecio.api.ConvertibleTooltipData;
import io.github.queerbric.inspecio.api.InspecioEntrypoint;
import io.github.queerbric.inspecio.api.InventoryProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents the Inspecio mod.
 *
 * @version 1.7.0
 * @since 1.0.0
 */
public class Inspecio implements ClientModInitializer {
	public static final String NAMESPACE = "inspecio";
	private static final Logger LOGGER = LogManager.getLogger(NAMESPACE);
	public static final TagKey<Item> HIDDEN_EFFECTS_TAG = TagKey.create(
			Registries.ITEM, new ResourceLocation(NAMESPACE, "hidden_effects")
	);
	public static final RandomSource COMMON_RANDOM = new LegacyRandomSource(System.currentTimeMillis());
	public static final ResourceLocation GUI_ICONS_TEXTURE = new ResourceLocation("textures/gui/icons.png");
	private static InspecioConfig config = InspecioConfig.defaultConfig();
	private static ModContainer mod;

	@Override
	public void onInitializeClient() {
		Inspecio.mod = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow();
		reloadConfig();

		InventoryProvider.register((stack, config) -> {
			if (config != null && config.isEnabled() && stack.getItem() instanceof BlockItem blockItem) {
				DyeColor color = null;
				if (blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock && ((InspecioConfig.ShulkerBoxConfig) config).hasColor())
					color = shulkerBoxBlock.getColor();

				var nbt = BlockItem.getBlockEntityData(stack);
				if (nbt == null) return null;

				var inventory = readInventory(nbt, getInvSizeFor(stack));
				if (inventory == null) return null;

				return new InventoryProvider.Context(inventory, color);
			}

			return null;
		});

		TooltipComponentCallback.EVENT.register(data -> data instanceof ConvertibleTooltipData convertible ? convertible.toComponent() : null);
		ClientCommandRegistrationCallback.EVENT.register(InspecioCommand::register);

		var entrypoints = FabricLoader.getInstance().getEntrypoints("inspecio", InspecioEntrypoint.class);
		for (var entrypoint : entrypoints) {
			entrypoint.onInspecioInitialized();
		}
	}

	/**
	 * Prints a message to the terminal.
	 *
	 * @param info the message to log
	 */
	public static void log(String info) {
		LOGGER.info("[Inspecio] " + info);
	}

	/**
	 * Prints a warning message to the terminal.
	 *
	 * @param info the message to log
	 */
	public static void warn(String info) {
		LOGGER.warn("[Inspecio] " + info);
	}

	/**
	 * Prints a warning message to the terminal.
	 *
	 * @param info the message to log
	 * @param params parameters to the message.
	 */
	public static void warn(String info, Object... params) {
		LOGGER.warn("[Inspecio] " + info, params);
	}

	/**
	 * Prints a warning message to the terminal.
	 *
	 * @param info the message to log
	 * @param throwable the exception to log, including its stack trace.
	 */
	public static void warn(String info, Throwable throwable) {
		LOGGER.warn("[Inspecio] " + info, throwable);
	}

	public static InspecioConfig getConfig() {
		return config;
	}

	static void reloadConfig() {
		config = InspecioConfig.load();
	}

	static Consumer<String> onConfigError(String path) {
		return error -> {
			InspecioConfig.shouldSaveConfigAfterLoad = true;
			warn("Configuration error at \"" + path + "\", error: " + error);
		};
	}

	static String getVersion() {
		var version = mod.getMetadata().getVersion().getFriendlyString();
		if (version.equals("${version}"))
			return "dev";
		return version;
	}

	private static int getInvSizeFor(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem) {
			var block = blockItem.getBlock();
			if (block instanceof DispenserBlock)
				return 9;
			else if (block instanceof HopperBlock)
				return 5;
			return 27;
		}
		return 0;
	}

	/**
	 * Appends block item tooltips.
	 *
	 * @param stack the stack to add tooltip to
	 * @param block the block
	 * @param tooltip the tooltip
	 */
	public static void appendBlockItemTooltip(ItemStack stack, Block block, List<Component> tooltip) {
		var config = Inspecio.getConfig().getContainersConfig().forBlock(block);
		if (config != null && config.hasLootTable()) {
			var blockEntityNbt = BlockItem.getBlockEntityData(stack);
			if (blockEntityNbt != null && blockEntityNbt.contains("LootTable")) {
				tooltip.add(Component.translatable("inspecio.tooltip.loot_table",
								Component.literal(blockEntityNbt.getString("LootTable"))
										.withStyle(ChatFormatting.GOLD))
						.withStyle(ChatFormatting.GRAY));
			}
		}
	}

	public static void removeVanillaTooltips(List<Component> tooltips, int fromIndex) {
		if (fromIndex >= tooltips.size()) return;

		int keepIndex = tooltips.indexOf(Component.empty());
		if (keepIndex != -1) {
			// we wanna keep tooltips that come after a line break
			keepIndex++;

			int tooltipsToKeep = tooltips.size() - keepIndex;

			// shift tooltips to keep to the front
			for (int i = 0; i < tooltipsToKeep; i++) {
				tooltips.set(fromIndex + i, tooltips.get(keepIndex + i));
			}

			// don't remove them
			fromIndex += tooltipsToKeep;
		}

		tooltips.subList(fromIndex, tooltips.size()).clear();
	}

	public static @Nullable MobEffectInstance getRawEffectFromTag(CompoundTag tag, String tagKey) {
		if (tag == null) {
			return null;
		}
		if (tag.contains(tagKey, Tag.TAG_INT)) {
			var effect = MobEffect.byId(tag.getInt(tagKey));
			if (effect != null)
				return new MobEffectInstance(effect, 200, 0);
		}
		return null;
	}

	/**
	 * Reads the inventory from the given NBT.
	 *
	 * @param nbt the NBT to read
	 * @param size the size of the inventory
	 * @return {@code null} if the inventory is empty, or the inventory otherwise
	 */
	public static @Nullable NonNullList<ItemStack> readInventory(CompoundTag nbt, int size) {
		var inventory = NonNullList.withSize(size, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(nbt, inventory);

		boolean empty = true;
		for (var item : inventory) {
			if (!item.isEmpty()) {
				empty = false;
				break;
			}
		}

		if (empty) {
			return null;
		}

		return inventory;
	}
}
