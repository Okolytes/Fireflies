package fireflies.entity.firefly;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.Fireflies;
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
    private static final ResourceLocation ABDOMEN = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen.png");
    private float i;
    private boolean pulseFlag;

    public FireflyAbdomenLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T firefly, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        RenderType eyes = RenderType.getEyes(ABDOMEN);
        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(eyes);

        firefly.animation = FireflyAbdomenAnimation.PULSE;
        switch (firefly.animation) {
            case OFF:
                i = 0;
                break;
            case ON:
                i = 1;
                break;
            case CALM:

                break;
            case PULSE:
                i += pulseFlag ? 0.002f : -0.002f;
                if (i <= 0) {
                    pulseFlag = true;
                    i = 0;
                } else if (i >= 1) {
                    pulseFlag = false;
                    i = 1;
                }
                break;
        }
        // the alpha parameter does not seem to do anything ?
        this.getEntityModel().render(matrixStackIn, ivertexbuilder, 15728640, OverlayTexture.NO_OVERLAY, i, i, i, 1F);
    }
}