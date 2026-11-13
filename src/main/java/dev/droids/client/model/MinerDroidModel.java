package dev.droids.client.model;

import dev.droids.DroidsMod;
import dev.droids.entity.MinerDroidEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 3D Model for the Heavy Clockwork Miner Droid.
 * Features a mining headlamp on the forehead, heavy hydraulic arms, and thick chimney.
 */
public class MinerDroidModel extends HierarchicalModel<MinerDroidEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(DroidsMod.MODID, "miner_droid"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MinerDroidModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Thick reinforced legs
        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.5F, 18.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(20, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.5F, 18.0F, 0.0F));

        // Heavy Iron Boiler Body + Double Exhaust Chimneys
        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 14).addBox(-6.0F, -11.0F, -3.5F, 12.0F, 11.0F, 7.0F, new CubeDeformation(0.0F))
                        // Dual heavy chimneys on the back
                        .texOffs(38, 14).addBox(-4.0F, -15.0F, 2.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 14).addBox(1.0F, -15.0F, 2.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        // Head with Mining Headlamp
        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
                        // Protruding Mining Headlamp / Lantern
                        .texOffs(32, 0).addBox(-2.0F, -5.5F, -5.5F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        // Heavy Drill/Pickaxe Socket Arm (Right)
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 24).addBox(-2.5F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.0F, -10.0F, 0.0F));

        // Heavy Gripper Arm (Left)
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 38).addBox(-1.5F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(7.0F, -10.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(MinerDroidEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        if (!entity.hasFuel()) {
            return; // Shutdown: completely frozen
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Walking animation
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.3F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.3F * limbSwingAmount;

        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.0F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbSwingAmount;

        // Rapid drilling/pickaxe swing animation while mining
        if (entity.isWorking()) {
            this.rightArm.xRot = -1.2F + Mth.sin(ageInTicks * 0.8F) * 0.8F;
        }
    }
}
