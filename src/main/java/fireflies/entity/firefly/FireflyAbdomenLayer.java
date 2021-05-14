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

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenLayer<T extends FireflyEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation ABDOMEN = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_abdomen.png");

    public FireflyAbdomenLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T firefly, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (firefly.isInvisible())
            return;

        float v;
        switch (firefly.animation) {
            case CALM_SYNCHRONIZED:
                v = MathHelper.lerp(partialTicks, FireflyAbdomenSync.calmGlowTime, FireflyAbdomenSync.calmGlowTime);
                break;
            case STARRY_NIGHT_SYNCHRONIZED:
                v = MathHelper.lerp(partialTicks, FireflyAbdomenSync.starryNightGlowTime, FireflyAbdomenSync.starryNightGlowTime);
                break;
            case GAMER:
                firefly.glowTime = MathHelper.lerp(partialTicks, FireflyAbdomenSync.calmGlowTime, FireflyAbdomenSync.calmGlowTime);
                if (firefly.glowTime >= 1) {
                    firefly.glowTime = 0;
                }
                Color c = Color.getHSBColor(firefly.glowTime, 1f, 1f);
                this.getEntityModel().render(matrixStackIn, bufferIn.getBuffer(RenderType.getEyes(ABDOMEN)), 15728640,
                        OverlayTexture.NO_OVERLAY, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
                return;
            default:
                v = MathHelper.lerp(partialTicks, firefly.prevGlowTime, firefly.glowTime);
                break;
        }
        //System.out.printf("%f\n", v);
        // the alpha parameter does not seem to do anything ?
        this.getEntityModel().render(matrixStackIn, bufferIn.getBuffer(RenderType.getEyes(ABDOMEN)), 15728640,
                OverlayTexture.NO_OVERLAY, v, v, v, 1f);

    }
}