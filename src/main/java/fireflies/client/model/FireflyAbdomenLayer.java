package fireflies.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenLayer<T extends FireflyEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation ABDOMEN0 = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly_layer_abdomen_0.png");
    private static final ResourceLocation ABDOMEN1 = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly_layer_abdomen_1.png");
    private static final ResourceLocation ABDOMEN2 = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly_layer_abdomen_2.png");
    public FireflyAbdomenLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getEntityTexture(T firefly) {
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
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, T firefly, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        final float glow = firefly.abdomenAnimationManager.abdomenAnimationProperties.glow;
        if (!firefly.isInvisible() && glow > .25f) {
            this.getEntityModel().render(matrixStack, buffer.getBuffer(glow >= .5f ? RenderType.getEyes(this.getEntityTexture(firefly)): RenderType.getEntityTranslucent(this.getEntityTexture(firefly)) ),
                    15728640, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
    }
}