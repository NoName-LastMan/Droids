package dev.droids.entity;

import dev.droids.entity.ai.FarmerBatchRoutineGoal;
import dev.droids.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Autonomous Clockwork Farmer Droid.
 * Operates on a scheduled batch routine: scans fields on 10s intervals,
 * chain-harvests all mature crops without pauses, unloads to storage,
 * and docks at its home Droid Station in motionless standby.
 */
public class FarmerDroidEntity extends AbstractDroidEntity {

    public FarmerDroidEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseDroidAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FarmerBatchRoutineGoal(this, 1.0D));
    }

    @Override
    public Item getDropItem() {
        return ModItems.FARMER_DROID.get();
    }

    @Override
    public Component getDroidTypeName() {
        return Component.literal("Farmer Droid");
    }

    @Override
    public String getSpecificStatus() {
        return isWorking() ? "Harvesting" : "Standby";
    }

    @Override
    public int getMaxAllowedAreaDimension() {
        return switch (getRangeUpgradesCount()) {
            case 1 -> 9;   // 9x9
            case 2 -> 15;  // 15x15
            case 3 -> 21;  // 21x21
            case 4 -> 31;  // 31x31
            default -> 5;  // 5x5 base
        };
    }

    @Override
    public void setWorkArea(int minX, int minZ, int maxX, int maxZ) {
        super.setWorkArea(minX, minZ, maxX, maxZ);
        // Position home dock at center of configured area
        setHomePosition(new BlockPos((minX + maxX) / 2, this.getBlockY(), (minZ + maxZ) / 2));
    }
}
