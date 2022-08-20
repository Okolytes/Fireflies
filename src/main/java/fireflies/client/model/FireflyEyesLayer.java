package fireflies.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyEyesLayer<T extends FireflyEntity, M extends FireflyModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation EYES = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_eyes.png");

    public FireflyEyesLayer(RenderLayerParent<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getTextureLocation(T fireflyEntity) {
        return EYES;
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T fireflyEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!fireflyEntity.isInvisible()) {
            this.getParentModel().renderToBuffer(matrixStack, buffer.getBuffer(RenderType.eyes(this.getTextureLocation(fireflyEntity))), 15728640, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }
}