package com.indomitable.carrieraircraft.client.model;

import com.indomitable.carrieraircraft.IndomitableCarrierAircraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * B-25 bomber entity model (from Port Royale mod via sheropshire).
 * Used for B-25 bomber aircraft.
 */
public class B25Model<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(IndomitableCarrierAircraft.MOD_ID, "b25"), "main");

    private final ModelPart bb_main;

    public B25Model(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -2.0F, -5.0F, 1.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
                        .texOffs(17, 13).addBox(-6.0F, -1.4F, -2.6F, 12.0F, 0.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(13, 3).addBox(-3.0F, -2.0F, 5.0F, 6.0F, 0.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 3).addBox(2.5F, -1.4F, -3.6F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 2).addBox(-3.0F, -4.0F, 5.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 2).addBox(3.0F, -4.0F, 5.0F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-3.5F, -1.4F, -3.6F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(11, 0).addBox(-7.0F, -1.4F, -1.6F, 14.0F, 0.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 33).addBox(-6.0F, -1.4F, -0.6F, 12.0F, 0.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 43).addBox(-0.5F, -1.0F, -6.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 44).addBox(-0.5F, -2.4F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
