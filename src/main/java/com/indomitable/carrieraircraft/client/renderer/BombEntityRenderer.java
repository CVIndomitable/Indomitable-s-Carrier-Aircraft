package com.indomitable.carrieraircraft.client.renderer;

import com.indomitable.carrieraircraft.entity.BombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

/**
 * 炸弹渲染器 - Phase 1 简化版本
 *
 * 当前实现：使用 TNT 物品模型
 * TODO: Phase 2 添加自定义模型和纹理
 */
public class BombEntityRenderer extends EntityRenderer<BombEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse("minecraft:textures/block/tnt_side.png");

    private final ItemRenderer itemRenderer;

    public BombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BombEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 渲染为 TNT 物品（临时可视化）
        poseStack.scale(0.5F, 0.5F, 0.5F);
        itemRenderer.renderStatic(
                Items.TNT.getDefaultInstance(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BombEntity entity) {
        return TEXTURE;
    }
}
