package dev.droids.menu;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Handheld Droid Telemetry Tablet.
 * Clean, simple remote inspection of Droid and Station status.
 */
public class DroidControllerMenu extends AbstractContainerMenu {

    private final BlockPos stationPos;
    private final ContainerData data;

    // Client-side constructor
    public DroidControllerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        super(ModMenuTypes.DROID_CONTROLLER_MENU.get(), containerId);
        this.stationPos = extraData.readBlockPos();
        this.data = new SimpleContainerData(10);
        this.addDataSlots(data);
        addPlayerSlots(playerInv);
    }

    // Server-side constructor
    public DroidControllerMenu(int containerId, Inventory playerInv, BlockPos stationPos, ItemStack controllerStack) {
        super(ModMenuTypes.DROID_CONTROLLER_MENU.get(), containerId);
        this.stationPos = stationPos;
        this.data = createServerData(playerInv.player, stationPos);
        this.addDataSlots(data);
        addPlayerSlots(playerInv);
    }

    private void addPlayerSlots(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    private static ContainerData createServerData(Player player, BlockPos stationPos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (!(player.level() instanceof ServerLevel serverLevel)) return 0;
                if (!(serverLevel.getBlockEntity(stationPos) instanceof DroidStationBlockEntity station)) return 0;
                Entity droid = station.getAssignedDroid(serverLevel);
                return switch (index) {
                    case 0 -> 1; // hasStation
                    case 1 -> droid instanceof MinerDroidEntity ? 1 : (droid instanceof FarmerDroidEntity ? 2 : 0);
                    case 2 -> { // Status code: 0 = Standby/Idle, 1 = Working, 2 = Unpowered, 3 = No Tool
                        if (droid instanceof dev.droids.entity.AbstractDroidEntity d) {
                            if (!d.hasFuel()) yield 2;
                            if (d instanceof MinerDroidEntity miner && !miner.hasValidPickaxe()) yield 3;
                            if (d.isWorking()) yield 1;
                            yield 0;
                        }
                        yield 0;
                    }
                    case 3 -> droid instanceof dev.droids.entity.AbstractDroidEntity d ? d.getFuelTicks() / 20 : 0;
                    case 4 -> droid != null ? (int) Math.sqrt(droid.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY(), stationPos.getZ() + 0.5D)) : 0;
                    case 5 -> { // Pickaxe durability
                        if (droid instanceof MinerDroidEntity miner && miner.hasValidPickaxe()) {
                            ItemStack pick = miner.getPickaxe();
                            if (pick.isDamageableItem()) {
                                int rem = pick.getMaxDamage() - pick.getDamageValue();
                                yield Math.max(0, (rem * 100) / pick.getMaxDamage());
                            }
                            yield 100;
                        }
                        yield 0;
                    }
                    case 6 -> station.getDataAccess().get(2); // Upgrade count
                    case 7 -> stationPos.getX();
                    case 8 -> stationPos.getY();
                    case 9 -> stationPos.getZ();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() { return 10; }
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getBlockEntity(this.stationPos) instanceof DroidStationBlockEntity station) {
                if (id == 0) {
                    station.recallAssignedDroid(serverLevel);
                    return true;
                }
            }
        }
        return super.clickMenuButton(player, id);
    }

    // ── Getters (called from Screen) ─────────────────────────────────────────
    public BlockPos getStationPos()          { return this.stationPos; }
    public boolean hasStation()              { return this.data.get(0) == 1; }
    public int getDroidType()                { return this.data.get(1); }
    public int getDroidStatus()              { return this.data.get(2); }
    public int getFuelSeconds()              { return this.data.get(3); }
    public int getDistance()                 { return this.data.get(4); }
    public int getPickaxeDurabilityPercent() { return this.data.get(5); }
    public int getUpgradeCount()             { return this.data.get(6); }
    public int getStationX()                 { return this.data.get(7); }
    public int getStationY()                 { return this.data.get(8); }
    public int getStationZ()                 { return this.data.get(9); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
