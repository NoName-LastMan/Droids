package dev.droids.entity;

import dev.droids.entity.ai.MinerRoutineGoal;
import dev.droids.init.ModItems;
import dev.droids.menu.MinerDroidMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Autonomous Heavy-Duty Clockwork Miner Droid.
 * Features dual operational modes:
 * 1. TUNNEL (Horizontal): Bores a 3x3 tunnel forward, bridging chasms and clearing falling gravel.
 * 2. QUARRY (Vertical): Excavates an area layer by layer downward, capped at a strict 20-block absolute depth limit.
 */
public class MinerDroidEntity extends PathfinderMob {

    public static final int MODE_TUNNEL = 0;
    public static final int MODE_QUARRY = 1;

    private static final EntityDataAccessor<Optional<BlockPos>> LINKED_CHEST =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> FUEL_TICKS =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_WORKING =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> INITIAL_DIRECTION =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> START_Y =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ASCENDING =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> HAS_WORK_AREA =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AREA_MIN_X =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MIN_Z =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MAX_X =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AREA_MAX_Z =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);

    // Inventories
    private final SimpleContainer inventory = new SimpleContainer(9);
    private final SimpleContainer fuelInventory = new SimpleContainer(1);
    private final SimpleContainer pickaxeInventory = new SimpleContainer(1);
    private final SimpleContainer upgradeInventory = new SimpleContainer(1);

    public MinerDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MinerRoutineGoal(this, 1.0D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LINKED_CHEST, Optional.empty());
        builder.define(HOME_POS, Optional.empty());
        builder.define(FUEL_TICKS, 0);
        builder.define(IS_WORKING, false);
        builder.define(MODE, MODE_TUNNEL);
        builder.define(INITIAL_DIRECTION, Direction.NORTH);
        builder.define(START_Y, 0);
        builder.define(IS_ASCENDING, false);
        builder.define(HAS_WORK_AREA, false);
        builder.define(AREA_MIN_X, 0);
        builder.define(AREA_MIN_Z, 0);
        builder.define(AREA_MAX_X, 0);
        builder.define(AREA_MAX_Z, 0);
    }

    // Mode Toggle
    public int getMode() {
        return this.entityData.get(MODE);
    }

    public void setMode(int mode) {
        this.entityData.set(MODE, mode == MODE_QUARRY ? MODE_QUARRY : MODE_TUNNEL);
    }

    public void toggleMode() {
        setMode(getMode() == MODE_TUNNEL ? MODE_QUARRY : MODE_TUNNEL);
    }

    public Direction getInitialDirection() {
        return this.entityData.get(INITIAL_DIRECTION);
    }

    public void setInitialDirection(Direction dir) {
        this.entityData.set(INITIAL_DIRECTION, dir);
    }

    public int getStartY() {
        return this.entityData.get(START_Y);
    }

    public void setStartY(int y) {
        this.entityData.set(START_Y, y);
    }

    public boolean isAscending() {
        return this.entityData.get(IS_ASCENDING);
    }

    public void setAscending(boolean ascending) {
        this.entityData.set(IS_ASCENDING, ascending);
    }

    private boolean recallRequested = false;

    public void requestRecall() {
        this.recallRequested = true;
    }

    public boolean isRecallRequested() {
        return this.recallRequested;
    }

    public void clearRecall() {
        this.recallRequested = false;
    }

    // Home & Area
    public BlockPos getHomePosition() {
        return this.entityData.get(HOME_POS).orElseGet(this::blockPosition);
    }

    public void setHomePosition(@Nullable BlockPos pos) {
        this.entityData.set(HOME_POS, Optional.ofNullable(pos));
        if (pos != null) {
            setStartY(pos.getY());
        }
    }

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

    public int getRangeUpgradesCount() {
        ItemStack stack = this.upgradeInventory.getItem(0);
        if (stack.is(ModItems.RANGE_UPGRADE.get())) {
            return stack.getCount();
        }
        return 0;
    }

    public int getMaxAllowedTunnelDistance() {
        return switch (getRangeUpgradesCount()) {
            case 1 -> 64;   // 64 blocks
            case 2 -> 128;  // 128 blocks
            case 3 -> 256;  // 256 blocks
            case 4 -> 512;  // 512 blocks
            default -> 32;  // 32 blocks base
        };
    }

    public int getMaxAllowedQuarryDimension() {
        return switch (getRangeUpgradesCount()) {
            case 1 -> 9;    // 9x9
            case 2 -> 16;   // 16x16 (Slime Chunk!)
            case 3 -> 21;   // 21x21
            case 4 -> 31;   // 31x31
            default -> 5;   // 5x5 base
        };
    }

    // Pickaxe Tool logic & Speed calculation
    public ItemStack getPickaxe() {
        return this.pickaxeInventory.getItem(0);
    }

    public boolean hasValidPickaxe() {
        ItemStack pick = getPickaxe();
        return !pick.isEmpty() && (pick.is(ItemTags.PICKAXES) || pick.getItem() instanceof DiggerItem);
    }

    public float getMiningSpeedModifier(BlockState state) {
        ItemStack pick = getPickaxe();
        if (pick.isEmpty()) return 1.0F;

        float speed = pick.getDestroySpeed(state);
        // Efficiency enchantment bonus
        int efficiency = this.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(Enchantments.EFFICIENCY))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, pick))
                .orElse(0);

        if (efficiency > 0) {
            speed += (efficiency * efficiency + 1);
        }
        return Math.max(1.0F, speed);
    }

    public void damagePickaxe(int amount) {
        ItemStack pick = getPickaxe();
        if (!pick.isEmpty() && pick.isDamageableItem()) {
            pick.setDamageValue(pick.getDamageValue() + amount);
            if (pick.getDamageValue() >= pick.getMaxDamage()) {
                this.pickaxeInventory.setItem(0, ItemStack.EMPTY);
                this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 0.9F);
            }
        }
    }

    // Storage, Fuel, and State
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

    public SimpleContainer getPickaxeInventory() {
        return this.pickaxeInventory;
    }

    public SimpleContainer getUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Heavy metallic chassis: immune to suffocating under gravel/sand and drowning
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) {
            return false;
        }
        return super.hurt(source, amount);
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

        // Clean status notification without cluttered entity GUI
        if (!this.level().isClientSide) {
            int fuelSec = getFuelTicks() / 20;
            String modeStr = getMode() == MODE_TUNNEL ? "Tunnel" : "Quarry";
            String status = !hasFuel() ? "Unpowered" : (isAscending() ? "Vertical Thrusters" : (isWorking() ? "Mining" : "Standby"));
            BlockPos home = getHomePosition();
            player.displayClientMessage(
                    Component.literal("§6[Miner Droid] §7Status: §f" + status +
                            " §7| Fuel: §a" + fuelSec + "s" +
                            " §7| Mode: §e" + modeStr +
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
                    case 1 -> getMode();
                    case 2 -> getPickaxe().isDamageableItem() ? getPickaxe().getMaxDamage() - getPickaxe().getDamageValue() : (hasValidPickaxe() ? 100 : 0);
                    case 3 -> getPickaxe().isDamageableItem() ? getPickaxe().getMaxDamage() : (hasValidPickaxe() ? 100 : 0);
                    case 4 -> getRangeUpgradesCount();
                    case 5 -> getLinkedChest().isPresent() ? 1 : 0;
                    case 6 -> getLinkedChest().map(BlockPos::getX).orElse(0);
                    case 7 -> getLinkedChest().map(BlockPos::getY).orElse(0);
                    case 8 -> getLinkedChest().map(BlockPos::getZ).orElse(0);
                    case 9 -> hasWorkArea() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) setFuelTicks(value);
                else if (index == 1) setMode(value);
            }

            @Override
            public int getCount() {
                return 10;
            }
        };
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Emits heavy steam and glowing headlamp light effect
            if (hasFuel() && this.random.nextFloat() < 0.15F) {
                this.level().addParticle(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX(),
                        this.getY() + 1.2D,
                        this.getZ(),
                        0.0D, 0.03D, 0.0D
                );
            }

            // Heavy vertical steam thruster burst when ascending
            if (isAscending()) {
                for (int i = 0; i < 3; i++) {
                    double ox = (this.random.nextDouble() - 0.5D) * 0.4D;
                    double oz = (this.random.nextDouble() - 0.5D) * 0.4D;
                    this.level().addParticle(
                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getX() + ox,
                            this.getY() + 0.1D,
                            this.getZ() + oz,
                            0.0D, -0.2D, 0.0D
                    );
                    this.level().addParticle(
                            ParticleTypes.FLAME,
                            this.getX() + ox,
                            this.getY() + 0.1D,
                            this.getZ() + oz,
                            0.0D, -0.15D, 0.0D
                    );
                }
                if (this.tickCount % 6 == 0) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                            SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 0.6F, 1.2F, false);
                }
            }
        } else {
            // Thrusters consume 2x fuel!
            if (isAscending() && getFuelTicks() > 0) {
                setFuelTicks(getFuelTicks() - 1);
            }

            // Emergency auto-refuel: only 1 coal if tank is in critical low (<15s)
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
        }
    }

    private void tryEmergencyRefuel() {
        // 1. Try from dedicated Fuel Slot
        ItemStack fuelSlot = this.fuelInventory.getItem(0);
        if (!fuelSlot.isEmpty()) {
            int burnTime = fuelSlot.getBurnTime(RecipeType.SMELTING);
            if (burnTime <= 0 && (fuelSlot.is(Items.COAL) || fuelSlot.is(Items.CHARCOAL))) burnTime = 1600;
            if (burnTime > 0) {
                setFuelTicks(getFuelTicks() + burnTime);
                fuelSlot.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                return;
            }
        }

        // 2. Strict Emergency: Check mined inventory for 1 Coal
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
                stack.shrink(1);
                setFuelTicks(getFuelTicks() + 1600); // exactly 1 coal consumed for survival
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                return;
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
        compound.putInt("Mode", getMode());
        compound.putInt("InitialDir", getInitialDirection().get3DDataValue());
        compound.putInt("StartY", getStartY());

        if (hasWorkArea()) {
            compound.putBoolean("HasWorkArea", true);
            compound.putInt("MinX", getAreaMinX());
            compound.putInt("MinZ", getAreaMinZ());
            compound.putInt("MaxX", getAreaMaxX());
            compound.putInt("MaxZ", getAreaMaxZ());
        }

        compound.put("Inventory", this.inventory.createTag(this.registryAccess()));
        compound.put("FuelInventory", this.fuelInventory.createTag(this.registryAccess()));
        compound.put("PickaxeInventory", this.pickaxeInventory.createTag(this.registryAccess()));
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
        setMode(compound.getInt("Mode"));
        if (compound.contains("InitialDir")) {
            setInitialDirection(Direction.from3DDataValue(compound.getInt("InitialDir")));
        }
        setStartY(compound.getInt("StartY"));

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
        if (compound.contains("PickaxeInventory")) {
            this.pickaxeInventory.fromTag(compound.getList("PickaxeInventory", 10), this.registryAccess());
        }
        if (compound.contains("UpgradeInventory")) {
            this.upgradeInventory.fromTag(compound.getList("UpgradeInventory", 10), this.registryAccess());
        }
    }
}
