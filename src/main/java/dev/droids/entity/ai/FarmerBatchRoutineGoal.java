package dev.droids.entity.ai;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * High-performance Automated Guided Batch Routine for Farmer Droid.
 * 
 * Replaces vanilla sluggish step-by-step scanning:
 * 1. IDLE: Rests motionless at Home dock. Scans entire field on a 10s timer.
 * 2. HARVESTING: Compiles all mature crops into an optimized route, chain-harvesting without pauses.
 * 3. DEPOSITING: Walks directly to linked chest and unloads all harvested produce.
 * 4. RETURNING: Returns to Home dock and enters motionless standby until the next scheduled cycle.
 */
public class FarmerBatchRoutineGoal extends Goal {

    public enum RoutineState {
        IDLE,
        HARVESTING,
        DEPOSITING,
        RETURNING
    }

    private final FarmerDroidEntity droid;
    private final double speedModifier;
    private RoutineState state = RoutineState.IDLE;

    private final Deque<BlockPos> harvestQueue = new ArrayDeque<>();
    private BlockPos currentCropTarget = null;

    private int scanTimer = 0;
    private int actionTicks = 0;
    private int repathTimer = 0;

    public FarmerBatchRoutineGoal(FarmerDroidEntity droid, double speedModifier) {
        this.droid = droid;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.droid.hasFuel();
    }

    @Override
    public boolean canContinueToUse() {
        return this.droid.hasFuel();
    }

    @Override
    public void tick() {
        if (!this.droid.hasFuel()) {
            this.droid.getNavigation().stop();
            this.state = RoutineState.IDLE;
            this.droid.setWorking(false);
            return;
        }

        switch (this.state) {
            case IDLE -> tickIdle();
            case HARVESTING -> tickHarvesting();
            case DEPOSITING -> tickDepositing();
            case RETURNING -> tickReturning();
        }
    }

    // 1. IDLE: Wait at home dock and scan area periodically
    private void tickIdle() {
        this.droid.setWorking(false);
        this.scanTimer++;

        // Periodic field scan every 10 seconds (200 ticks)
        if (this.scanTimer >= 200) {
            this.scanTimer = 0;
            planHarvestRoute();

            if (!this.harvestQueue.isEmpty()) {
                this.currentCropTarget = this.harvestQueue.poll();
                this.state = RoutineState.HARVESTING;
                this.droid.getNavigation().moveTo(
                        this.currentCropTarget.getX() + 0.5D,
                        this.currentCropTarget.getY(),
                        this.currentCropTarget.getZ() + 0.5D,
                        this.speedModifier
                );
            } else if (hasItemsToDeposit() && this.droid.getLinkedChest().isPresent()) {
                this.state = RoutineState.DEPOSITING;
            }
        }
    }

