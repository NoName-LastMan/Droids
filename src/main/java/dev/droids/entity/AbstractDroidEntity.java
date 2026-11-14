package dev.droids.entity;

import dev.droids.block.DroidStationBlock;
import dev.droids.init.ModBlocks;
import dev.droids.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Base Abstract Class for all autonomous Clockwork Droids.
 * Encapsulates shared mechanical behaviors:
 * - Fuel & thermal power consumption
 * - Droid Station docking & work area coordinates
 * - Internal buffer inventory, fuel slot, and upgrade slot
 * - Safe dismantling via Droid Wrench
 * - NBT persistence & telemetry communication
 */
public abstract class AbstractDroidEntity extends PathfinderMob {

    protected static final EntityDataAccessor<Optional<BlockPos>> LINKED_CHEST =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    protected static final EntityDataAccessor<Optional<BlockPos>> HOME_POS =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    protected static final EntityDataAccessor<Integer> FUEL_TICKS =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> IS_WORKING =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.BOOLEAN);

    protected static final EntityDataAccessor<Boolean> HAS_WORK_AREA =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> AREA_MIN_X =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> AREA_MIN_Z =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> AREA_MAX_X =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> AREA_MAX_Z =
            SynchedEntityData.defineId(AbstractDroidEntity.class, EntityDataSerializers.INT);

    // Common Inventories
    protected final SimpleContainer inventory = new SimpleContainer(9);
    protected final SimpleContainer fuelInventory = new SimpleContainer(1);
    protected final SimpleContainer upgradeInventory = new SimpleContainer(1);

    protected AbstractDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createBaseDroidAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LINKED_CHEST, Optional.empty());
        builder.define(HOME_POS, Optional.empty());
        builder.define(FUEL_TICKS, 0);
        builder.define(IS_WORKING, false);
        builder.define(HAS_WORK_AREA, false);
        builder.define(AREA_MIN_X, 0);
        builder.define(AREA_MIN_Z, 0);
        builder.define(AREA_MAX_X, 0);
        builder.define(AREA_MAX_Z, 0);
    }

    // ─── Polymorphic Abstract Methods ──────────────────────────────────────────

    /**
     * The item dropped when this Droid is dismantled.
     */
    public abstract Item getDropItem();

    /**
     * User-facing localized name of this Droid type.
     */
    public abstract Component getDroidTypeName();

    /**
     * Maximum allowed area dimension (width/length) based on installed range upgrades.
     */
    public abstract int getMaxAllowedAreaDimension();

    /**
     * Subclass-specific status string for telemetry and interaction.
     */
    public abstract String getSpecificStatus();

    // ─── Fuel & Power ──────────────────────────────────────────────────────────

    public int getFuelTicks() {
        return this.entityData.get(FUEL_TICKS);
    }

    public void setFuelTicks(int fuelTicks) {
        this.entityData.set(FUEL_TICKS, Math.max(0, fuelTicks));
    }

    public boolean hasFuel() {
        return getFuelTicks() > 0;
    }

    public boolean isWorking() {
        return this.entityData.get(IS_WORKING);
    }

    public void setWorking(boolean working) {
        this.entityData.set(IS_WORKING, working);
    }

    protected void tryEmergencyRefuel() {
        // 1. Try from dedicated Fuel Slot
        ItemStack fuelSlot = this.fuelInventory.getItem(0);
        if (!fuelSlot.isEmpty()) {
            int burnTime = fuelSlot.getBurnTime(RecipeType.SMELTING);
            if (burnTime <= 0) {
                if (fuelSlot.is(Items.COAL) || fuelSlot.is(Items.CHARCOAL)) burnTime = 1600;
                else if (fuelSlot.is(Items.COAL_BLOCK)) burnTime = 16000;
                else if (fuelSlot.is(Items.BLAZE_ROD)) burnTime = 2400;
                else if (fuelSlot.is(Items.STICK)) burnTime = 100;
            }
            if (burnTime > 0) {
                setFuelTicks(getFuelTicks() + burnTime);
                fuelSlot.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                return;
            }
        }

        // 2. Strict Emergency: Check internal buffer for 1 piece of Coal
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
                stack.shrink(1);
                setFuelTicks(getFuelTicks() + 1600);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                return;
            }
        }
    }

    // ─── Home & Station Docking ────────────────────────────────────────────────

    public BlockPos getHomePosition() {
        return this.entityData.get(HOME_POS).orElseGet(this::blockPosition);
    }

    public void setHomePosition(@Nullable BlockPos pos) {
        this.entityData.set(HOME_POS, Optional.ofNullable(pos));
    }

    public Optional<BlockPos> getLinkedChest() {
        return this.entityData.get(LINKED_CHEST);
    }

    public void setLinkedChest(@Nullable BlockPos pos) {
        this.entityData.set(LINKED_CHEST, Optional.ofNullable(pos));
    }

    // ─── Work Area Boundaries ──────────────────────────────────────────────────

    public boolean hasWorkArea() {
        return this.entityData.get(HAS_WORK_AREA);
    }

    public int getAreaMinX() {
        return this.entityData.get(AREA_MIN_X);
    }

    public int getAreaMinZ() {
        return this.entityData.get(AREA_MIN_Z);
    }

    public int getAreaMaxX() {
        return this.entityData.get(AREA_MAX_X);
    }

    public int getAreaMaxZ() {
        return this.entityData.get(AREA_MAX_Z);
    }

    public void setWorkArea(int minX, int minZ, int maxX, int maxZ) {
        this.entityData.set(AREA_MIN_X, minX);
        this.entityData.set(AREA_MIN_Z, minZ);
        this.entityData.set(AREA_MAX_X, maxX);
        this.entityData.set(AREA_MAX_Z, maxZ);
        this.entityData.set(HAS_WORK_AREA, true);
    }

    public int getEffectiveMinX() {
        if (hasWorkArea()) return getAreaMinX();
        int r = getMaxAllowedAreaDimension() / 2;
        return getHomePosition().getX() - r;
    }

    public int getEffectiveMaxX() {
        if (hasWorkArea()) return getAreaMaxX();
        int r = getMaxAllowedAreaDimension() / 2;
        return getHomePosition().getX() + r;
    }

    public int getEffectiveMinZ() {
        if (hasWorkArea()) return getAreaMinZ();
        int r = getMaxAllowedAreaDimension() / 2;
        return getHomePosition().getZ() - r;
    }

    public int getEffectiveMaxZ() {
        if (hasWorkArea()) return getAreaMaxZ();
        int r = getMaxAllowedAreaDimension() / 2;
        return getHomePosition().getZ() + r;
    }

    public int getRangeUpgradesCount() {
        ItemStack stack = this.upgradeInventory.getItem(0);
        if (stack.is(ModItems.RANGE_UPGRADE.get())) {
            return stack.getCount();
        }
        return 0;
    }

    // ─── Inventories ───────────────────────────────────────────────────────────

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public SimpleContainer getFuelInventory() {
        return this.fuelInventory;
    }

    public SimpleContainer getUpgradeInventory() {
        return this.upgradeInventory;
    }

    /**
     * Subclasses can override this to drop additional inventories (e.g., pickaxe).
     */
    protected void dropExtraInventories(Level level, BlockPos pos) {}

    // ─── Lifecycle & Tick ──────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (hasFuel() && this.random.nextFloat() < 0.12F) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getX(),
                        this.getY() + 1.1D,
                        this.getZ(),
                        0.0D, 0.04D, 0.0D
                );
            }
            droidTickClient();
        } else {
            if (getFuelTicks() <= 300) {
                tryEmergencyRefuel();
            }

            int currentFuel = getFuelTicks();
            if (currentFuel > 0) {
                setFuelTicks(currentFuel - 1);
            } else {
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
            }

            droidTickServer();
        }
    }

    protected void droidTickClient() {}

    protected void droidTickServer() {}

    // ─── Interaction & Dismantling ─────────────────────────────────────────────

    public InteractionResult dismantle(Player player) {
        Level level = this.level();
        if (!level.isClientSide) {
            BlockPos pos = this.blockPosition();

            // 1. Drop all common inventories
            Containers.dropContents(level, pos, this.inventory);
            Containers.dropContents(level, pos, this.fuelInventory);
            Containers.dropContents(level, pos, this.upgradeInventory);

            // 2. Drop extra subclass inventories
            dropExtraInventories(level, pos);

            // 3. Clear station occupied state if home was station
            BlockPos homePos = getHomePosition();
            BlockState homeState = level.getBlockState(homePos);
            if (homeState.is(ModBlocks.DROID_STATION.get())) {
                level.setBlock(homePos, homeState.setValue(DroidStationBlock.OCCUPIED, false), 3);
            }

            // 4. Drop Droid item form
            ItemStack droidItem = new ItemStack(getDropItem());
            ItemEntity itemEntity = new ItemEntity(level, this.getX(), this.getY() + 0.5D, this.getZ(), droidItem);
            level.addFreshEntity(itemEntity);

            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
            player.displayClientMessage(
                    Component.literal("§a[Droids] §7Dismantled " + getDroidTypeName().getString() + "."),
                    true
            );

            this.discard();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Wrench dismantles droid
        if (held.is(ModItems.DROID_WRENCH.get())) {
            return dismantle(player);
        }

        // Controller handles its own interaction
        if (held.is(ModItems.DROID_CONTROLLER.get())) {
            return InteractionResult.PASS;
        }

        // Clean action bar telemetry readout for empty hand or other items
        if (!this.level().isClientSide) {
            int fuelSec = getFuelTicks() / 20;
            String status = !hasFuel() ? "Unpowered" : getSpecificStatus();
            BlockPos home = getHomePosition();
            player.displayClientMessage(
                    Component.literal("§6[" + getDroidTypeName().getString() + "] §7Status: §f" + status +
                            " §7| Fuel: §a" + fuelSec + "s" +
                            " §7| Base: §b(" + home.getX() + ", " + home.getY() + ", " + home.getZ() + ")"),
                    true
            );
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // ─── Damage & Resistances ──────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Metallic chassis is completely immune to drowning
        if (source.is(DamageTypes.DROWN)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // ─── NBT Persistence ───────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        getLinkedChest().ifPresent(pos -> {
            compound.putInt("ChestX", pos.getX());
            compound.putInt("ChestY", pos.getY());
            compound.putInt("ChestZ", pos.getZ());
        });
        getHomePosition();
        this.entityData.get(HOME_POS).ifPresent(pos -> {
            compound.putInt("HomeX", pos.getX());
            compound.putInt("HomeY", pos.getY());
            compound.putInt("HomeZ", pos.getZ());
        });

        compound.putInt("FuelTicks", getFuelTicks());

        if (hasWorkArea()) {
            compound.putBoolean("HasWorkArea", true);
            compound.putInt("MinX", getAreaMinX());
            compound.putInt("MinZ", getAreaMinZ());
            compound.putInt("MaxX", getAreaMaxX());
            compound.putInt("MaxZ", getAreaMaxZ());
        }

        compound.put("Inventory", this.inventory.createTag(this.registryAccess()));
        compound.put("FuelInventory", this.fuelInventory.createTag(this.registryAccess()));
        compound.put("UpgradeInventory", this.upgradeInventory.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ChestX") && compound.contains("ChestY") && compound.contains("ChestZ")) {
            setLinkedChest(new BlockPos(compound.getInt("ChestX"), compound.getInt("ChestY"), compound.getInt("ChestZ")));
        }
        if (compound.contains("HomeX") && compound.contains("HomeY") && compound.contains("HomeZ")) {
            setHomePosition(new BlockPos(compound.getInt("HomeX"), compound.getInt("HomeY"), compound.getInt("HomeZ")));
        }

        setFuelTicks(compound.getInt("FuelTicks"));

        if (compound.getBoolean("HasWorkArea")) {
            setWorkArea(
                    compound.getInt("MinX"),
                    compound.getInt("MinZ"),
                    compound.getInt("MaxX"),
                    compound.getInt("MaxZ")
            );
        }

        if (compound.contains("Inventory")) {
            this.inventory.fromTag(compound.getList("Inventory", 10), this.registryAccess());
        }
        if (compound.contains("FuelInventory")) {
            this.fuelInventory.fromTag(compound.getList("FuelInventory", 10), this.registryAccess());
        }
        if (compound.contains("UpgradeInventory")) {
            this.upgradeInventory.fromTag(compound.getList("UpgradeInventory", 10), this.registryAccess());
        }
    }
}
