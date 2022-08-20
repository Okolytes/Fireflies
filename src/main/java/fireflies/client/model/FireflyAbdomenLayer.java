package fireflies.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenLayer<T extends FireflyEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation ABDOMEN0 = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen_0.png");
    private static final ResourceLocation ABDOMEN1 = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen_1.png");
    private static final ResourceLocation ABDOMEN2 = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen_2.png");
    public FireflyAbdomenLayer(RenderLayerParent<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getTextureLocation(T firefly) {
        final float glow = firefly.abdomenAnimationManager.abdomenAnimationProperties.glow;
        if(glow >= .75f){
            return ABDOMEN0;
        } else if(glow >= .5f){
            return ABDOMEN1;
        }
        else if(glow >= .25f){
            return ABDOMEN2;
        }
        return null;
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T firefly, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        final float glow = firefly.abdomenAnimationManager.abdomenAnimationProperties.glow;
        if (!firefly.isInvisible() && glow > .25f) {
            this.getParentModel().renderToBuffer(matrixStack, buffer.getBuffer(glow >= .5f ? RenderType.eyes(this.getTextureLocation(firefly)): RenderType.entityTranslucent(this.getTextureLocation(firefly)) ),
                    15728640, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
    }
}