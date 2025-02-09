package io.github.queerbric.inspecio.api;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public interface ConvertibleTooltipData extends TooltipComponent {
    ClientTooltipComponent toComponent();
}
