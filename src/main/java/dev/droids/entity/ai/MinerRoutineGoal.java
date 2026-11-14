package dev.droids.entity.ai;

import dev.droids.entity.MinerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Intelligent Mining AI for Miner Droid.
 * Features:
 * - TUNNEL: 3x3 horizontal bore forward.
 * - QUARRY: Layer-by-layer downward excavation strictly capped at 20 blocks depth.
 * - VERTICAL STEAM THRUSTERS: Boosts straight up to Droid Station when full, low fuel, or recalled.
 * - DROID STATION DOCKING: Unloads ore buffer, auto-refuels, restocks pickaxe.
 */
public class MinerRoutineGoal extends Goal {

    public enum State {
        IDLE,
        DIGGING,
        ASCENDING_TO_STATION,
        SERVICING_AT_STATION,
        DESCENDING_TO_WORK
    }

    private final MinerDroidEntity droid;
    private final double speedModifier;
    private State state = State.IDLE;

    private BlockPos currentTarget = null;
    private int miningProgress = 0;
    private int repathTimer = 0;
    private int serviceTimer = 0;

    // Quarry progress tracking
    private int currentQuarryY;
    private int quarryX;
    private int quarryZ;

    // Tunnel progress tracking
    private int tunnelStep = 0;
    private int tunnelSubIndex = 0; // 0..8 for 3x3 face

    public MinerRoutineGoal(MinerDroidEntity droid, double speedModifier) {
        this.droid = droid;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.droid.hasFuel();
    }

    @Override
    public boolean canContinueToUse() {
        return this.droid.hasFuel();
    }

    @Override
    public void start() {
        initQuarryCoords();
        this.state = State.DIGGING;
    }

    private void initQuarryCoords() {
        this.currentQuarryY = this.droid.getStartY();
        this.quarryX = this.droid.getAreaMinX();
        this.quarryZ = this.droid.getAreaMinZ();
    }

    @Override
    public void tick() {
        if (!this.droid.hasFuel()) {
            this.droid.getNavigation().stop();
            this.droid.setWorking(false);
            this.droid.setAscending(false);
            this.state = State.IDLE;
            return;
        }

        switch (this.state) {
            case IDLE -> tickIdle();
            case DIGGING -> tickDigging();
            case ASCENDING_TO_STATION -> tickAscending();
            case SERVICING_AT_STATION -> tickServicing();
            case DESCENDING_TO_WORK -> tickDescending();
        }
    }

    private void tickIdle() {
        this.droid.setWorking(false);
        this.droid.setAscending(false);

        // Check if ready to resume work
        if (this.droid.hasValidPickaxe() && this.droid.getFuelTicks() > 400 && !isInventoryFull()) {
            if (this.droid.getMode() == MinerDroidEntity.MODE_QUARRY && this.currentQuarryY < this.droid.getStartY() - 1) {
                this.state = State.DESCENDING_TO_WORK;
            } else {
                this.state = State.DIGGING;
            }
        }
    }

