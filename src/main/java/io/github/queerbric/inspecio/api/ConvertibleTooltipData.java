package io.github.queerbric.inspecio.api;

import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;

public interface ConvertibleTooltipData extends TooltipData {
    TooltipComponent toComponent();
}
