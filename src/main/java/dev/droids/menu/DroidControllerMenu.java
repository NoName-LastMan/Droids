package dev.droids.menu;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class DroidControllerMenu extends AbstractContainerMenu {

    private final BlockPos stationPos;
    private final ContainerData data;

    public DroidControllerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readBlockPos(), new SimpleContainerData(14));
    }

    public DroidControllerMenu(int containerId, Inventory playerInv, BlockPos stationPos) {
        this(containerId, playerInv, stationPos, createServerData(playerInv.player, stationPos));
    }

    public DroidControllerMenu(int containerId, Inventory playerInv, BlockPos stationPos, ContainerData data) {
        super(ModMenuTypes.DROID_CONTROLLER_MENU.get(), containerId);
        this.stationPos = stationPos;
        this.data = data;
        this.addDataSlots(data);

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
                    case 2 -> {
                        if (droid instanceof MinerDroidEntity miner) {
                            if (!miner.hasFuel()) yield 3; // No fuel
                            if (miner.isAscending()) yield 2; // Ascending
                            if (miner.isWorking()) yield 1; // Mining
                            yield 0; // Standby
                        } else if (droid instanceof FarmerDroidEntity farmer) {
                            if (!farmer.hasFuel()) yield 3;
                            if (farmer.isWorking()) yield 1;
                            yield 0;
                        }
                        yield 0;
                    }
                    case 3 -> {
                        if (droid instanceof MinerDroidEntity miner) yield miner.getFuelTicks() / 20;
                        if (droid instanceof FarmerDroidEntity farmer) yield farmer.getFuelTicks() / 20;
                        yield 0;
                    }
                    case 4 -> droid != null ? (int) Math.sqrt(droid.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY(), stationPos.getZ() + 0.5D)) : 0;
                    case 5 -> station.getMode();
                    case 6 -> station.getWorkDirection().get2DDataValue();
                    case 7 -> {
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
                    case 8 -> {
                        if (droid instanceof MinerDroidEntity miner) {
                            int depth = miner.getStartY() - miner.getBlockY();
                            yield Math.max(0, Math.min(20, depth));
                        }
                        yield 0;
                    }
                    case 9 -> 20; // Max allowed depth limit
                    case 10 -> droid != null ? (int) droid.getX() : stationPos.getX();
                    case 11 -> droid != null ? (int) droid.getY() : stationPos.getY();
                    case 12 -> droid != null ? (int) droid.getZ() : stationPos.getZ();
                    case 13 -> station.getDataAccess().get(2); // Upgrades
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 14;
            }
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getBlockEntity(this.stationPos) instanceof DroidStationBlockEntity station) {
                if (id == 0) {
                    station.toggleMode();
                    return true;
                } else if (id == 1) {
                    station.cycleWorkDirection();
                    return true;
                } else if (id == 2) {
                    station.recallAssignedDroid(serverLevel);
                    return true;
                }
            }
        }
        return super.clickMenuButton(player, id);
    }

    public BlockPos getStationPos() {
        return this.stationPos;
    }

    public boolean hasStation() {
        return this.data.get(0) == 1;
    }

    public int getDroidType() {
        return this.data.get(1); // 0=None, 1=Miner, 2=Farmer
    }

    public int getDroidStatus() {
        return this.data.get(2); // 0=Standby, 1=Active, 2=Thrusters, 3=Unpowered
    }

    public int getFuelSeconds() {
        return this.data.get(3);
    }

    public int getDistance() {
        return this.data.get(4);
    }

    public int getMode() {
        return this.data.get(5);
    }

    public Direction getWorkDirection() {
        return Direction.from2DDataValue(this.data.get(6));
    }

    public int getPickaxeDurabilityPercent() {
        return this.data.get(7);
    }

    public int getQuarryDepth() {
        return this.data.get(8);
    }

    public int getMaxDepth() {
        return this.data.get(9);
    }

    public int getDroidX() {
        return this.data.get(10);
    }

    public int getDroidY() {
        return this.data.get(11);
    }

    public int getDroidZ() {
        return this.data.get(12);
    }

    public int getUpgradeCount() {
        return this.data.get(13);
    }

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
    public boolean stillValid(Player player) {
        return true;
    }
}
