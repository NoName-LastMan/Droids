package dev.droids.entity.ai;

import dev.droids.block.DroidStationBlock;
import dev.droids.entity.MinerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Clockwork Generator Breaker AI for Miner Droid.
 *
 * Design Philosophy:
 * - Simple, robust, and zero-pathfinding overhead.
 * - Anchored at the Droid Station, facing the player's generator setup.
 * - Automatically breaks solid cobblestone, stone, basalt, or ore blocks in front.
 * - Extracts bonus minerals with Fortune enchantment support.
 * - Feeds drops directly into the Droid Station output buffer.
 */
public class MinerRoutineGoal extends AbstractDroidGoal<MinerDroidEntity> {

    private BlockPos currentTarget = null;
    private int miningProgress = 0;
    private int swingCooldown = 0;

    public MinerRoutineGoal(MinerDroidEntity droid, double speedModifier) {
        super(droid, speedModifier);
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        // 1. Unpowered check
        if (!this.droid.hasFuel()) {
            this.droid.setWorking(false);
            this.currentTarget = null;
            this.miningProgress = 0;
            return;
        }

        // 2. Keep Miner docked neatly on Station cradle
        dockAtStation();

        // 3. Tool & inventory check
        if (!this.droid.hasValidPickaxe() || isInventoryFull()) {
            this.droid.setWorking(false);
            this.currentTarget = null;
            this.miningProgress = 0;
            return;
        }

        Level level = this.droid.level();

        // 4. Validate or acquire next solid minable block
        if (this.currentTarget == null || !isValidTarget(level, this.currentTarget)) {
            this.currentTarget = scanNextBlockInFront();
            this.miningProgress = 0;
        }

        // 5. If no block to mine, rest quietly on station
        if (this.currentTarget == null) {
            this.droid.setWorking(false);
            return;
        }

        // 6. Actively mine target block
        mineCurrentTarget();
    }

    /**
     * Anchors the Droid onto the station pad and aligns rotation with station facing.
     */
    private void dockAtStation() {
        BlockPos stationPos = this.droid.getHomePosition();
        double targetY = stationPos.getY() + 0.88D;

        // Keep stationary
        this.droid.setDeltaMovement(Vec3.ZERO);
        this.droid.setPos(stationPos.getX() + 0.5D, targetY, stationPos.getZ() + 0.5D);

        // Align rotation to Station facing
        Level level = this.droid.level();
        BlockState stationState = level.getBlockState(stationPos);
        if (stationState.hasProperty(DroidStationBlock.FACING)) {
            Direction facing = stationState.getValue(DroidStationBlock.FACING);
            this.droid.setInitialDirection(facing);
            float yRot = facing.toYRot();
            this.droid.setYRot(yRot);
            this.droid.setYHeadRot(yRot);
            this.droid.setYBodyRot(yRot);
        }
    }

    /**
     * Scans front arc based on range upgrades:
     * - Tier 0: 1 block directly ahead
     * - Tier 1: 3 blocks wide (center, left, right)
     * - Tier 2+: 3x2 face (center, left, right and upper layer)
     */
    private @Nullable BlockPos scanNextBlockInFront() {
        Level level = this.droid.level();
        BlockPos home = this.droid.getHomePosition();
        Direction forward = this.droid.getInitialDirection();
        Direction right = forward.getClockWise();
        Direction left = forward.getCounterClockWise();

        List<BlockPos> candidates = new ArrayList<>();
        BlockPos center = home.relative(forward, 1);

        int upgrades = this.droid.getRangeUpgradesCount();

        // 1. Center block
        candidates.add(center);

        // 2. Horizontal sides (with 1+ upgrades)
        if (upgrades >= 1) {
            candidates.add(center.relative(left, 1));
            candidates.add(center.relative(right, 1));
        }

        // 3. Vertical upper row (with 2+ upgrades)
        if (upgrades >= 2) {
            candidates.add(center.above());
            candidates.add(center.relative(left, 1).above());
            candidates.add(center.relative(right, 1).above());
        }

        // Pick the first valid minable block
        for (BlockPos pos : candidates) {
            if (isValidTarget(level, pos)) {
                return pos;
            }
        }

        return null;
    }

