package dev.droids.item;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.List;

/**
 * Wrench used to link Droids to storage blocks (chests, barrels) and inspect status.
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

        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            // Check if player clicked a container
            if (!level.isClientSide && player != null) {
                // Store position in item custom data (1.21 Data Components)
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("TargetX", pos.getX());
                    tag.putInt("TargetY", pos.getY());
                    tag.putInt("TargetZ", pos.getZ());
                });
                player.displayClientMessage(
                        Component.translatable("message.droids.wrench.pos_recorded", pos.getX(), pos.getY(), pos.getZ()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget instanceof FarmerDroidEntity droid) {
            Level level = player.level();
            if (!level.isClientSide) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && customData.contains("TargetX")) {
                    int x = customData.copyTag().getInt("TargetX");
                    int y = customData.copyTag().getInt("TargetY");
                    int z = customData.copyTag().getInt("TargetZ");
                    BlockPos targetPos = new BlockPos(x, y, z);

                    droid.setLinkedChest(targetPos);
                    player.displayClientMessage(
                            Component.translatable("message.droids.wrench.linked", x, y, z),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.droids.wrench.no_target"),
                            true
                    );
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
        if (customData != null && customData.contains("TargetX")) {
            int x = customData.copyTag().getInt("TargetX");
            int y = customData.copyTag().getInt("TargetY");
            int z = customData.copyTag().getInt("TargetZ");
            tooltipComponents.add(Component.translatable("tooltip.droids.wrench.target", x, y, z));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.droids.wrench.howto"));
        }
    }
}
