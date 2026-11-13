package dev.droids.client;

import dev.droids.DroidsMod;
import dev.droids.client.model.FarmerDroidModel;
import dev.droids.client.renderer.FarmerDroidRenderer;
import dev.droids.client.screen.FarmerDroidScreen;
import dev.droids.init.ModEntities;
import dev.droids.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = DroidsMod.MODID, value = Dist.CLIENT)
public class DroidsClient {

    @SubscribeEvent
    public static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FarmerDroidModel.LAYER_LOCATION, FarmerDroidModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FARMER_DROID.get(), FarmerDroidRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FARMER_DROID_MENU.get(), FarmerDroidScreen::new);
    }
}
