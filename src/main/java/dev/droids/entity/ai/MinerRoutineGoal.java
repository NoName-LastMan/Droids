package dev.droids.entity.ai;

import dev.droids.entity.MinerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Intelligent Mining AI for Miner Droid.
 * Supports TUNNEL (horizontal bore) and QUARRY (strict 20-block depth limit) modes,
 * clears overhead falling gravel, patches lava/chasms with stone, and scales mining speed with pickaxe.
 */
public class MinerRoutineGoal extends Goal {

    public enum State {
        IDLE,
        DIGGING,
        DEPOSITING,
        RETURNING
    }

    private final MinerDroidEntity droid;
    private final double speedModifier;
    private State state = State.IDLE;

    private BlockPos currentTarget = null;
    private int miningProgress = 0;
    private int repathTimer = 0;

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
        return this.droid.hasFuel() && this.droid.hasValidPickaxe();
    }

    @Override
    public boolean canContinueToUse() {
        return this.droid.hasFuel() && this.droid.hasValidPickaxe();
    }

    @Override
    public void start() {
        if (this.droid.getMode() == MinerDroidEntity.MODE_QUARRY) {
            this.currentQuarryY = this.droid.getStartY();
            this.quarryX = this.droid.getAreaMinX();
            this.quarryZ = this.droid.getAreaMinZ();
        }
        this.state = State.DIGGING;
    }

    @Override
    public void tick() {
        if (!this.droid.hasFuel() || !this.droid.hasValidPickaxe()) {
            this.droid.getNavigation().stop();
            this.droid.setWorking(false);
            this.state = State.IDLE;
            return;
        }

        switch (this.state) {
            case IDLE -> tickIdle();
            case DIGGING -> tickDigging();
            case DEPOSITING -> tickDepositing();
            case RETURNING -> tickReturning();
        }
    }

    private void tickIdle() {
        this.droid.setWorking(false);
        if (isInventoryFull()) {
            if (this.droid.getLinkedChest().isPresent()) {
                this.state = State.DEPOSITING;
            }
        } else {
            this.state = State.DIGGING;
        }
    }

    private void tickDigging() {
        if (isInventoryFull()) {
            this.droid.setWorking(false);
            this.state = this.droid.getLinkedChest().isPresent() ? State.DEPOSITING : State.IDLE;
            return;
        }

        Level level = this.droid.level();

        // 1. Continuous Hazard Clearing: Check for falling gravel/sand directly in front or above
        BlockPos overheadHazard = findFallingHazard();
        if (overheadHazard != null) {
            mineBlockAt(overheadHazard);
            return;
        }

        // 2. Patch Lava or Floor chasms
        patchHazardsAround();

        // 3. Find next mining target based on Mode
        if (this.currentTarget == null || level.getBlockState(this.currentTarget).isAir()) {
            this.currentTarget = findNextMiningBlock();
            this.miningProgress = 0;
            if (this.currentTarget == null) {
                // Completed tunnel or quarry!
                this.state = State.IDLE;
                this.droid.setWorking(false);
                return;
            }
        }

        // 4. Move within range and break block
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

        if (distSq <= 12.0D) {
            this.droid.getNavigation().stop();
            mineBlockAt(this.currentTarget);
        } else {
            if (++this.repathTimer >= 15 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(
                        this.currentTarget.getX() + 0.5D,
                        this.currentTarget.getY(),
                        this.currentTarget.getZ() + 0.5D,
                        this.speedModifier
                );
            }
        }
    }

    private BlockPos findFallingHazard() {
        Level level = this.droid.level();
        BlockPos pos = this.droid.blockPosition();

        // Check spaces in front and directly above
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

        // Check floor underneath
        BlockPos floor = pos.below();
        if (level.getBlockState(floor).isAir() || level.getBlockState(floor).is(Blocks.LAVA)) {
            placePatchBlock(floor);
        }

        // Check adjacent lava sources to plug them
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adj = pos.relative(dir);
            if (level.getBlockState(adj).is(Blocks.LAVA)) {
                placePatchBlock(adj);
            }
        }
    }

    private void placePatchBlock(BlockPos pos) {
        // Use cobblestone/deepslate from inventory to patch
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

    // Tunnel boring: 3x3 face moving forward
    private @org.jetbrains.annotations.Nullable BlockPos findNextTunnelBlock() {
        Level level = this.droid.level();
        Direction forward = this.droid.getInitialDirection();
        Direction right = forward.getClockWise();
        BlockPos home = this.droid.getHomePosition();
        int maxDist = this.droid.getMaxAllowedTunnelDistance();

        while (this.tunnelStep < maxDist) {
            BlockPos centerOfSlice = home.relative(forward, this.tunnelStep);

            // 3x3 face coordinates
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
        return null; // Completed tunnel length
    }

    // Quarry excavation: layer by layer downward, max 20 blocks depth!
    private @org.jetbrains.annotations.Nullable BlockPos findNextQuarryBlock() {
        Level level = this.droid.level();
        int minX = this.droid.getAreaMinX();
        int maxX = this.droid.getAreaMaxX();
        int minZ = this.droid.getAreaMinZ();
        int maxZ = this.droid.getAreaMaxZ();
        int startY = this.droid.getStartY();
        int maxDepthY = startY - 20; // STRICT 20-BLOCK ABSOLUTE DEPTH LIMIT!

        while (this.currentQuarryY >= maxDepthY) {
            while (this.quarryX <= maxX) {
                while (this.quarryZ <= maxZ) {
                    BlockPos target = new BlockPos(this.quarryX, this.currentQuarryY, this.quarryZ);
                    this.quarryZ++;

                    BlockState state = level.getBlockState(target);
                    // Place ladder at min corner
                    if (target.getX() == minX && target.getZ() == minZ) {
                        ensureLadderAt(target);
                    }

                    if (!state.isAir() && state.getDestroySpeed(level, target) >= 0 && !state.is(Blocks.LADDER)) {
                        return target;
                    }
                }
                this.quarryZ = minZ;
                this.quarryX++;
            }
            this.quarryX = minX;
            this.currentQuarryY--; // descend to next layer down
        }
        return null; // Reached 20-block absolute depth limit
    }

    private void ensureLadderAt(BlockPos pos) {
        Level level = this.droid.level();
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 3);
        }
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

        // Calculate dynamic mining speed based on pickaxe & block hardness
        float hardness = Math.max(0.5F, state.getDestroySpeed(serverLevel, target));
        float toolSpeed = this.droid.getMiningSpeedModifier(state);
        int requiredTicks = Math.max(2, (int) ((hardness * 20.0F) / toolSpeed));

        this.miningProgress++;

        if (this.miningProgress >= requiredTicks) {
            // Break block
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

    private void tickDepositing() {
        Optional<BlockPos> chestOpt = this.droid.getLinkedChest();
        if (chestOpt.isEmpty()) {
            this.state = State.RETURNING;
            return;
        }

        BlockPos chestPos = chestOpt.get();
        double distSq = this.droid.distanceToSqr(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);
        this.droid.getLookControl().setLookAt(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D);

        if (distSq <= 6.0D) {
            depositToChest(chestPos);
            this.state = State.RETURNING;
        } else {
            if (++this.repathTimer >= 20 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D, this.speedModifier);
            }
        }
    }

    private void depositToChest(BlockPos chestPos) {
        Level level = this.droid.level();
        BlockEntity be = level.getBlockEntity(chestPos);

        if (be instanceof Container targetContainer) {
            Container droidInv = this.droid.getInventory();
            boolean transferredAny = false;

            for (int i = 0; i < droidInv.getContainerSize(); i++) {
                ItemStack stack = droidInv.getItem(i);
                if (stack.isEmpty()) continue;

                // Keep up to 32 stones for patching lava/chasms, deposit everything else
                int toDeposit = stack.getCount();
                if ((stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE)) && stack.getCount() > 32) {
                    toDeposit = stack.getCount() - 32;
                } else if (stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE)) {
                    continue; // keep building stones
                }

                ItemStack split = stack.split(toDeposit);
                ItemStack remaining = insertIntoContainer(targetContainer, split);
                if (!remaining.isEmpty()) {
                    stack.grow(remaining.getCount());
                } else {
                    transferredAny = true;
                }
            }

            if (transferredAny) {
                level.playSound(null, chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
        }
    }

    private ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack inSlot = container.getItem(i);
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
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private void tickReturning() {
        BlockPos returnTarget = this.currentTarget != null ? this.currentTarget : this.droid.getHomePosition();
        double distSq = this.droid.distanceToSqr(returnTarget.getX() + 0.5D, returnTarget.getY(), returnTarget.getZ() + 0.5D);

        if (distSq <= 4.0D) {
            this.state = State.DIGGING;
        } else {
            if (++this.repathTimer >= 20 || this.droid.getNavigation().isDone()) {
                this.repathTimer = 0;
                this.droid.getNavigation().moveTo(returnTarget.getX() + 0.5D, returnTarget.getY(), returnTarget.getZ() + 0.5D, this.speedModifier);
            }
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
