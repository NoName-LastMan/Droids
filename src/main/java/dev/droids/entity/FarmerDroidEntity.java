package dev.droids.entity;

import dev.droids.entity.ai.FarmerBatchRoutineGoal;
import dev.droids.init.ModItems;
import dev.droids.menu.FarmerDroidMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Autonomous Clockwork Farmer Droid.
 * Operates on a scheduled batch routine: scans field on 10s intervals,
 * chain-harvests all mature crops without pauses, unloads to storage,
 * and docks at its home position in motionless standby.
 */
public class FarmerDroidEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<BlockPos>> LINKED_CHEST =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> FUEL_TICKS =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_WORKING =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> HAS_WORK_AREA =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AREA_MIN_X =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MIN_Z =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MAX_X =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MAX_Z =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);

    // Inventories
    private final SimpleContainer inventory = new SimpleContainer(9);
    private final SimpleContainer fuelInventory = new SimpleContainer(1);
    private final SimpleContainer upgradeInventory = new SimpleContainer(1);

    public FarmerDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // High-performance batch routine goal (replaces vanilla sluggish step-by-step goal)
        this.goalSelector.addGoal(1, new FarmerBatchRoutineGoal(this, 1.0D));
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

    // Home Dock position
    public BlockPos getHomePosition() {
        return this.entityData.get(HOME_POS).orElseGet(this::blockPosition);
    }

    public void setHomePosition(@Nullable BlockPos pos) {
        this.entityData.set(HOME_POS, Optional.ofNullable(pos));
    }

    // Work Area methods
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
        // Set home dock position at the center of the configured work area
        setHomePosition(new BlockPos((minX + maxX) / 2, this.getBlockY(), (minZ + maxZ) / 2));
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

    public int getMaxAllowedAreaDimension() {
        int upgrades = getRangeUpgradesCount();
        return switch (upgrades) {
            case 1 -> 9;   // 9x9
            case 2 -> 15;  // 15x15
            case 3 -> 21;  // 21x21
            case 4 -> 31;  // 31x31
            default -> 5;  // 5x5 base
        };
    }

    // Storage and Fuel
    public Optional<BlockPos> getLinkedChest() {
        return this.entityData.get(LINKED_CHEST);
    }

    public void setLinkedChest(@Nullable BlockPos pos) {
        this.entityData.set(LINKED_CHEST, Optional.ofNullable(pos));
    }

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

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public SimpleContainer getFuelInventory() {
        return this.fuelInventory;
    }

    public SimpleContainer getUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.DROID_WRENCH.get())) {
            return super.mobInteract(player, hand);
        }

        if (held.is(ModItems.DROID_CONTROLLER.get())) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide) {
            int fuelSec = getFuelTicks() / 20;
            String status = !hasFuel() ? "Unpowered" : (isWorking() ? "Harvesting" : "Standby");
            BlockPos home = getHomePosition();
            player.displayClientMessage(
                    Component.literal("§2[Farmer Droid] §7Status: §f" + status +
                            " §7| Fuel: §a" + fuelSec + "s" +
                            " §7| Base: §b(" + home.getX() + ", " + home.getY() + ", " + home.getZ() + ")"),
                    true
            );
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private ContainerData createMenuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getFuelTicks();
                    case 1 -> getRangeUpgradesCount();
                    case 2 -> hasWorkArea() ? 1 : 0;
                    case 3 -> getAreaMinX();
                    case 4 -> getAreaMinZ();
                    case 5 -> getAreaMaxX();
                    case 6 -> getAreaMaxZ();
                    case 7 -> getLinkedChest().isPresent() ? 1 : 0;
                    case 8 -> getLinkedChest().map(BlockPos::getX).orElse(0);
                    case 9 -> getLinkedChest().map(BlockPos::getY).orElse(0);
                    case 10 -> getLinkedChest().map(BlockPos::getZ).orElse(0);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) setFuelTicks(value);
            }

            @Override
            public int getCount() {
                return 11;
            }
        };
    }

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
        } else {
            if (getFuelTicks() <= 40) {
                tryConsumeFuelFromSlot();
            }

            int currentFuel = getFuelTicks();
            if (currentFuel > 0) {
                setFuelTicks(currentFuel - 1);
            } else {
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private void tryConsumeFuelFromSlot() {
        ItemStack fuelStack = this.fuelInventory.getItem(0);
        if (!fuelStack.isEmpty()) {
            int burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
            if (burnTime <= 0) {
                if (fuelStack.is(Items.COAL) || fuelStack.is(Items.CHARCOAL)) burnTime = 1600;
                else if (fuelStack.is(Items.COAL_BLOCK)) burnTime = 16000;
                else if (fuelStack.is(Items.BLAZE_ROD)) burnTime = 2400;
                else if (fuelStack.is(Items.STICK)) burnTime = 100;
            }

            if (burnTime > 0) {
                setFuelTicks(getFuelTicks() + burnTime);
                fuelStack.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
            }
        }
    }

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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }
}
