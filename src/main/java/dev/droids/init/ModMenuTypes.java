package dev.droids.init;

import dev.droids.DroidsMod;
import dev.droids.menu.FarmerDroidMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DroidsMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FarmerDroidMenu>> FARMER_DROID_MENU =
            MENUS.register("farmer_droid", () -> IMenuTypeExtension.create(FarmerDroidMenu::new));
}
