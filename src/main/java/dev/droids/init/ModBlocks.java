package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.block.DroidStationBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(DroidsMod.MODID);

    public static final DeferredBlock<DroidStationBlock> DROID_STATION =
            BLOCKS.register("droid_station", () -> new DroidStationBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, BlockItem> DROID_STATION_ITEM =
            ModItems.ITEMS.register("droid_station", () -> new BlockItem(DROID_STATION.get(), new Item.Properties()));
}
