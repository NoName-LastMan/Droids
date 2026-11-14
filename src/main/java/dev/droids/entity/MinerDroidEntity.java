package dev.droids.entity;

import dev.droids.entity.ai.MinerRoutineGoal;
import dev.droids.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Clockwork Miner Droid: The Generator Breaker.
 *
 * Robust, stationary mechanical worker that operates at the Droid Station,
 * breaking cobblestone, stone, basalt, or ore blocks in front with pickaxe tools,
 * and extracting bonus minerals with Fortune support.
 */
public class MinerDroidEntity extends AbstractDroidEntity {

    public static final int MODE_TUNNEL = 0;
    public static final int MODE_QUARRY = 1;

    private static final EntityDataAccessor<Direction> INITIAL_DIRECTION =
            SynchedEntityData.defineId(MinerDroidEntity.class, EntityDataSerializers.DIRECTION);

    // Miner-specific inventory: Tool Slot
    private final SimpleContainer pickaxeInventory = new SimpleContainer(1);

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
        builder.define(INITIAL_DIRECTION, Direction.NORTH);
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
        if (!hasFuel()) return "Unpowered";
        if (!hasValidPickaxe()) return "No Pickaxe";
        if (isWorking()) return "Breaking";
        return "Idle (Waiting Block)";
    }

    @Override
    public int getMaxAllowedAreaDimension() {
        return 1 + (getRangeUpgradesCount() * 2);
    }

    // ─── Direction ─────────────────────────────────────────────────────────────

    public Direction getInitialDirection() {
        return this.entityData.get(INITIAL_DIRECTION);
    }

    public void setInitialDirection(Direction dir) {
        this.entityData.set(INITIAL_DIRECTION, dir);
    }

    // Compatibility stubs for menus/telemetry
    public int getMode() { return 0; }
    public void setMode(int mode) {}
    public void toggleMode() {}
    public boolean isAscending() { return false; }
    public boolean isTaskCompleted() { return false; }
    public void setTaskCompleted(boolean completed) {}
    public void restartWork() {}
    public void requestRecall() {}
    public boolean isRecallRequested() { return false; }
    public void clearRecall() {}
    public int getStartY() { return getHomePosition().getY(); }

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
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // ─── NBT Persistence ───────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("InitialDir", getInitialDirection().get3DDataValue());
        compound.put("PickaxeInventory", this.pickaxeInventory.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("InitialDir")) {
            setInitialDirection(Direction.from3DDataValue(compound.getInt("InitialDir")));
        }
        if (compound.contains("PickaxeInventory")) {
            this.pickaxeInventory.fromTag(compound.getList("PickaxeInventory", 10), this.registryAccess());
        }
    }
}
