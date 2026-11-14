package dev.droids.item;

import dev.droids.block.DroidStationBlock;
import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModBlocks;
import dev.droids.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Heavy-duty Droid Wrench.
 * Exclusively used to safely dismantle and retrieve Droids and Stations into item form.
 */
public class DroidWrenchItem extends Item {

    public DroidWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        // 1. Dismantle Farmer Droid
        if (target instanceof FarmerDroidEntity farmer) {
            if (!level.isClientSide) {
                // Drop all contents
                Containers.dropContents(level, farmer.blockPosition(), farmer.getInventory());
                Containers.dropContents(level, farmer.blockPosition(), farmer.getFuelInventory());
                Containers.dropContents(level, farmer.blockPosition(), farmer.getUpgradeInventory());

                // Reset station occupied state if home was a station
                BlockPos homePos = farmer.getHomePosition();
                BlockState homeState = level.getBlockState(homePos);
                if (homeState.is(ModBlocks.DROID_STATION.get())) {
                    level.setBlock(homePos, homeState.setValue(DroidStationBlock.OCCUPIED, false), 3);
                }

                // Drop Droid Item
                ItemStack droidItem = new ItemStack(ModItems.FARMER_DROID.get());
                ItemEntity itemEntity = new ItemEntity(level, farmer.getX(), farmer.getY() + 0.5D, farmer.getZ(), droidItem);
                level.addFreshEntity(itemEntity);

                level.playSound(null, farmer.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
                player.displayClientMessage(Component.translatable("message.droids.wrench.retrieved_farmer"), true);
                farmer.discard();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. Dismantle Miner Droid
        if (target instanceof MinerDroidEntity miner) {
            if (!level.isClientSide) {
                // Drop all contents
                Containers.dropContents(level, miner.blockPosition(), miner.getInventory());
                Containers.dropContents(level, miner.blockPosition(), miner.getFuelInventory());
                Containers.dropContents(level, miner.blockPosition(), miner.getPickaxeInventory());
                Containers.dropContents(level, miner.blockPosition(), miner.getUpgradeInventory());

                // Reset station occupied state if home was a station
                BlockPos homePos = miner.getHomePosition();
                BlockState homeState = level.getBlockState(homePos);
                if (homeState.is(ModBlocks.DROID_STATION.get())) {
                    level.setBlock(homePos, homeState.setValue(DroidStationBlock.OCCUPIED, false), 3);
                }

                // Drop Droid Item
                ItemStack droidItem = new ItemStack(ModItems.MINER_DROID.get());
                ItemEntity itemEntity = new ItemEntity(level, miner.getX(), miner.getY() + 0.5D, miner.getZ(), droidItem);
                level.addFreshEntity(itemEntity);

                level.playSound(null, miner.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
                player.displayClientMessage(Component.translatable("message.droids.wrench.retrieved_miner"), true);
                miner.discard();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        // Dismantle Droid Station on shift-click
        if (player != null && player.isShiftKeyDown() && state.is(ModBlocks.DROID_STATION.get())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DroidStationBlockEntity station) {
                    Containers.dropContents(level, pos, station);
                }
                level.destroyBlock(pos, true, player);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
                player.displayClientMessage(Component.translatable("message.droids.wrench.retrieved_station"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.droids.wrench.description"));
        tooltipComponents.add(Component.translatable("tooltip.droids.wrench.usage_entity"));
        tooltipComponents.add(Component.translatable("tooltip.droids.wrench.usage_block"));
    }
}
