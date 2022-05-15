package fireflies.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyEyesLayer<T extends FireflyEntity, M extends FireflyModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation EYES = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly_layer_eyes.png");
    private static final ResourceLocation EYES_ILLUMERIN = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly_layer_eyes_illumerin.png");

    public FireflyEyesLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getEntityTexture(T fireflyEntity) {
        return fireflyEntity.hasIllumerin() ? EYES_ILLUMERIN : EYES;
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, T fireflyEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!fireflyEntity.isInvisible()) {
            final float eyeGlow = MathHelper.cos(ageInTicks * 0.05F) * 0.3F + 0.7f;
            this.getEntityModel().render(matrixStack, buffer.getBuffer(RenderType.getEyes(this.getEntityTexture(fireflyEntity))), 15728640, OverlayTexture.NO_OVERLAY,
                    eyeGlow, eyeGlow, eyeGlow, eyeGlow);
        }
    }
}