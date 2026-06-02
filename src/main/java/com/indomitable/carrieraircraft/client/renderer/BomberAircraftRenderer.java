package com.indomitable.carrieraircraft.client.renderer;

import com.indomitable.carrieraircraft.entity.BomberAircraftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 轰炸机渲染器 - Phase 1 简化版本
 *
 * 当前实现：使用简单的方块模型
 * TODO: Phase 2 添加自定义模型和纹理
 */
public class BomberAircraftRenderer extends EntityRenderer<BomberAircraftEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("indomitablecarrieraircraft:textures/entity/bomber_aircraft.png");

    public BomberAircraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.5F;
    }

    @Override
    public void render(BomberAircraftEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 简单旋转以匹配飞机朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));

        // TODO: Phase 2 - 渲染自定义模型
        // 当前为空实现，只显示阴影

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BomberAircraftEntity entity) {
        return TEXTURE;
    }
}
