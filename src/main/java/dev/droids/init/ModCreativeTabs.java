package dev.droids.init;

import dev.droids.DroidsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DroidsMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DROIDS_TAB =
            CREATIVE_MODE_TABS.register("droids_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.droids"))
                    .icon(() -> new ItemStack(ModItems.FARMER_DROID.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FARMER_DROID.get());
                        output.accept(ModItems.DROID_WRENCH.get());
                    })
                    .build());
}
