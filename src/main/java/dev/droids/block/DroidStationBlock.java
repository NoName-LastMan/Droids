package dev.droids.block;

import com.mojang.serialization.MapCodec;
import dev.droids.block.entity.DroidStationBlockEntity;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModBlockEntities;
import dev.droids.init.ModEntities;
import dev.droids.init.ModItems;
import dev.droids.item.FarmerDroidItem;
import dev.droids.item.MinerDroidItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Droid Station Block: The physical docking cradle and logistics center for Droids.
 */
public class DroidStationBlock extends BaseEntityBlock {

    public static final MapCodec<DroidStationBlock> CODEC = simpleCodec(DroidStationBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");

    // Heavy metallic docking pad shape (14 pixels tall)
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public DroidStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OCCUPIED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OCCUPIED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroidStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.DROID_STATION.get(), DroidStationBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Deploy Miner Droid onto Station
        if (stack.getItem() instanceof MinerDroidItem) {
            if (state.getValue(OCCUPIED)) {
                if (!level.isClientSide && player != null) {
                    player.displayClientMessage(Component.translatable("message.droids.station_occupied"), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                BlockPos spawnPos = pos.above();
                MinerDroidEntity droid = ModEntities.MINER_DROID.get().create(serverLevel, null, spawnPos, MobSpawnType.SPAWN_EGG, true, false);
                if (droid != null) {
                    droid.moveTo(pos.getX() + 0.5D, pos.getY() + 0.88D, pos.getZ() + 0.5D, state.getValue(FACING).toYRot(), 0.0F);
                    droid.setHomePosition(pos);
                    droid.setInitialDirection(state.getValue(FACING));
                    serverLevel.addFreshEntityWithPassengers(droid);

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof DroidStationBlockEntity station) {
                        station.setAssignedDroid(droid);
                    }

                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
                    level.setBlock(pos, state.setValue(OCCUPIED, true), 3);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Deploy Farmer Droid onto Station
        if (stack.getItem() instanceof FarmerDroidItem) {
            if (state.getValue(OCCUPIED)) {
                if (!level.isClientSide && player != null) {
                    player.displayClientMessage(Component.translatable("message.droids.station_occupied"), true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                BlockPos spawnPos = pos.above();
                FarmerDroidEntity droid = ModEntities.FARMER_DROID.get().create(serverLevel, null, spawnPos, MobSpawnType.SPAWN_EGG, true, false);
                if (droid != null) {
                    droid.moveTo(pos.getX() + 0.5D, pos.getY() + 0.88D, pos.getZ() + 0.5D, state.getValue(FACING).toYRot(), 0.0F);
                    droid.setHomePosition(pos);
                    serverLevel.addFreshEntityWithPassengers(droid);

                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof DroidStationBlockEntity station) {
                        station.setAssignedDroid(droid);
                    }

                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
                    level.setBlock(pos, state.setValue(OCCUPIED, true), 3);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DroidStationBlockEntity station) {
                player.openMenu(station);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DroidStationBlockEntity station) {
                Containers.dropContents(level, pos, station);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
