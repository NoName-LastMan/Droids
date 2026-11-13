package dev.droids.entity.ai;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * AI Goal for Farmer Droid: searches for mature crops on farmland, harvests them,
 * replants matching seeds, and stores products in the droid's internal inventory.
 */
public class FarmerHarvestGoal extends MoveToBlockGoal {

    private final FarmerDroidEntity droid;
    private int workTicks = 0;

    public FarmerHarvestGoal(FarmerDroidEntity droid, double speedModifier, int searchRange) {
        super(droid, speedModifier, searchRange, 2);
        this.droid = droid;
    }

    @Override
    public boolean canUse() {
        if (!this.droid.hasFuel()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.droid.hasFuel() && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.FARMLAND)) {
            BlockPos cropPos = pos.above();
            BlockState cropState = level.getBlockState(cropPos);
            if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
                return true;
            }
            return cropState.is(Blocks.PUMPKIN) || cropState.is(Blocks.MELON);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.droid.getLookControl().setLookAt(
                this.blockPos.getX() + 0.5D,
                this.blockPos.getY() + 1.0D,
                this.blockPos.getZ() + 0.5D,
                10.0F,
                this.droid.getMaxHeadXRot()
        );

        if (this.isReachedTarget()) {
            this.workTicks++;
            this.droid.setWorking(true);

            if (this.workTicks >= 20) { // ~1 second to harvest
                harvestCrop();
                this.workTicks = 0;
                this.droid.setWorking(false);
            }
        } else {
            this.workTicks = 0;
            this.droid.setWorking(false);
        }
    }

    private void harvestCrop() {
        if (!(this.droid.level() instanceof ServerLevel serverLevel)) return;

        BlockPos cropPos = this.blockPos.above();
        BlockState cropState = serverLevel.getBlockState(cropPos);

        if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
            // Get drops from crop
            List<ItemStack> drops = Block.getDrops(cropState, serverLevel, cropPos, null, this.droid, ItemStack.EMPTY);

            // Replant crop at age 0
            serverLevel.setBlock(cropPos, crop.getStateForAge(0), 3);
            serverLevel.playSound(null, cropPos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

            // Deposit drops into droid inventory
            for (ItemStack drop : drops) {
                ItemStack remaining = this.droid.getInventory().addItem(drop);
                if (!remaining.isEmpty()) {
                    // If inventory full, drop on ground
                    Block.popResource(serverLevel, cropPos, remaining);
                }
            }
        } else if (cropState.is(Blocks.PUMPKIN) || cropState.is(Blocks.MELON)) {
            serverLevel.destroyBlock(cropPos, false, this.droid);
            ItemStack fruit = cropState.is(Blocks.PUMPKIN) ? new ItemStack(Items.PUMPKIN) : new ItemStack(Items.MELON_SLICE, 4);
            ItemStack remaining = this.droid.getInventory().addItem(fruit);
            if (!remaining.isEmpty()) {
                Block.popResource(serverLevel, cropPos, remaining);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.droid.setWorking(false);
        this.workTicks = 0;
    }
}
