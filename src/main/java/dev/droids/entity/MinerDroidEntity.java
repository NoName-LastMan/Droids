package dev.droids.entity;

import dev.droids.entity.ai.MinerRoutineGoal;
import dev.droids.init.ModItems;
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
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Autonomous Heavy-Duty Clockwork Miner Droid.
 * Features:
 * - Tunnel (Horizontal) & Quarry (Layer-by-layer down, max 20 blocks depth) modes.
 * - Vertical Steam Thrusters for pit ascent at 2x fuel burn rate.
 * - Dynamic mining speed scaling with pickaxe tier and Efficiency enchantment.
 */
public class MinerDroidEntity extends AbstractDroidEntity {

    public static final int MODE_TUNNEL = 0;
    public static final int MODE_QUARRY = 1;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> INITIAL_DIRECTION =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> START_Y =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ASCENDING =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TASK_COMPLETED =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.BOOLEAN);

    // Miner-specific inventory: Tool Slot
    private final SimpleContainer pickaxeInventory = new SimpleContainer(1);

    private boolean recallRequested = false;

    public MinerDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseDroidAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ARMOR, 10.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MinerRoutineGoal(this, 1.0D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, MODE_TUNNEL);
        builder.define(INITIAL_DIRECTION, Direction.NORTH);
        builder.define(START_Y, 0);
        builder.define(IS_ASCENDING, false);
        builder.define(TASK_COMPLETED, false);
    }

    // ─── Polymorphic Abstract Method Implementations ───────────────────────────

    @Override
    public Item getDropItem() {
        return ModItems.MINER_DROID.get();
    }

    @Override
    public Component getDroidTypeName() {
        return Component.literal("Miner Droid");
    }

    @Override
    public String getSpecificStatus() {
        if (isTaskCompleted()) return "Completed";
        if (isAscending()) return "Vertical Thrusters";
        if (isWorking()) return "Mining";
        return "Standby";
    }

    @Override
    public int getMaxAllowedAreaDimension() {
        return getMaxAllowedQuarryDimension();
    }

    // ─── Mining Mode & Direction ───────────────────────────────────────────────

    public int getMode() {
        return this.entityData.get(MODE);
    }

    public void setMode(int mode) {
        int targetMode = mode == MODE_QUARRY ? MODE_QUARRY : MODE_TUNNEL;
        if (getMode() != targetMode) {
            setTaskCompleted(false);
        }
        this.entityData.set(MODE, targetMode);
    }

    public void toggleMode() {
        setMode(getMode() == MODE_TUNNEL ? MODE_QUARRY : MODE_TUNNEL);
    }

    public Direction getInitialDirection() {
        return this.entityData.get(INITIAL_DIRECTION);
    }

    public void setInitialDirection(Direction dir) {
        if (getInitialDirection() != dir) {
            setTaskCompleted(false);
        }
        this.entityData.set(INITIAL_DIRECTION, dir);
    }

    @Override
    public void setWorkArea(int minX, int minZ, int maxX, int maxZ) {
        super.setWorkArea(minX, minZ, maxX, maxZ);
        setTaskCompleted(false);
    }

    public int getStartY() {
        return this.entityData.get(START_Y);
    }

    public void setStartY(int y) {
        this.entityData.set(START_Y, y);
    }

    @Override
    public void setHomePosition(@Nullable BlockPos pos) {
        super.setHomePosition(pos);
        if (pos != null) {
            setStartY(pos.getY());
        }
    }

    // ─── Vertical Steam Thrusters & Recall ─────────────────────────────────────

    public boolean isAscending() {
        return this.entityData.get(IS_ASCENDING);
    }

    public void setAscending(boolean ascending) {
        this.entityData.set(IS_ASCENDING, ascending);
    }

    public boolean isTaskCompleted() {
        return this.entityData.get(TASK_COMPLETED);
    }

    public void setTaskCompleted(boolean completed) {
        this.entityData.set(TASK_COMPLETED, completed);
    }

    public void requestRecall() {
        this.recallRequested = true;
    }

    public boolean isRecallRequested() {
        return this.recallRequested;
    }

    public void clearRecall() {
        this.recallRequested = false;
    }

    public void restartWork() {
        setTaskCompleted(false);
        this.recallRequested = false;
    }

    // ─── Range Limits ──────────────────────────────────────────────────────────

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

    // ─── Pickaxe Tool Logic & Mining Speed ─────────────────────────────────────

    public SimpleContainer getPickaxeInventory() {
        return this.pickaxeInventory;
    }

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

    // ─── Subclass Hooks ────────────────────────────────────────────────────────

    @Override
    protected void dropExtraInventories(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this.pickaxeInventory);
    }

    @Override
    protected void droidTickClient() {
        // Vertical Steam Thruster visual burst & audio hiss
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
    }

    @Override
    protected void droidTickServer() {
        // Thrusters burn fuel at 2x rate!
        if (isAscending() && getFuelTicks() > 0) {
            setFuelTicks(getFuelTicks() - 1);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Immune to gravel/sand suffocating in wall
        if (source.is(DamageTypes.IN_WALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // ─── NBT Persistence ───────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Mode", getMode());
        compound.putInt("InitialDir", getInitialDirection().get3DDataValue());
        compound.putInt("StartY", getStartY());
        compound.put("PickaxeInventory", this.pickaxeInventory.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setMode(compound.getInt("Mode"));
        if (compound.contains("InitialDir")) {
            setInitialDirection(Direction.from3DDataValue(compound.getInt("InitialDir")));
        }
        setStartY(compound.getInt("StartY"));
        if (compound.contains("PickaxeInventory")) {
            this.pickaxeInventory.fromTag(compound.getList("PickaxeInventory", 10), this.registryAccess());
        }
    }
}
