package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.item.DroidControllerItem;
import dev.droids.item.DroidWrenchItem;
import dev.droids.item.FarmerDroidItem;
import dev.droids.item.MinerDroidItem;
import dev.droids.item.RangeUpgradeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DroidsMod.MODID);

    public static final DeferredHolder<Item, FarmerDroidItem> FARMER_DROID =
            ITEMS.register("farmer_droid", () -> new FarmerDroidItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, MinerDroidItem> MINER_DROID =
            ITEMS.register("miner_droid", () -> new MinerDroidItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, DroidWrenchItem> DROID_WRENCH =
            ITEMS.register("droid_wrench", () -> new DroidWrenchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, DroidControllerItem> DROID_CONTROLLER =
            ITEMS.register("droid_controller", () -> new DroidControllerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, RangeUpgradeItem> RANGE_UPGRADE =
            ITEMS.register("range_upgrade", () -> new RangeUpgradeItem(new Item.Properties().stacksTo(4)));
}
