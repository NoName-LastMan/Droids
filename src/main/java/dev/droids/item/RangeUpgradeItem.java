package dev.droids.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Upgrade module placed in a Droid's upgrade slot to expand its maximum manageable work area.
 */
public class RangeUpgradeItem extends Item {

    public RangeUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.droids.range_upgrade.desc"));
        tooltipComponents.add(Component.translatable("tooltip.droids.range_upgrade.tiers"));
    }
}
