package dev.droids.item;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.menu.DroidControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Handheld Clockwork Droid Telemetry Monitor Tablet.
 *
 * Usage:
 * - Right-click Droid Station / Droid → Link tablet to that station.
 * - Right-click in AIR                 → Open telemetry monitor dashboard.
 */
public class DroidControllerItem extends Item {

    public DroidControllerItem(Properties properties) {
        super(properties);
    }

    // ─── useOn: Link to Station ────────────────────────────────────────────────

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DroidStationBlockEntity) {
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("StationX", pos.getX());
                    tag.putInt("StationY", pos.getY());
                    tag.putInt("StationZ", pos.getZ());
                });
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                player.displayClientMessage(
                        Component.translatable("message.droids.controller.linked", pos.getX(), pos.getY(), pos.getZ()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // If clicked on any other block, open menu if linked
        return use(level, player, context.getHand()).getResult();
    }

    // ─── interactLivingEntity: Link tablet by right-clicking the Droid itself ──

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        if (target instanceof dev.droids.entity.AbstractDroidEntity droid) {
            BlockPos home = droid.getHomePosition();
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("StationX", home.getX());
                    tag.putInt("StationY", home.getY());
                    tag.putInt("StationZ", home.getZ());
                });
                level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                player.displayClientMessage(
                        Component.translatable("message.droids.controller.linked", home.getX(), home.getY(), home.getZ()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    // ─── use (right-click in air): Open telemetry dashboard ───────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("StationX")) {
            int sx = customData.copyTag().getInt("StationX");
            int sy = customData.copyTag().getInt("StationY");
            int sz = customData.copyTag().getInt("StationZ");
            BlockPos stationPos = new BlockPos(sx, sy, sz);

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                        new SimpleMenuProvider(
                                (containerId, playerInv, p) -> new DroidControllerMenu(containerId, playerInv, stationPos, stack),
                                Component.translatable("item.droids.droid_controller")
                        ),
                        buffer -> {
                            buffer.writeBlockPos(stationPos);
                        }
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.droids.controller.unlinked"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    // ─── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        if (customData != null && customData.contains("StationX")) {
            int sx = customData.copyTag().getInt("StationX");
            int sy = customData.copyTag().getInt("StationY");
            int sz = customData.copyTag().getInt("StationZ");
            tooltipComponents.add(Component.translatable("tooltip.droids.controller.linked_pos", sx, sy, sz));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.droids.controller.not_linked"));
        }

        tooltipComponents.add(Component.literal("§7Right-click in air to view Telemetry."));
    }
}