    /**
     * Checks if a block is solid, minable, and NOT a liquid.
     */
    private boolean isValidTarget(Level level, BlockPos pos) {
        // Never mine the station or the block supporting it
        BlockPos home = this.droid.getHomePosition();
        if (pos.equals(home) || pos.equals(home.below())) return false;

        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.liquid() && state.getDestroySpeed(level, pos) >= 0;
    }

    /**
     * Executes the mechanical mining cycle.
     */
    private void mineCurrentTarget() {
        if (!(this.droid.level() instanceof ServerLevel serverLevel)) return;

        BlockState state = serverLevel.getBlockState(this.currentTarget);
        if (!isValidTarget(serverLevel, this.currentTarget)) {
            this.currentTarget = null;
            this.droid.setWorking(false);
            return;
        }

        this.droid.setWorking(true);
        this.droid.getLookControl().setLookAt(
                this.currentTarget.getX() + 0.5D,
                this.currentTarget.getY() + 0.5D,
                this.currentTarget.getZ() + 0.5D
        );

        // Swing arm animation
        if (++this.swingCooldown >= 4) {
            this.swingCooldown = 0;
            this.droid.swing(InteractionHand.MAIN_HAND);
        }

        // Mining duration based on tool speed and block hardness
        float hardness = Math.max(0.5F, state.getDestroySpeed(serverLevel, this.currentTarget));
        float toolSpeed = this.droid.getMiningSpeedModifier(state);
        int requiredTicks = Math.max(6, (int) ((hardness * 16.0F) / toolSpeed));

        this.miningProgress++;

        if (this.miningProgress >= requiredTicks) {
            ItemStack pickaxe = this.droid.getPickaxe();

            // Calculate drops (with Fortune / Silk Touch support)
            List<ItemStack> drops = Block.getDrops(state, serverLevel, this.currentTarget, null, this.droid, pickaxe);

            // Break the block
            serverLevel.destroyBlock(this.currentTarget, false, this.droid);
            this.droid.damagePickaxe(1);

            // Insert normal drops into inventory (which unloads to Station)
            for (ItemStack drop : drops) {
                ItemStack remaining = this.droid.getInventory().addItem(drop);
                if (!remaining.isEmpty()) {
                    Block.popResource(serverLevel, this.currentTarget, remaining);
                }
            }

            // Mineral Extraction Bonus for natural generator stone
            extractBonusMineral(serverLevel, state);

            // Burn small active fuel per block
            int fuel = this.droid.getFuelTicks();
            if (fuel > 0) {
                this.droid.setFuelTicks(Math.max(0, fuel - 10));
            }

            this.miningProgress = 0;
            this.currentTarget = null;
            this.droid.setWorking(false);
        }
    }

    /**
     * Extracts mineral nuggets from broken generator stone.
     * Fortune enchantment increases extraction probability.
     */
    private void extractBonusMineral(ServerLevel level, BlockState brokenState) {
        if (brokenState.is(Blocks.STONE) || brokenState.is(Blocks.COBBLESTONE)
                || brokenState.is(Blocks.DEEPSLATE) || brokenState.is(Blocks.COBBLED_DEEPSLATE)
                || brokenState.is(Blocks.BASALT)) {

            ItemStack pick = this.droid.getPickaxe();
            int fortune = level.registryAccess().lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.FORTUNE))
                    .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, pick))
                    .orElse(0);

            // Base chance: 7% + 2.5% per Fortune level
            float chance = 0.07F + (fortune * 0.025F);
            if (this.droid.getRandom().nextFloat() < chance) {
                float roll = this.droid.getRandom().nextFloat();
                ItemStack bonus;
                if (roll < 0.40F) bonus = new ItemStack(Items.RAW_IRON);
                else if (roll < 0.70F) bonus = new ItemStack(Items.RAW_COPPER);
                else if (roll < 0.85F) bonus = new ItemStack(Items.COAL);
                else if (roll < 0.95F) bonus = new ItemStack(Items.GOLD_NUGGET);
                else bonus = new ItemStack(Items.REDSTONE);

                ItemStack remaining = this.droid.getInventory().addItem(bonus);
                if (!remaining.isEmpty()) {
                    Block.popResource(level, this.currentTarget, remaining);
                }
                level.playSound(null, this.currentTarget, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 1.4F);
            }
        }
    }
}
