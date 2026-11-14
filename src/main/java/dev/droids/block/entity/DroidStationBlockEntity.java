package dev.droids.block.entity;

import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModBlockEntities;
import dev.droids.init.ModItems;
import dev.droids.menu.DroidStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * BlockEntity for the Droid Station.
 * Acts as the physical docking pad, fuel intake, tool supplier, and output buffer.
 * Fully compatible with Hoppers and modded pipes!
 */
public class DroidStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_TOOL = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 9;
    public static final int TOTAL_SLOTS = 12;

    private NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private @Nullable UUID assignedDroidUUID = null;
    private int assignedDroidEntityId = -1;

    // Station Settings
    private int mode = 0; // 0 = Tunnel, 1 = Quarry
    private Direction workDirection = Direction.NORTH;
    private int workRadiusTier = 0; // 0..4 based on upgrades

    public DroidStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROID_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DroidStationBlockEntity station) {
        station.tickStation((ServerLevel) level);
    }

    private void tickStation(ServerLevel level) {
        // Find assigned Droid if present
        Entity droidEntity = getAssignedDroid(level);

        if (droidEntity instanceof MinerDroidEntity miner) {
            // Check if Droid is docked on top of station (dist <= 1.5 blocks)
            double distSq = miner.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D);
            if (distSq <= 2.5D) {
                // 1. Refuel Droid from Station's Fuel Slot
                if (miner.getFuelTicks() < 3000) {
                    ItemStack fuelStack = this.items.get(SLOT_FUEL);
                    if (!fuelStack.isEmpty()) {
                        int burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
                        if (burnTime <= 0 && (fuelStack.is(Items.COAL) || fuelStack.is(Items.CHARCOAL))) burnTime = 1600;
                        if (burnTime > 0) {
                            miner.setFuelTicks(miner.getFuelTicks() + burnTime);
                            fuelStack.shrink(1);
                            level.playSound(null, this.worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.2F);
                            setChanged();
                        }
                    }
                }

                // 2. Supply Tool to Droid if Droid has no pickaxe
                if (!miner.hasValidPickaxe()) {
                    ItemStack toolStack = this.items.get(SLOT_TOOL);
                    if (!toolStack.isEmpty() && (toolStack.is(ItemTags.PICKAXES) || toolStack.getItem() instanceof DiggerItem)) {
                        miner.getPickaxeInventory().setItem(0, toolStack.copy());
                        this.items.set(SLOT_TOOL, ItemStack.EMPTY);
                        level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        setChanged();
                    }
                }

                // 3. Unload Droid's internal inventory into Station's 9 Output Buffer slots
                for (int i = 0; i < miner.getInventory().getContainerSize(); i++) {
                    ItemStack mined = miner.getInventory().getItem(i);
                    if (!mined.isEmpty()) {
                        ItemStack remaining = insertIntoOutput(mined);
                        miner.getInventory().setItem(i, remaining);
                        if (remaining.getCount() != mined.getCount()) {
                            setChanged();
                        }
                    }
                }

                // 4. Sync Upgrades from Station to Droid
                ItemStack upgradeStack = this.items.get(SLOT_UPGRADE);
                miner.getUpgradeInventory().setItem(0, upgradeStack.copy());
                miner.setMode(this.mode);
            }
        } else if (droidEntity instanceof dev.droids.entity.FarmerDroidEntity farmer) {
            double distSq = farmer.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D, this.worldPosition.getZ() + 0.5D);
            if (distSq <= 2.5D) {
                // 1. Refuel Farmer from Station
                if (farmer.getFuelTicks() < 3000) {
                    ItemStack fuelStack = this.items.get(SLOT_FUEL);
                    if (!fuelStack.isEmpty()) {
                        int burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
                        if (burnTime <= 0 && (fuelStack.is(Items.COAL) || fuelStack.is(Items.CHARCOAL))) burnTime = 1600;
                        if (burnTime > 0) {
                            farmer.setFuelTicks(farmer.getFuelTicks() + burnTime);
                            fuelStack.shrink(1);
                            level.playSound(null, this.worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.2F);
                            setChanged();
                        }
                    }
                }

                // 2. Unload produce into Station Output Buffer
                for (int i = 0; i < farmer.getInventory().getContainerSize(); i++) {
                    ItemStack crop = farmer.getInventory().getItem(i);
                    if (!crop.isEmpty()) {
                        ItemStack remaining = insertIntoOutput(crop);
                        farmer.getInventory().setItem(i, remaining);
                        if (remaining.getCount() != crop.getCount()) {
                            setChanged();
                        }
                    }
                }

                // 3. Sync Upgrades
                ItemStack upgradeStack = this.items.get(SLOT_UPGRADE);
                farmer.getUpgradeInventory().setItem(0, upgradeStack.copy());
            }
        }
    }

    public void recallAssignedDroid(ServerLevel level) {
        Entity droid = getAssignedDroid(level);
        if (droid instanceof MinerDroidEntity miner) {
            miner.requestRecall();
        }
    }

    private ItemStack insertIntoOutput(ItemStack stack) {
        // Try merge into existing stacks
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack inSlot = this.items.get(i);
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
        // Try empty slots
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public @Nullable Entity getAssignedDroid(ServerLevel level) {
        if (this.assignedDroidUUID != null) {
            Entity e = level.getEntity(this.assignedDroidUUID);
            if (e != null && e.isAlive()) {
                this.assignedDroidEntityId = e.getId();
                return e;
            }
        }
        if (this.assignedDroidEntityId != -1) {
            Entity e = level.getEntity(this.assignedDroidEntityId);
            if (e != null && e.isAlive()) return e;
        }
        return null;
    }

    public void setAssignedDroid(Entity droid) {
        this.assignedDroidUUID = droid.getUUID();
        this.assignedDroidEntityId = droid.getId();
        setChanged();
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        setChanged();
    }

    public void toggleMode() {
        this.mode = this.mode == 0 ? 1 : 0;
        setChanged();
    }

    public Direction getWorkDirection() {
        return this.workDirection;
    }

    public void cycleWorkDirection() {
        this.workDirection = this.workDirection.getClockWise();
        setChanged();
    }

    // WorldlyContainer (Sided Hopper/Pipe Automation)
    private static final int[] SLOTS_TOP = new int[]{SLOT_TOOL, SLOT_UPGRADE};
    private static final int[] SLOTS_SIDES = new int[]{SLOT_FUEL};
    private static final int[] SLOTS_BOTTOM = new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11};

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return SLOTS_TOP;
        if (side == Direction.DOWN) return SLOTS_BOTTOM;
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) {
        if (index == SLOT_FUEL) {
            return stack.getBurnTime(RecipeType.SMELTING) > 0 || stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
        }
        if (index == SLOT_TOOL) {
            return stack.is(ItemTags.PICKAXES) || stack.getItem() instanceof DiggerItem;
        }
        if (index == SLOT_UPGRADE) {
            return stack.is(ModItems.RANGE_UPGRADE.get());
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
        // Only extract from output slots through the bottom
        return side == Direction.DOWN && index >= OUTPUT_START;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.droids.droid_station");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DroidStationMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mode;
                case 1 -> workDirection.get2DDataValue();
                case 2 -> items.get(SLOT_UPGRADE).getCount();
                case 3 -> assignedDroidUUID != null ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) mode = value;
            else if (index == 1) workDirection = Direction.from2DDataValue(value);
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        if (this.assignedDroidUUID != null) {
            tag.putUUID("AssignedDroid", this.assignedDroidUUID);
        }
        tag.putInt("Mode", this.mode);
        tag.putInt("WorkDir", this.workDirection.get2DDataValue());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        if (tag.hasUUID("AssignedDroid")) {
            this.assignedDroidUUID = tag.getUUID("AssignedDroid");
        }
        this.mode = tag.getInt("Mode");
        if (tag.contains("WorkDir")) {
            this.workDirection = Direction.from2DDataValue(tag.getInt("WorkDir"));
        }
    }
}
