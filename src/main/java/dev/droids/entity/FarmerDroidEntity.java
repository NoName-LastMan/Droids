package dev.droids.entity;

import dev.droids.entity.ai.FarmerDepositGoal;
import dev.droids.entity.ai.FarmerHarvestGoal;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Autonomous Clockwork Farmer Droid.
 * Detects mature crops, harvests them, replants seeds, and deposits yields to a linked chest.
 */
public class FarmerDroidEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<BlockPos>> LINKED_CHEST =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> FUEL_TICKS =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_WORKING =
            SynchedEntityData.defineId(FarmerDroidEntity.class, EntityDataSerializers.BOOLEAN);

    // Internal inventory (seeds and harvested crops)
    private final SimpleContainer inventory = new SimpleContainer(9);

    public FarmerDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FarmerDepositGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new FarmerHarvestGoal(this, 0.9D, 12));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LINKED_CHEST, Optional.empty());
        builder.define(FUEL_TICKS, 0);
        builder.define(IS_WORKING, false);
    }

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

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Refueling with solid fuels
        int addedFuel = getFuelValue(held);
        if (addedFuel > 0) {
            if (!this.level().isClientSide) {
                setFuelTicks(getFuelTicks() + addedFuel);
                held.consume(1, player);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                player.displayClientMessage(
                        Component.translatable("message.droids.refueled", getFuelTicks() / 20),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Status check when sneaking with empty hand
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!this.level().isClientSide) {
                String chestInfo = getLinkedChest()
                        .map(pos -> "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                        .orElse("None");
                player.displayClientMessage(
                        Component.translatable("message.droids.status", getFuelTicks() / 20, chestInfo),
                        false
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private int getFuelValue(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 1600; // 80 seconds
        if (stack.is(Items.BLAZE_ROD)) return 2400; // 120 seconds
        if (stack.is(Items.STICK)) return 100; // 5 seconds
        if (stack.is(Items.COAL_BLOCK)) return 16000; // 800 seconds
        return 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        getLinkedChest().ifPresent(pos -> {
            compound.putInt("ChestX", pos.getX());
            compound.putInt("ChestY", pos.getY());
            compound.putInt("ChestZ", pos.getZ());
        });
        compound.putInt("FuelTicks", getFuelTicks());
        compound.put("Inventory", this.inventory.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ChestX") && compound.contains("ChestY") && compound.contains("ChestZ")) {
            setLinkedChest(new BlockPos(compound.getInt("ChestX"), compound.getInt("ChestY"), compound.getInt("ChestZ")));
        }
        setFuelTicks(compound.getInt("FuelTicks"));
        if (compound.contains("Inventory")) {
            this.inventory.fromTag(compound.getList("Inventory", 10), this.registryAccess());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Emit steam puffs from chimney if working and fueled
            if (hasFuel() && this.random.nextFloat() < 0.15F) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getX(),
                        this.getY() + 1.1D,
                        this.getZ(),
                        0.0D, 0.05D, 0.0D
                );
            }
        } else {
            // Consume fuel
            int currentFuel = getFuelTicks();
            if (currentFuel > 0) {
                setFuelTicks(currentFuel - 1);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }
}
