package dev.droids.menu;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModMenuTypes;
import dev.droids.item.DroidControllerItem;
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

    // Pending area corners (from client extraData buffer)
    private final boolean hasC1;
    private final boolean hasC2;
    private final int c1x, c1y, c1z;
    private final int c2x, c2y, c2z;

    // ── Client-side constructor (from buf) ──────────────────────────────────────
    public DroidControllerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        super(ModMenuTypes.DROID_CONTROLLER_MENU.get(), containerId);
        this.stationPos = extraData.readBlockPos();
        this.data = new SimpleContainerData(14);
        this.addDataSlots(data);

        this.hasC1 = extraData.readBoolean();
        if (this.hasC1) {
            this.c1x = extraData.readInt();
            this.c1y = extraData.readInt();
            this.c1z = extraData.readInt();
        } else { this.c1x = 0; this.c1y = 0; this.c1z = 0; }

        this.hasC2 = extraData.readBoolean();
        if (this.hasC2) {
            this.c2x = extraData.readInt();
            this.c2y = extraData.readInt();
            this.c2z = extraData.readInt();
        } else { this.c2x = 0; this.c2y = 0; this.c2z = 0; }

        addPlayerSlots(playerInv);
    }

    // ── Server-side constructor ─────────────────────────────────────────────────
    public DroidControllerMenu(int containerId, Inventory playerInv, BlockPos stationPos, ItemStack controllerStack) {
        super(ModMenuTypes.DROID_CONTROLLER_MENU.get(), containerId);
        this.stationPos = stationPos;
        this.data = createServerData(playerInv.player, stationPos);
        this.addDataSlots(data);
        this.hasC1 = false; this.hasC2 = false;
        this.c1x = 0; this.c1y = 0; this.c1z = 0;
        this.c2x = 0; this.c2y = 0; this.c2z = 0;
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
                    case 0 -> 1;
                    case 1 -> droid instanceof MinerDroidEntity ? 1 : (droid instanceof FarmerDroidEntity ? 2 : 0);
                    case 2 -> {
                        if (droid instanceof MinerDroidEntity miner) {
                            if (!miner.hasFuel()) yield 3;
                            if (miner.isAscending()) yield 2;
                            if (miner.isWorking()) yield 1;
                            yield 0;
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
                    case 9 -> 20;
                    case 10 -> droid != null ? (int) droid.getX() : stationPos.getX();
                    case 11 -> droid != null ? (int) droid.getY() : stationPos.getY();
                    case 12 -> droid != null ? (int) droid.getZ() : stationPos.getZ();
                    case 13 -> station.getDataAccess().get(2);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() { return 14; }
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getBlockEntity(this.stationPos) instanceof DroidStationBlockEntity station) {
                if (id == 0) { station.toggleMode(); return true; }
                if (id == 1) { station.cycleWorkDirection(); return true; }
                if (id == 2) { station.recallAssignedDroid(serverLevel); return true; }
            }
            // id == 3: Apply area from Controller item in hand to the droid
            if (id == 3) {
                return DroidControllerItem.applyAreaToDroid(player, this.stationPos);
            }
        }
        return super.clickMenuButton(player, id);
    }

    // ── Getters (called from Screen) ─────────────────────────────────────────
    public BlockPos getStationPos() { return this.stationPos; }
    public boolean hasStation()     { return this.data.get(0) == 1; }
    public int getDroidType()       { return this.data.get(1); }
    public int getDroidStatus()     { return this.data.get(2); }
    public int getFuelSeconds()     { return this.data.get(3); }
    public int getDistance()        { return this.data.get(4); }
    public int getMode()            { return this.data.get(5); }
    public Direction getWorkDirection() { return Direction.from2DDataValue(this.data.get(6)); }
    public int getPickaxeDurabilityPercent() { return this.data.get(7); }
    public int getQuarryDepth()     { return this.data.get(8); }
    public int getMaxDepth()        { return this.data.get(9); }
    public int getDroidX()          { return this.data.get(10); }
    public int getDroidY()          { return this.data.get(11); }
    public int getDroidZ()          { return this.data.get(12); }
    public int getUpgradeCount()    { return this.data.get(13); }

    // Corner data (sent from server via extra buf, used by screen for preview)
    public boolean hasCorner1() { return this.hasC1; }
    public boolean hasCorner2() { return this.hasC2; }
    public int getC1X() { return c1x; }
    public int getC1Z() { return c1z; }
    public int getC2X() { return c2x; }
    public int getC2Z() { return c2z; }
    public int getAreaWidth()  { return hasC1 && hasC2 ? Math.abs(c2x - c1x) + 1 : 0; }
    public int getAreaLength() { return hasC1 && hasC2 ? Math.abs(c2z - c1z) + 1 : 0; }

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

