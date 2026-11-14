package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.menu.DroidControllerMenu;
import dev.droids.menu.DroidStationMenu;
import dev.droids.menu.FarmerDroidMenu;
import dev.droids.menu.MinerDroidMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DroidsMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FarmerDroidMenu>> FARMER_DROID_MENU =
            MENUS.register("farmer_droid", () -> IMenuTypeExtension.create(FarmerDroidMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MinerDroidMenu>> MINER_DROID_MENU =
            MENUS.register("miner_droid", () -> IMenuTypeExtension.create(MinerDroidMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DroidStationMenu>> DROID_STATION_MENU =
            MENUS.register("droid_station", () -> new MenuType<>(DroidStationMenu::new, FeatureFlagSet.of()));

    public static final DeferredHolder<MenuType<?>, MenuType<DroidControllerMenu>> DROID_CONTROLLER_MENU =
            MENUS.register("droid_controller", () -> IMenuTypeExtension.create(DroidControllerMenu::new));
}
