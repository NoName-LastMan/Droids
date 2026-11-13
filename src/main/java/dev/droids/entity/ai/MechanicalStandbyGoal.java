package dev.droids.entity.ai;

import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Mechanical Standby Goal: When fueled but idle (no crops ready and nothing to deposit),
 * the Droid returns to the center of its assigned area and stands completely still in standby mode.
 */
public class MechanicalStandbyGoal extends Goal {

    private final FarmerDroidEntity droid;
    private final double speedModifier;
    private int cooldown = 0;

    public MechanicalStandbyGoal(FarmerDroidEntity droid, double speedModifier) {
        this.droid = droid;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.droid.hasFuel() || this.droid.isWorking()) {
            return false;
        }

        if (--this.cooldown > 0) {
            return false;
        }
        this.cooldown = 30; // check every 1.5s

        BlockPos center = this.droid.getWorkAreaCenter();
        double distSq = this.droid.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        return distSq > 4.0D;
    }

    @Override
    public void start() {
        BlockPos center = this.droid.getWorkAreaCenter();
        this.droid.getNavigation().moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, this.speedModifier);
    }

    @Override
    public void tick() {
        BlockPos center = this.droid.getWorkAreaCenter();
        double distSq = this.droid.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);

        if (distSq <= 4.0D) {
            this.droid.getNavigation().stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.droid.hasFuel() || this.droid.isWorking()) {
            return false;
        }
        BlockPos center = this.droid.getWorkAreaCenter();
        double distSq = this.droid.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        return distSq > 4.0D && !this.droid.getNavigation().isDone();
    }
}