    private void tickDigging() {
        // Return conditions: Full Inventory, Low Fuel (<10s), Broken Tool, or Recall Command
        if (isInventoryFull() || this.droid.getFuelTicks() < 200 || !this.droid.hasValidPickaxe() || this.droid.isRecallRequested()) {
            this.droid.setWorking(false);
            this.state = State.ASCENDING_TO_STATION;
            return;
        }

        Level level = this.droid.level();

        // 1. Hazard Clearing: Falling gravel/sand overhead
        BlockPos overheadHazard = findFallingHazard();
        if (overheadHazard != null) {
            mineBlockAt(overheadHazard);
            return;
        }

        // 2. Patch floor lava or gaps with cobblestone
        patchHazardsAround();

        // 3. Find next mining block
        if (this.currentTarget == null || level.getBlockState(this.currentTarget).isAir()) {
            this.currentTarget = findNextMiningBlock();
            this.miningProgress = 0;
            if (this.currentTarget == null) {
                // Completed quarry or tunnel!
                this.state = State.ASCENDING_TO_STATION;
                return;
            }
        }

        // 4. Approach and mine block
        double distSq = this.droid.distanceToSqr(
                this.currentTarget.getX() + 0.5D,
                this.currentTarget.getY() + 0.5D,
                this.currentTarget.getZ() + 0.5D
        );

        this.droid.getLookControl().setLookAt(
                this.currentTarget.getX() + 0.5D,
                this.currentTarget.getY() + 0.5D,
                this.currentTarget.getZ() + 0.5D
        );

        if (distSq <= 16.0D) {
            // Within 4-block mining reach
            this.droid.getNavigation().stop();
            mineBlockAt(this.currentTarget);
        } else {
            // Move towards standable position above or next to target
            BlockPos standPos = this.currentTarget.above();
            if (++this.repathTimer >= 15 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                boolean pathFound = this.droid.getNavigation().moveTo(
                        standPos.getX() + 0.5D,
                        standPos.getY(),
                        standPos.getZ() + 0.5D,
                        this.speedModifier
                );

                // If path obstructed in deep quarry, use thruster nudge
                if (!pathFound && this.droid.getMode() == MinerDroidEntity.MODE_QUARRY) {
                    double dx = (standPos.getX() + 0.5D) - this.droid.getX();
                    double dz = (standPos.getZ() + 0.5D) - this.droid.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.1D) {
                        this.droid.setDeltaMovement((dx / dist) * 0.18D, 0.1D, (dz / dist) * 0.18D);
                    }
                }
            }
        }
    }

    private void tickAscending() {
        this.droid.setWorking(false);
        this.droid.setAscending(true);
        this.droid.getNavigation().stop();

        BlockPos stationPos = this.droid.getHomePosition();
        double targetY = stationPos.getY() + 0.88D;

        double dx = (stationPos.getX() + 0.5D) - this.droid.getX();
        double dz = (stationPos.getZ() + 0.5D) - this.droid.getZ();
        double hDistSq = dx * dx + dz * dz;

        // Phase 1: Vertical steam thruster climb up to station height
        if (this.droid.getY() < targetY - 0.2D) {
            double hDist = Math.sqrt(hDistSq);
            double vx = hDist > 0.5D ? (dx / hDist) * 0.08D : 0.0D;
            double vz = hDist > 0.5D ? (dz / hDist) * 0.08D : 0.0D;
            this.droid.setDeltaMovement(vx, 0.35D, vz);
            this.droid.fallDistance = 0.0F;
        } else {
            // Phase 2: Horizontal glide into station docking cradle
            if (hDistSq <= 0.6D) {
                // Docked successfully!
                this.droid.setDeltaMovement(Vec3.ZERO);
                this.droid.setPos(stationPos.getX() + 0.5D, targetY, stationPos.getZ() + 0.5D);
                this.droid.setAscending(false);
                this.serviceTimer = 0;
                this.state = State.SERVICING_AT_STATION;
            } else {
                double hDist = Math.sqrt(hDistSq);
                this.droid.setDeltaMovement((dx / hDist) * 0.22D, 0.02D, (dz / hDist) * 0.22D);
                this.droid.fallDistance = 0.0F;
            }
        }
    }

    private void tickServicing() {
        this.droid.setAscending(false);
        this.droid.setWorking(false);
        this.droid.getNavigation().stop();

        this.serviceTimer++;

        // Rest at station for 25 ticks (1.25s) while station unloads, refuels, and equips tool
        if (this.serviceTimer >= 25) {
            // If user requested recall, remain parked at station
            if (this.droid.isRecallRequested()) {
                this.droid.clearRecall();
                this.state = State.IDLE;
                return;
            }

            // Check if quarry completed (reached strict 20-block depth limit)
            int maxDepthY = this.droid.getStartY() - 20;
            if (this.droid.getMode() == MinerDroidEntity.MODE_QUARRY && this.currentQuarryY < maxDepthY) {
                this.state = State.IDLE;
                return;
            }

            // Check if serviced and ready to work
            if (this.droid.hasFuel() && this.droid.hasValidPickaxe() && !isInventoryFull()) {
                if (this.droid.getMode() == MinerDroidEntity.MODE_QUARRY && this.currentQuarryY < this.droid.getStartY() - 1) {
                    this.state = State.DESCENDING_TO_WORK;
                } else {
                    this.state = State.DIGGING;
                }
            } else {
                // Keep waiting at dock for fuel or tools
                this.serviceTimer = 10;
            }
        }
    }

    private void tickDescending() {
        this.droid.setWorking(false);
        this.droid.getNavigation().stop();

        int targetY = this.currentQuarryY + 1;
        BlockPos quarryCenter = new BlockPos(
                (this.droid.getAreaMinX() + this.droid.getAreaMaxX()) / 2,
                targetY,
                (this.droid.getAreaMinZ() + this.droid.getAreaMaxZ()) / 2
        );

        double dx = (quarryCenter.getX() + 0.5D) - this.droid.getX();
        double dz = (quarryCenter.getZ() + 0.5D) - this.droid.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);

        // Controlled descent with thrusters to prevent fall damage
        if (this.droid.getY() > targetY + 0.5D) {
            double vx = hDist > 0.5D ? (dx / hDist) * 0.15D : 0.0D;
            double vz = hDist > 0.5D ? (dz / hDist) * 0.15D : 0.0D;
            this.droid.setDeltaMovement(vx, -0.22D, vz);
            this.droid.fallDistance = 0.0F;
        } else {
            // Landed at work layer
            this.droid.setDeltaMovement(Vec3.ZERO);
            this.droid.fallDistance = 0.0F;
            this.state = State.DIGGING;
        }
    }

    private BlockPos findFallingHazard() {
        Level level = this.droid.level();
        BlockPos pos = this.droid.blockPosition();

        for (Direction dir : new Direction[]{Direction.UP, this.droid.getInitialDirection()}) {
            BlockPos check = pos.relative(dir);
            BlockState state = level.getBlockState(check);
            if (state.getBlock() instanceof FallingBlock) {
                return check;
            }
        }
        return null;
    }

    private void patchHazardsAround() {
        Level level = this.droid.level();
        BlockPos pos = this.droid.blockPosition();

        BlockPos floor = pos.below();
        if (level.getBlockState(floor).isAir() || level.getBlockState(floor).is(Blocks.LAVA)) {
            placePatchBlock(floor);
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = pos.relative(dir);
            if (level.getBlockState(adj).is(Blocks.LAVA)) {
                placePatchBlock(adj);
            }
        }
    }

    private void placePatchBlock(BlockPos pos) {
        Container inv = this.droid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.DIRT)) {
                stack.shrink(1);
                this.droid.level().setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                this.droid.level().playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                break;
            }
        }
    }

    private @org.jetbrains.annotations.Nullable BlockPos findNextMiningBlock() {
        if (this.droid.getMode() == MinerDroidEntity.MODE_TUNNEL) {
            return findNextTunnelBlock();
        } else {
            return findNextQuarryBlock();
        }
    }

    // Tunnel boring: 3x3 bore face moving forward
    private @org.jetbrains.annotations.Nullable BlockPos findNextTunnelBlock() {
        Level level = this.droid.level();
        Direction forward = this.droid.getInitialDirection();
        Direction right = forward.getClockWise();
        BlockPos home = this.droid.getHomePosition();
        int maxDist = this.droid.getMaxAllowedTunnelDistance();

        while (this.tunnelStep < maxDist) {
            BlockPos centerOfSlice = home.relative(forward, this.tunnelStep);

            int rOffset = (this.tunnelSubIndex % 3) - 1; // -1, 0, 1
            int yOffset = (this.tunnelSubIndex / 3);      // 0, 1, 2

            BlockPos target = centerOfSlice.relative(right, rOffset).above(yOffset);
            this.tunnelSubIndex++;

            if (this.tunnelSubIndex >= 9) {
                this.tunnelSubIndex = 0;
                this.tunnelStep++;
            }

            BlockState state = level.getBlockState(target);
            if (!state.isAir() && state.getDestroySpeed(level, target) >= 0) {
                return target;
            }
        }
        return null;
    }

    // Quarry excavation: layer by layer downward, STRICT 20-BLOCK ABSOLUTE DEPTH LIMIT!
    private @org.jetbrains.annotations.Nullable BlockPos findNextQuarryBlock() {
        Level level = this.droid.level();
        int minX = this.droid.getAreaMinX();
        int maxX = this.droid.getAreaMaxX();
        int minZ = this.droid.getAreaMinZ();
        int maxZ = this.droid.getAreaMaxZ();
        int startY = this.droid.getStartY();
        int maxDepthY = startY - 20; // STRICT 20-BLOCK LIMIT!

        // If area not configured, fallback to 5x5 centered on station
        if (!this.droid.hasWorkArea()) {
            int r = this.droid.getMaxAllowedQuarryDimension() / 2;
            minX = this.droid.getHomePosition().getX() - r;
            maxX = this.droid.getHomePosition().getX() + r;
            minZ = this.droid.getHomePosition().getZ() - r;
            maxZ = this.droid.getHomePosition().getZ() + r;
        }

        while (this.currentQuarryY >= maxDepthY) {
            while (this.quarryX <= maxX) {
                while (this.quarryZ <= maxZ) {
                    BlockPos target = new BlockPos(this.quarryX, this.currentQuarryY, this.quarryZ);
                    this.quarryZ++;

                    BlockState state = level.getBlockState(target);
                    if (!state.isAir() && state.getDestroySpeed(level, target) >= 0) {
                        return target;
                    }
                }
                this.quarryZ = minZ;
                this.quarryX++;
            }
            this.quarryX = minX;
            this.currentQuarryY--; // descend to next depth layer
        }
        return null; // All 20 layers completed!
    }

    private void mineBlockAt(BlockPos target) {
        if (!(this.droid.level() instanceof ServerLevel serverLevel)) return;

        BlockState state = serverLevel.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(serverLevel, target) < 0) {
            this.currentTarget = null;
            this.droid.setWorking(false);
            return;
        }

        this.droid.setWorking(true);

        float hardness = Math.max(0.5F, state.getDestroySpeed(serverLevel, target));
        float toolSpeed = this.droid.getMiningSpeedModifier(state);
        int requiredTicks = Math.max(2, (int) ((hardness * 20.0F) / toolSpeed));

        this.miningProgress++;

        if (this.miningProgress >= requiredTicks) {
            List<ItemStack> drops = Block.getDrops(state, serverLevel, target, null, this.droid, this.droid.getPickaxe());
            serverLevel.destroyBlock(target, false, this.droid);
            this.droid.damagePickaxe(1);

            for (ItemStack drop : drops) {
                ItemStack remaining = this.droid.getInventory().addItem(drop);
                if (!remaining.isEmpty()) {
                    Block.popResource(serverLevel, target, remaining);
                }
            }

            this.miningProgress = 0;
            this.currentTarget = null;
            this.droid.setWorking(false);
        }
    }

    private boolean isInventoryFull() {
        Container inv = this.droid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}
