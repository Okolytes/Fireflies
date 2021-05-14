package fireflies.entity.firefly;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.Color;

@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenLayer<T extends FireflyEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation ABDOMEN = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen.png");

    private float animationValue;
    private boolean animationFlag;

    public FireflyAbdomenLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T firefly, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (firefly.isInvisible())
            return;

        firefly.animation = FireflyAbdomenAnimation.CALM_SLOW_SYNCHRONIZED;
        switch (firefly.animation) {
            case OFF:
                firefly.animationValue = 0;
                break;
            case ON:
                firefly.animationValue = 1;
                break;
            case CALM:
                firefly.animationValue += firefly.animationFlag ? 0.004f : -0.004f;
                if (firefly.animationValue <= 0) {
                    firefly.animationFlag = true;
                    firefly.animationValue = 0;
                } else if (firefly.animationValue >= 1) {
                    firefly.animationFlag = false;
                    firefly.animationValue = 1;
                }
                break;
            case CALM_SLOW_SYNCHRONIZED:
                animationValue += animationFlag ? 0.001f : -0.001f;
                if (animationValue <= 0) {
                    animationFlag = true;
                    animationValue = 0;
                } else if (animationValue >= 1) {
                    animationFlag = false;
                    animationValue = 1;
                }
                firefly.animationValue = animationValue;
                break;
            case STARRY_NIGHT:
                firefly.animationValue += firefly.animationFlag ? 0.05f : -0.01f;
                if (firefly.animationValue <= 0) {
                    firefly.animationFlag = true;
                    firefly.animationValue = 0;
                } else if (firefly.animationValue >= 1) {
                    firefly.animationFlag = false;
                    firefly.animationValue = 1;
                }
                break;
            case STARRY_NIGHT_SYNCHRONIZED:
                animationValue += animationFlag ? 0.01f : -0.005f;
                if (animationValue <= 0) {
                    animationFlag = true;
                    animationValue = 0;
                } else if (animationValue >= 1) {
                    animationFlag = false;
                    animationValue = 1;
                }
                firefly.animationValue = animationValue;
                break;
            case GAMER:
                firefly.animationValue += 0.002f;
                if (firefly.animationValue >= 1) {
                    firefly.animationValue = 0;
                }
                Color c = Color.getHSBColor(firefly.animationValue, 1f, 1f);
                this.getEntityModel().render(matrixStackIn, bufferIn.getBuffer(RenderType.getEyes(ABDOMEN)), 15728640,
                        OverlayTexture.NO_OVERLAY, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
                return;
        }
        // the alpha parameter does not seem to do anything ?
        this.getEntityModel().render(matrixStackIn, bufferIn.getBuffer(RenderType.getEyes(ABDOMEN)), 15728640,
                OverlayTexture.NO_OVERLAY, firefly.animationValue, firefly.animationValue, firefly.animationValue, 1f);
    }
}