package dev.droids.menu;

import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModItems;
import dev.droids.init.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public class MinerDroidMenu extends AbstractContainerMenu {

    private final MinerDroidEntity droid;
    private final ContainerData data;

    public MinerDroidMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInv, getDroidFromBuf(playerInv, extraData), new SimpleContainerData(10));
    }

    public MinerDroidMenu(int containerId, Inventory playerInv, @Nullable MinerDroidEntity droid, ContainerData data) {
        super(ModMenuTypes.MINER_DROID_MENU.get(), containerId);
        this.droid = droid;
        this.data = data;
        this.addDataSlots(data);

        Container fuelInv = droid != null ? droid.getFuelInventory() : null;
        Container pickInv = droid != null ? droid.getPickaxeInventory() : null;
        Container upgradeInv = droid != null ? droid.getUpgradeInventory() : null;
        Container storageInv = droid != null ? droid.getInventory() : null;

        // Slot 0: Fuel Slot (x=26, y=20)
        if (fuelInv != null) {
            this.addSlot(new Slot(fuelInv, 0, 26, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getBurnTime(RecipeType.SMELTING) > 0 || stack.is(net.minecraft.world.item.Items.COAL) || stack.is(net.minecraft.world.item.Items.CHARCOAL);
                }
            });
        }

        // Slot 1: Pickaxe Slot (x=26, y=42)
        if (pickInv != null) {
            this.addSlot(new Slot(pickInv, 0, 26, 42) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ItemTags.PICKAXES) || stack.getItem() instanceof DiggerItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Slot 2: Range Upgrade Slot (x=26, y=64)
        if (upgradeInv != null) {
            this.addSlot(new Slot(upgradeInv, 0, 26, 64) {
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

        // Slots 3..11: Droid 3x3 Storage Inventory (x=98, y=18)
        if (storageInv != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    this.addSlot(new Slot(storageInv, col + row * 3, 98 + col * 18, 18 + row * 18));
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

    private static @Nullable MinerDroidEntity getDroidFromBuf(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        Entity entity = playerInv.player.level().getEntity(entityId);
        return entity instanceof MinerDroidEntity d ? d : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.droid != null) {
            this.droid.toggleMode();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public @Nullable MinerDroidEntity getDroid() {
        return this.droid;
    }

    public int getFuelSeconds() {
        return this.data.get(0) / 20;
    }

    public int getMode() {
        return this.data.get(1);
    }

    public int getPickaxeDurability() {
        return this.data.get(2);
    }

    public int getPickaxeMaxDurability() {
        return this.data.get(3);
    }

    public int getUpgradeCount() {
        return this.data.get(4);
    }

    public boolean hasChest() {
        return this.data.get(5) == 1;
    }

    public int getChestX() {
        return this.data.get(6);
    }

    public int getChestY() {
        return this.data.get(7);
    }

    public int getChestZ() {
        return this.data.get(8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int totalDroidSlots = 12; // 0 (fuel) + 1 (pickaxe) + 2 (upgrade) + 9 (storage)

            if (index < totalDroidSlots) {
                if (!this.moveItemStackTo(stackInSlot, totalDroidSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.is(ItemTags.PICKAXES) || stackInSlot.getItem() instanceof DiggerItem) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        if (!this.moveItemStackTo(stackInSlot, 3, totalDroidSlots, false)) return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(ModItems.RANGE_UPGRADE.get())) {
                    if (!this.moveItemStackTo(stackInSlot, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.getBurnTime(RecipeType.SMELTING) > 0) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        if (!this.moveItemStackTo(stackInSlot, 3, totalDroidSlots, false)) return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stackInSlot, 3, totalDroidSlots, false)) {
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
