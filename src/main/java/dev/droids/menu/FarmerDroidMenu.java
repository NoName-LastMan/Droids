package dev.droids.menu;

import dev.droids.entity.FarmerDroidEntity;
import dev.droids.init.ModItems;
import dev.droids.init.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public class FarmerDroidMenu extends AbstractContainerMenu {

    private final FarmerDroidEntity droid;
    private final ContainerData data;

    // Client-side constructor
    public FarmerDroidMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInv, getDroidFromBuf(playerInv, extraData), new SimpleContainerData(11));
    }

    // Server-side constructor
    public FarmerDroidMenu(int containerId, Inventory playerInv, @Nullable FarmerDroidEntity droid, ContainerData data) {
        super(ModMenuTypes.FARMER_DROID_MENU.get(), containerId);
        this.droid = droid;
        this.data = data;
        this.addDataSlots(data);

        Container fuelInv = droid != null ? droid.getFuelInventory() : null;
        Container upgradeInv = droid != null ? droid.getUpgradeInventory() : null;
        Container droidInv = droid != null ? droid.getInventory() : null;

        // Slot 0: Fuel slot (x=26, y=26)
        if (fuelInv != null) {
            this.addSlot(new Slot(fuelInv, 0, 26, 26) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getBurnTime(RecipeType.SMELTING) > 0 || stack.is(net.minecraft.world.item.Items.COAL) || stack.is(net.minecraft.world.item.Items.CHARCOAL);
                }
            });
        }

        // Slot 1: Range Upgrade slot (x=26, y=56)
        if (upgradeInv != null) {
            this.addSlot(new Slot(upgradeInv, 0, 26, 56) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.RANGE_UPGRADE.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 4;
                }
            });
        }

        // Slots 2..10: Droid 3x3 Storage Inventory (x=98, y=18)
        if (droidInv != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    this.addSlot(new Slot(droidInv, col + row * 3, 98 + col * 18, 18 + row * 18));
                }
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

    private static @Nullable FarmerDroidEntity getDroidFromBuf(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        Entity entity = playerInv.player.level().getEntity(entityId);
        return entity instanceof FarmerDroidEntity d ? d : null;
    }

    public @Nullable FarmerDroidEntity getDroid() {
        return this.droid;
    }

    public ContainerData getData() {
        return this.data;
    }

    public int getFuelSeconds() {
        return this.data.get(0) / 20;
    }

    public int getUpgradeCount() {
        return this.data.get(1);
    }

    public boolean hasArea() {
        return this.data.get(2) == 1;
    }

    public int getAreaMinX() {
        return this.data.get(3);
    }

    public int getAreaMinZ() {
        return this.data.get(4);
    }

    public int getAreaMaxX() {
        return this.data.get(5);
    }

    public int getAreaMaxZ() {
        return this.data.get(6);
    }

    public boolean hasChest() {
        return this.data.get(7) == 1;
    }

    public int getChestX() {
        return this.data.get(8);
    }

    public int getChestY() {
        return this.data.get(9);
    }

    public int getChestZ() {
        return this.data.get(10);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int totalDroidSlots = 11; // 0 (fuel) + 1 (upgrade) + 9 (inventory)

            if (index < totalDroidSlots) {
                // Move from droid to player inventory
                if (!this.moveItemStackTo(stackInSlot, totalDroidSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player to droid:
                if (stackInSlot.is(ModItems.RANGE_UPGRADE.get())) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getBurnTime(RecipeType.SMELTING) > 0) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        if (!this.moveItemStackTo(stackInSlot, 2, totalDroidSlots, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(stackInSlot, 2, totalDroidSlots, false)) {
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
        return this.droid != null && this.droid.isAlive() && this.droid.distanceTo(player) < 8.0F;
    }
}
