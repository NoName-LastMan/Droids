package dev.droids.item;

import dev.droids.entity.FarmerDroidEntity;
import dev.droids.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Item used to deploy a Farmer Droid onto a block.
 */
public class FarmerDroidItem extends Item {

    public FarmerDroidItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clickedPos).getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(clickedFace);

        FarmerDroidEntity droid = ModEntities.FARMER_DROID.get().create(
                serverLevel,
                null,
                spawnPos,
                MobSpawnType.SPAWN_EGG,
                true,
                !clickedPos.equals(spawnPos) && clickedFace == Direction.UP
        );

        if (droid != null) {
            float yRot = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
            droid.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, yRot, 0.0F);
            serverLevel.addFreshEntityWithPassengers(droid);
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
            itemStack.consume(1, context.getPlayer());
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
