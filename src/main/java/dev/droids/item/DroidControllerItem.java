package dev.droids.item;

import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.menu.DroidControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Handheld Steampunk Droid Controller Tablet.
 *
 * Usage:
 * - Right-click Droid Station / Droid  → Link tablet to that station.
 * - Right-click any block (non-station) → Mark Corner 1 or Corner 2 for the work area.
 * - Shift + Right-click any block       → Clears all stored area corners.
 * - Right-click in AIR                  → Open telemetry / control dashboard.
 *
 * Area is applied to the assigned Droid via "Apply Area" button inside the dashboard.
 */
public class DroidControllerItem extends Item {

    public DroidControllerItem(Properties properties) {
        super(properties);
    }

    // ─── useOn: Link to Station OR mark area corners ───────────────────────────

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // ── Linking: Right-click on Droid Station ──────────────────────────────
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DroidStationBlockEntity) {
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("StationX", pos.getX());
                    tag.putInt("StationY", pos.getY());
                    tag.putInt("StationZ", pos.getZ());
                    // Clear any pending area corners when re-linking
                    tag.remove("C1X"); tag.remove("C1Y"); tag.remove("C1Z");
                    tag.remove("C2X"); tag.remove("C2Y"); tag.remove("C2Z");
                });
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
                player.displayClientMessage(
                        Component.translatable("message.droids.controller.linked", pos.getX(), pos.getY(), pos.getZ()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ── Area marking: Shift + Right-click = clear corners ─────────────────
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.remove("C1X"); tag.remove("C1Y"); tag.remove("C1Z");
                    tag.remove("C2X"); tag.remove("C2Y"); tag.remove("C2Z");
                });
                player.displayClientMessage(Component.translatable("message.droids.controller.area_cleared"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ── Area marking: Normal Right-click on any block = mark corner ────────
        if (!level.isClientSide) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            boolean hasC1 = customData != null && customData.contains("C1X");

            if (!hasC1) {
                // Set Corner 1
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("C1X", pos.getX());
                    tag.putInt("C1Y", pos.getY());
                    tag.putInt("C1Z", pos.getZ());
                    tag.remove("C2X"); tag.remove("C2Y"); tag.remove("C2Z");
                });
                player.displayClientMessage(
                        Component.literal("§e[Controller] §7Corner 1 marked at §f(" + pos.getX() + ", " + pos.getZ() + ")§7. Mark Corner 2 or open tablet to apply."),
                        true
                );
            } else {
                // Set Corner 2 and show preview
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("C2X", pos.getX());
                    tag.putInt("C2Y", pos.getY());
                    tag.putInt("C2Z", pos.getZ());
                });
                CustomData updatedData = stack.get(DataComponents.CUSTOM_DATA);
                int c1x = updatedData.copyTag().getInt("C1X");
                int c1z = updatedData.copyTag().getInt("C1Z");
                int w = Math.abs(pos.getX() - c1x) + 1;
                int l = Math.abs(pos.getZ() - c1z) + 1;
                player.displayClientMessage(
                        Component.literal("§e[Controller] §7Corner 2 marked. Area: §a" + w + "×" + l + "§7 blocks. Open tablet to apply."),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ─── interactLivingEntity: Link tablet by right-clicking the Droid itself ──

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        BlockPos home = null;
        if (target instanceof MinerDroidEntity miner) {
            home = miner.getHomePosition();
        } else if (target instanceof FarmerDroidEntity farmer) {
            home = farmer.getHomePosition();
        }

        if (home != null) {
            if (!level.isClientSide) {
                final BlockPos finalHome = home;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("StationX", finalHome.getX());
                    tag.putInt("StationY", finalHome.getY());
                    tag.putInt("StationZ", finalHome.getZ());
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
                            // Pass pending corners to menu
                            boolean hasC1 = customData.contains("C1X");
                            boolean hasC2 = customData.contains("C2X");
                            buffer.writeBoolean(hasC1);
                            if (hasC1) {
                                buffer.writeInt(customData.copyTag().getInt("C1X"));
                                buffer.writeInt(customData.copyTag().getInt("C1Y"));
                                buffer.writeInt(customData.copyTag().getInt("C1Z"));
                            }
                            buffer.writeBoolean(hasC2);
                            if (hasC2) {
                                buffer.writeInt(customData.copyTag().getInt("C2X"));
                                buffer.writeInt(customData.copyTag().getInt("C2Y"));
                                buffer.writeInt(customData.copyTag().getInt("C2Z"));
                            }
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

            boolean hasC1 = customData.contains("C1X");
            boolean hasC2 = customData.contains("C2X");
            if (hasC1 && hasC2) {
                int c1x = customData.copyTag().getInt("C1X");
                int c1z = customData.copyTag().getInt("C1Z");
                int c2x = customData.copyTag().getInt("C2X");
                int c2z = customData.copyTag().getInt("C2Z");
                int w = Math.abs(c2x - c1x) + 1;
                int l = Math.abs(c2z - c1z) + 1;
                tooltipComponents.add(Component.literal("§aArea Marked: " + w + "×" + l));
            } else if (hasC1) {
                int c1x = customData.copyTag().getInt("C1X");
                int c1z = customData.copyTag().getInt("C1Z");
                tooltipComponents.add(Component.literal("§eCorner 1: (" + c1x + ", " + c1z + ") — awaiting Corner 2"));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.droids.controller.not_linked"));
        }

        tooltipComponents.add(Component.translatable("tooltip.droids.controller.instructions"));
    }

    // ─── Static helper: apply pending area from item to assigned droid ─────────

    /**
     * Called when player presses "Apply Area" button (button id=3) in the dashboard.
     * Reads corners from the item in the player's hand and applies them to the droid.
     */
    public static boolean applyAreaToDroid(Player player, BlockPos stationPos) {
        ItemStack stack = findControllerInHands(player);
        if (stack.isEmpty()) return false;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains("C1X") || !customData.contains("C2X")) return false;

        int c1x = customData.copyTag().getInt("C1X");
        int c1z = customData.copyTag().getInt("C1Z");
        int c2x = customData.copyTag().getInt("C2X");
        int c2z = customData.copyTag().getInt("C2Z");

        int minX = Math.min(c1x, c2x);
        int maxX = Math.max(c1x, c2x);
        int minZ = Math.min(c1z, c2z);
        int maxZ = Math.max(c1z, c2z);

        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        if (!(serverLevel.getBlockEntity(stationPos) instanceof DroidStationBlockEntity station)) return false;

        var droidEntity = station.getAssignedDroid(serverLevel);

        if (droidEntity instanceof MinerDroidEntity miner) {
            int max = miner.getMaxAllowedQuarryDimension();
            int w = maxX - minX + 1;
            int l = maxZ - minZ + 1;
            if (w > max || l > max) {
                player.displayClientMessage(
                        Component.literal("§c[Controller] Area " + w + "×" + l + " exceeds limit (" + max + "×" + max + "). Add Range Upgrades."),
                        false
                );
                return false;
            }
            miner.setWorkArea(minX, minZ, maxX, maxZ);
            player.displayClientMessage(
                    Component.literal("§a[Controller] Area " + w + "×" + l + " applied to Miner Droid!"),
                    true
            );
        } else if (droidEntity instanceof FarmerDroidEntity farmer) {
            int max = farmer.getMaxAllowedAreaDimension();
            int w = maxX - minX + 1;
            int l = maxZ - minZ + 1;
            if (w > max || l > max) {
                player.displayClientMessage(
                        Component.literal("§c[Controller] Area " + w + "×" + l + " exceeds limit (" + max + "×" + max + "). Add Range Upgrades."),
                        false
                );
                return false;
            }
            farmer.setWorkArea(minX, minZ, maxX, maxZ);
            player.displayClientMessage(
                    Component.literal("§a[Controller] Area " + w + "×" + l + " applied to Farmer Droid!"),
                    true
            );
        } else {
            player.displayClientMessage(Component.literal("§c[Controller] No Droid docked at that Station."), false);
            return false;
        }

        // Clear corners from item after successful application
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("C1X"); tag.remove("C1Y"); tag.remove("C1Z");
            tag.remove("C2X"); tag.remove("C2Y"); tag.remove("C2Z");
        });

        return true;
    }

    private static ItemStack findControllerInHands(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DroidControllerItem) return stack;
        }
        return ItemStack.EMPTY;
    }
}
