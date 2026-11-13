package dev.droids.client.renderer;

import dev.droids.DroidsMod;
import dev.droids.client.model.MinerDroidModel;
import dev.droids.entity.MinerDroidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MinerDroidRenderer extends MobRenderer<MinerDroidEntity, MinerDroidModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "textures/entity/miner_droid.png");

    public MinerDroidRenderer(EntityRendererProvider.Context context) {
        super(context, new MinerDroidModel(context.bakeLayer(MinerDroidModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(MinerDroidEntity entity) {
        return TEXTURE;
    }
}
