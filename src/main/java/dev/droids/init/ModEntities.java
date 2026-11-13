package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DroidsMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FarmerDroidEntity>> FARMER_DROID =
            ENTITIES.register("farmer_droid", () ->
                    EntityType.Builder.of(FarmerDroidEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.2F)
                            .clientTrackingRange(10)
                            .build("farmer_droid"));

    public static final DeferredHolder<EntityType<?>, EntityType<MinerDroidEntity>> MINER_DROID =
            ENTITIES.register("miner_droid", () ->
                    EntityType.Builder.of(MinerDroidEntity::new, MobCategory.MISC)
                            .sized(0.8F, 1.3F)
                            .clientTrackingRange(10)
                            .build("miner_droid"));
}
