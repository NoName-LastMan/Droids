package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.item.DroidWrenchItem;
import dev.droids.item.FarmerDroidItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DroidsMod.MODID);

    public static final DeferredHolder<Item, FarmerDroidItem> FARMER_DROID =
            ITEMS.register("farmer_droid", () -> new FarmerDroidItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, DroidWrenchItem> DROID_WRENCH =
            ITEMS.register("droid_wrench", () -> new DroidWrenchItem(new Item.Properties().stacksTo(1)));
}
