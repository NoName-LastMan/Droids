package dev.droids.entity.ai;

import dev.droids.entity.AbstractDroidEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Base Abstract AI Goal for all Droids.
 * Provides shared routines:
 * - Fuel operational checks
 * - Container stack insertion utilities
 * - Full inventory checks
 */
public abstract class AbstractDroidGoal<T extends AbstractDroidEntity> extends Goal {

    protected final T droid;
    protected final double speedModifier;

    public AbstractDroidGoal(T droid, double speedModifier) {
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

    /**
     * Shared utility for merging or inserting item stacks into a target container.
     */
    protected ItemStack insertIntoContainer(Container container, ItemStack stack) {
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

    /**
     * Checks if the Droid's internal buffer inventory is completely full.
     */
    protected boolean isInventoryFull() {
        Container inv = this.droid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}
