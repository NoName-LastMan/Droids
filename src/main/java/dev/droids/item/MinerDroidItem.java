package dev.droids.item;

import dev.droids.block.DroidStationBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item representing a compact Miner Droid.
 * Must be deployed onto a Droid Station docking pad.
 */
public class MinerDroidItem extends Item {

    public MinerDroidItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        Player player = context.getPlayer();

        if (!(state.getBlock() instanceof DroidStationBlock)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.droids.must_place_on_station"), true);
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }
}
