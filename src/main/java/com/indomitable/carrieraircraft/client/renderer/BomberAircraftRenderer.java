package com.indomitable.carrieraircraft.client.renderer;

import com.indomitable.carrieraircraft.client.model.B25Model;
import com.indomitable.carrieraircraft.entity.BomberAircraftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * B-25轰炸机渲染器 - 使用本地B-25模型
 */
public class BomberAircraftRenderer extends EntityRenderer<BomberAircraftEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("indomitablecarrieraircraft:textures/entity/b25.png");

    private final B25Model<BomberAircraftEntity> model;

    public BomberAircraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.5F;
        this.model = new B25Model<>(context.bakeLayer(B25Model.LAYER_LOCATION));
    }

    @Override
    public void render(BomberAircraftEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 标准MC实体旋转：YP(180 - yaw) + scale(-1, -1, 1)
        // 模型朝向-Z（机头在Z=-5），此旋转将机头转到+Z（yaw=0时朝南）
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        // 俯仰角处理
        float pitch = net.minecraft.util.Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        // 缩放和位移
        poseStack.scale(-1.5F, -1.5F, 1.5F);
        poseStack.translate(0.0, -1.501, 0.0);

        // 渲染模型
        VertexConsumer vertexConsumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BomberAircraftEntity entity) {
        return TEXTURE;
    }
}
