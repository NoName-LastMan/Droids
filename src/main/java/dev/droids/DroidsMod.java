package dev.droids;

import dev.droids.entity.FarmerDroidEntity;
import dev.droids.entity.MinerDroidEntity;
import dev.droids.init.ModBlockEntities;
import dev.droids.init.ModBlocks;
import dev.droids.init.ModCreativeTabs;
import dev.droids.init.ModEntities;
import dev.droids.init.ModItems;
import dev.droids.init.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DroidsMod.MODID)
public class DroidsMod {
    public static final String MODID = "droids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public DroidsMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Droids Mod (id: {})", MODID);

        // Register registries to mod event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        // Register event listeners
        modEventBus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FARMER_DROID.get(), FarmerDroidEntity.createAttributes().build());
        event.put(ModEntities.MINER_DROID.get(), MinerDroidEntity.createAttributes().build());
    }
}
