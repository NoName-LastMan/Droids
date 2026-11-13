package dev.droids.client.renderer;

import dev.droids.DroidsMod;
import dev.droids.client.model.FarmerDroidModel;
import dev.droids.entity.FarmerDroidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FarmerDroidRenderer extends MobRenderer<FarmerDroidEntity, FarmerDroidModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/entity/farmer_droid.png");

    public FarmerDroidRenderer(EntityRendererProvider.Context context) {
        super(context, new FarmerDroidModel(context.bakeLayer(FarmerDroidModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FarmerDroidEntity entity) {
        return TEXTURE;
    }
}
