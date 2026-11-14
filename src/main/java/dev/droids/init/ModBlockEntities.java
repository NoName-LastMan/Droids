package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.block.entity.DroidStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DroidsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroidStationBlockEntity>> DROID_STATION =
            BLOCK_ENTITIES.register("droid_station", () ->
                    BlockEntityType.Builder.of(DroidStationBlockEntity::new, ModBlocks.DROID_STATION.get()).build(null));
}