    // Compiles all mature crops in the bounding box and sorts them via nearest-neighbor
    private void planHarvestRoute() {
        this.harvestQueue.clear();
        Level level = this.droid.level();

        int minX = this.droid.getEffectiveMinX();
        int maxX = this.droid.getEffectiveMaxX();
        int minZ = this.droid.getEffectiveMinZ();
        int maxZ = this.droid.getEffectiveMaxZ();
        int currentY = this.droid.getBlockY();

        List<BlockPos> found = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Check farmland and crop above within +-2 vertical range
                for (int y = currentY - 2; y <= currentY + 2; y++) {
                    BlockPos farmPos = new BlockPos(x, y, z);
                    BlockState farmState = level.getBlockState(farmPos);

                    if (farmState.is(Blocks.FARMLAND)) {
                        BlockPos cropPos = farmPos.above();
                        BlockState cropState = level.getBlockState(cropPos);

                        if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
                            found.add(cropPos);
                            break;
                        } else if (cropState.is(Blocks.PUMPKIN) || cropState.is(Blocks.MELON)) {
                            found.add(cropPos);
                            break;
                        }
                    }
                }
            }
        }

        if (found.isEmpty()) return;

        // Sort found targets with Nearest-Neighbor path heuristic for optimal fluid travel
        BlockPos currentPos = this.droid.blockPosition();
        while (!found.isEmpty()) {
            BlockPos nearest = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (BlockPos p : found) {
                double dSq = currentPos.distSqr(p);
                if (dSq < nearestDistSq) {
                    nearestDistSq = dSq;
                    nearest = p;
                }
            }

            if (nearest != null) {
                this.harvestQueue.add(nearest);
                found.remove(nearest);
                currentPos = nearest;
            }
        }
    }

    // 2. HARVESTING: Chain-harvests crops without pauses
    private void tickHarvesting() {
        if (this.currentCropTarget == null) {
            if (!this.harvestQueue.isEmpty()) {
                this.currentCropTarget = this.harvestQueue.poll();
            } else {
                finishHarvestingAndDecideNextState();
                return;
            }
        }

        double distSq = this.droid.distanceToSqr(
                this.currentCropTarget.getX() + 0.5D,
                this.currentCropTarget.getY(),
                this.currentCropTarget.getZ() + 0.5D
        );

        this.droid.getLookControl().setLookAt(
                this.currentCropTarget.getX() + 0.5D,
                this.currentCropTarget.getY() + 0.5D,
                this.currentCropTarget.getZ() + 0.5D
        );

        if (distSq <= 4.0D) { // Reached crop
            this.actionTicks++;
            this.droid.setWorking(true);

            // Fast, crisp mechanical action (only 6 ticks = 0.3s)
            if (this.actionTicks >= 6) {
                harvestAt(this.currentCropTarget);
                this.actionTicks = 0;
                this.droid.setWorking(false);

                // Instantly advance to next crop in queue (Zero delay!)
                if (!this.harvestQueue.isEmpty()) {
                    this.currentCropTarget = this.harvestQueue.poll();
                    this.droid.getNavigation().moveTo(
                            this.currentCropTarget.getX() + 0.5D,
                            this.currentCropTarget.getY(),
                            this.currentCropTarget.getZ() + 0.5D,
                            this.speedModifier
                    );
                } else {
                    this.currentCropTarget = null;
                    finishHarvestingAndDecideNextState();
                }
            }
        } else {
            // Repath periodically if path is done
            if (++this.repathTimer >= 20 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(
                        this.currentCropTarget.getX() + 0.5D,
                        this.currentCropTarget.getY(),
                        this.currentCropTarget.getZ() + 0.5D,
                        this.speedModifier
                );
            }
        }
    }

    private void finishHarvestingAndDecideNextState() {
        this.droid.setWorking(false);
        this.actionTicks = 0;

        if (hasItemsToDeposit() && this.droid.getLinkedChest().isPresent()) {
            this.state = RoutineState.DEPOSITING;
        } else {
            this.state = RoutineState.RETURNING;
        }
    }

    private void harvestAt(BlockPos cropPos) {
        if (!(this.droid.level() instanceof ServerLevel serverLevel)) return;

        BlockState cropState = serverLevel.getBlockState(cropPos);

        if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
            List<ItemStack> drops = Block.getDrops(cropState, serverLevel, cropPos, null, this.droid, ItemStack.EMPTY);
            serverLevel.setBlock(cropPos, crop.getStateForAge(0), 3);
            serverLevel.playSound(null, cropPos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.1F);

            for (ItemStack drop : drops) {
                ItemStack remaining = this.droid.getInventory().addItem(drop);
                if (!remaining.isEmpty()) {
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

    // 3. DEPOSITING: Walk straight to linked chest and deposit
    private void tickDepositing() {
        Optional<BlockPos> chestOpt = this.droid.getLinkedChest();
        if (chestOpt.isEmpty() || !hasItemsToDeposit()) {
            this.state = RoutineState.RETURNING;
            return;
        }

        BlockPos chestPos = chestOpt.get();
        double distSq = this.droid.distanceToSqr(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);
        this.droid.getLookControl().setLookAt(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);

        if (distSq <= 6.0D) {
            depositToChest(chestPos);
            this.state = RoutineState.RETURNING;
        } else {
            if (++this.repathTimer >= 20 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D, this.speedModifier);
            }
        }
    }

    private void depositToChest(BlockPos chestPos) {
        Level level = this.droid.level();
        BlockEntity be = level.getBlockEntity(chestPos);

        if (be instanceof Container targetContainer) {
            Container droidInv = this.droid.getInventory();
            boolean transferredAny = false;

            for (int i = 0; i < droidInv.getContainerSize(); i++) {
                ItemStack stack = droidInv.getItem(i);
                if (stack.isEmpty()) continue;

                int transferableAmount = 0;
                if (isProduce(stack.getItem())) {
                    transferableAmount = stack.getCount();
                } else if (isSeed(stack.getItem()) && stack.getCount() > 16) {
                    transferableAmount = stack.getCount() - 16;
                }

                if (transferableAmount > 0) {
                    ItemStack toTransfer = stack.split(transferableAmount);
                    ItemStack remaining = insertIntoContainer(targetContainer, toTransfer);

                    if (!remaining.isEmpty()) {
                        stack.grow(remaining.getCount());
                    } else {
                        transferredAny = true;
                    }
                }
            }

            if (transferredAny) {
                level.playSound(null, chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
        }
    }

    private ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack inSlot = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    inSlot.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    // 4. RETURNING: Return to Home dock and enter standby
    private void tickReturning() {
        BlockPos homePos = this.droid.getHomePosition();
        double distSq = this.droid.distanceToSqr(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D);

        if (distSq <= 3.0D) {
            // Arrived home! Freeze navigation and enter standby
            this.droid.getNavigation().stop();
            this.state = RoutineState.IDLE;
            this.scanTimer = 0;
        } else {
            if (++this.repathTimer >= 20 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, this.speedModifier * 0.9D);
            }
        }
    }

    private boolean hasItemsToDeposit() {
        Container inv = this.droid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (isProduce(stack.getItem())) return true;
                if (isSeed(stack.getItem()) && stack.getCount() > 16) return true;
            }
        }
        return false;
    }

    private boolean isProduce(Item item) {
        return item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO
                || item == Items.BEETROOT || item == Items.PUMPKIN || item == Items.MELON_SLICE;
    }

    private boolean isSeed(Item item) {
        return item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS
                || item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS;
    }
}
