package dev.droids.menu;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.init.ModItems;
import dev.droids.init.ModMenuTypes;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

public class DroidStationMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    public DroidStationMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(DroidStationBlockEntity.TOTAL_SLOTS), new SimpleContainerData(4));
    }

    public DroidStationMenu(int containerId, Inventory playerInv, Container container, ContainerData data) {
        super(ModMenuTypes.DROID_STATION_MENU.get(), containerId);
        checkContainerSize(container, DroidStationBlockEntity.TOTAL_SLOTS);
        this.container = container;
        this.data = data;
        this.addDataSlots(data);

        // Slot 0: Fuel Slot (x=26, y=20)
        this.addSlot(new Slot(container, DroidStationBlockEntity.SLOT_FUEL, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getBurnTime(RecipeType.SMELTING) > 0 || stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            }
        });

        // Slot 1: Tool Slot (x=26, y=42)
        this.addSlot(new Slot(container, DroidStationBlockEntity.SLOT_TOOL, 26, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.PICKAXES) || stack.getItem() instanceof DiggerItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 2: Range Upgrade Slot (x=26, y=64)
        this.addSlot(new Slot(container, DroidStationBlockEntity.SLOT_UPGRADE, 26, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RANGE_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 4;
            }
        });

        // Slots 3..11: 3x3 Output Buffer Grid (x=98, y=18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = DroidStationBlockEntity.OUTPUT_START + col + row * 3;
                this.addSlot(new Slot(container, slotIndex, 98 + col * 18, 18 + row * 18));
            }
        }

        // Player Inventory (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 cols)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.container instanceof DroidStationBlockEntity station) {
            if (id == 0) {
                station.toggleMode();
                return true;
            } else if (id == 1) {
                station.cycleWorkDirection();
                return true;
            }
        }
        return super.clickMenuButton(player, id);
    }

    public int getMode() {
        return this.data.get(0);
    }

    public Direction getWorkDirection() {
        return Direction.from2DDataValue(this.data.get(1));
    }

    public int getUpgradeCount() {
        return this.data.get(2);
    }

    public boolean hasAssignedDroid() {
        return this.data.get(3) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int totalStationSlots = DroidStationBlockEntity.TOTAL_SLOTS; // 12

            if (index < totalStationSlots) {
                // Moving from station to player inventory
                if (!this.moveItemStackTo(stackInSlot, totalStationSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to station
                if (stackInSlot.is(ItemTags.PICKAXES) || stackInSlot.getItem() instanceof DiggerItem) {
                    if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.SLOT_TOOL, DroidStationBlockEntity.SLOT_TOOL + 1, false)) {
                        if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.OUTPUT_START, totalStationSlots, false)) return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.RANGE_UPGRADE.get())) {
                    if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.SLOT_UPGRADE, DroidStationBlockEntity.SLOT_UPGRADE + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getBurnTime(RecipeType.SMELTING) > 0 || stackInSlot.is(Items.COAL) || stackInSlot.is(Items.CHARCOAL)) {
                    if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.SLOT_FUEL, DroidStationBlockEntity.SLOT_FUEL + 1, false)) {
                        if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.OUTPUT_START, totalStationSlots, false)) return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stackInSlot, DroidStationBlockEntity.OUTPUT_START, totalStationSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
