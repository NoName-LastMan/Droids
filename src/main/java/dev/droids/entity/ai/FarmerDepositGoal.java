package dev.droids.entity.ai;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.Optional;

/**
 * AI Goal for Farmer Droid: navigates to its linked storage chest/barrel and deposits
 * harvested yields while keeping a working reserve of seeds for replanting.
 */
public class FarmerDepositGoal extends Goal {

    private final FarmerDroidEntity droid;
    private final double speedModifier;
    private int checkCooldown = 0;

    public FarmerDepositGoal(FarmerDroidEntity droid, double speedModifier) {
        this.droid = droid;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.droid.hasFuel()) {
            return false;
        }

        if (--this.checkCooldown > 0) {
            return false;
        }
        this.checkCooldown = 40; // check every 2 seconds

        Optional<BlockPos> chestPos = this.droid.getLinkedChest();
        if (chestPos.isEmpty()) {
            return false;
        }

        // Only deposit if inventory has harvest items
        return hasItemsToDeposit();
    }

    private boolean hasItemsToDeposit() {
        Container inv = this.droid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (isProduce(stack.getItem())) {
                    return true;
                }
                if (isSeed(stack.getItem()) && stack.getCount() > 16) {
                    return true;
                }
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

    @Override
    public boolean canContinueToUse() {
        Optional<BlockPos> chestPos = this.droid.getLinkedChest();
        return chestPos.isPresent() && hasItemsToDeposit() && this.droid.hasFuel();
    }

    @Override
    public void start() {
        this.droid.getLinkedChest().ifPresent(pos ->
                this.droid.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, this.speedModifier)
        );
    }

    @Override
    public void tick() {
        Optional<BlockPos> optPos = this.droid.getLinkedChest();
        if (optPos.isEmpty()) return;

        BlockPos chestPos = optPos.get();
        double distSq = this.droid.distanceToSqr(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);

        this.droid.getLookControl().setLookAt(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);

        if (distSq <= 6.0D) { // within deposit distance
            depositItems(chestPos);
        } else if (this.droid.getNavigation().isDone()) {
            this.droid.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D, this.speedModifier);
        }
    }

    private void depositItems(BlockPos chestPos) {
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
        // Try merging into existing matching stacks
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack inSlot = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    inSlot.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Try empty slots
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }
}
