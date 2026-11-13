package dev.droids.item;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Wrench used to mark farm boundaries (2 corners), link storage chests, and configure Droids.
 */
public class DroidWrenchItem extends Item {

    public DroidWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);

        // Shift-click on container: record storage chest
        if (player.isShiftKeyDown() && be != null) {
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("ChestX", pos.getX());
                    tag.putInt("ChestY", pos.getY());
                    tag.putInt("ChestZ", pos.getZ());
                });
                player.displayClientMessage(
                        Component.translatable("message.droids.wrench.chest_recorded", pos.getX(), pos.getY(), pos.getZ()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Normal right-click on block: mark corner 1 or corner 2
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                boolean hasCorner1 = customData != null && customData.contains("Corner1X");
                boolean hasCorner2 = customData != null && customData.contains("Corner2X");

                if (!hasCorner1 || hasCorner2) {
                    // Start new area: set Corner 1
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putInt("Corner1X", pos.getX());
                        tag.putInt("Corner1Y", pos.getY());
                        tag.putInt("Corner1Z", pos.getZ());
                        tag.remove("Corner2X");
                        tag.remove("Corner2Y");
                        tag.remove("Corner2Z");
                    });
                    player.displayClientMessage(
                            Component.translatable("message.droids.wrench.corner1_marked", pos.getX(), pos.getZ()),
                            true
                    );
                } else {
                    // Set Corner 2 and finish bounding box
                    int c1x = customData.copyTag().getInt("Corner1X");
                    int c1z = customData.copyTag().getInt("Corner1Z");
                    int width = Math.abs(pos.getX() - c1x) + 1;
                    int length = Math.abs(pos.getZ() - c1z) + 1;

                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putInt("Corner2X", pos.getX());
                        tag.putInt("Corner2Y", pos.getY());
                        tag.putInt("Corner2Z", pos.getZ());
                    });
                    player.displayClientMessage(
                            Component.translatable("message.droids.wrench.corner2_marked", pos.getX(), pos.getZ(), width, length),
                            true
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Shift-right-click in air: clear coordinates
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.remove("Corner1X");
                    tag.remove("Corner1Y");
                    tag.remove("Corner1Z");
                    tag.remove("Corner2X");
                    tag.remove("Corner2Y");
                    tag.remove("Corner2Z");
                    tag.remove("ChestX");
                    tag.remove("ChestY");
                    tag.remove("ChestZ");
                });
                player.displayClientMessage(Component.translatable("message.droids.wrench.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget instanceof FarmerDroidEntity droid) {
            Level level = player.level();
            if (!level.isClientSide) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData == null) {
                    player.displayClientMessage(Component.translatable("message.droids.wrench.empty"), true);
                    return InteractionResult.SUCCESS;
                }

                // Transfer Chest pos if present
                if (customData.contains("ChestX")) {
                    int cx = customData.copyTag().getInt("ChestX");
                    int cy = customData.copyTag().getInt("ChestY");
                    int cz = customData.copyTag().getInt("ChestZ");
                    droid.setLinkedChest(new BlockPos(cx, cy, cz));
                }

                // Transfer Area if both corners present
                if (customData.contains("Corner1X") && customData.contains("Corner2X")) {
                    int c1x = customData.copyTag().getInt("Corner1X");
                    int c1z = customData.copyTag().getInt("Corner1Z");
                    int c2x = customData.copyTag().getInt("Corner2X");
                    int c2z = customData.copyTag().getInt("Corner2Z");

                    int width = Math.abs(c2x - c1x) + 1;
                    int length = Math.abs(c2z - c1z) + 1;
                    int maxAllowed = droid.getMaxAllowedAreaDimension();

                    if (width > maxAllowed || length > maxAllowed) {
                        player.displayClientMessage(
                                Component.translatable("message.droids.wrench.area_too_large", width, length, maxAllowed, maxAllowed),
                                false
                        );
                        return InteractionResult.SUCCESS;
                    }

                    droid.setWorkArea(
                            Math.min(c1x, c2x),
                            Math.min(c1z, c2z),
                            Math.max(c1x, c2x),
                            Math.max(c1z, c2z)
                    );

                    player.displayClientMessage(
                            Component.translatable("message.droids.wrench.configured", width, length),
                            true
                    );
                } else {
                    player.displayClientMessage(Component.translatable("message.droids.wrench.chest_only_configured"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("Corner1X")) {
            int c1x = customData.copyTag().getInt("Corner1X");
            int c1z = customData.copyTag().getInt("Corner1Z");
            if (customData.contains("Corner2X")) {
                int c2x = customData.copyTag().getInt("Corner2X");
                int c2z = customData.copyTag().getInt("Corner2Z");
                int w = Math.abs(c2x - c1x) + 1;
                int l = Math.abs(c2z - c1z) + 1;
                tooltipComponents.add(Component.translatable("tooltip.droids.wrench.area_ready", w, l));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.droids.wrench.corner1_set", c1x, c1z));
            }
        }

        if (customData != null && customData.contains("ChestX")) {
            int cx = customData.copyTag().getInt("ChestX");
            int cy = customData.copyTag().getInt("ChestY");
            int cz = customData.copyTag().getInt("ChestZ");
            tooltipComponents.add(Component.translatable("tooltip.droids.wrench.chest_set", cx, cy, cz));
        }

        tooltipComponents.add(Component.translatable("tooltip.droids.wrench.instruction"));
    }
}
