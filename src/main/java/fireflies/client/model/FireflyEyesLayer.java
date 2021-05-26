package fireflies.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyEyesLayer<T extends FireflyEntity, M extends FireflyModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation EYES = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_eyes.png");
    private static final ResourceLocation EYES_REDSTONE = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_eyes_redstone.png");

    public FireflyEyesLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getEntityTexture(T fireflyEntity) {
        return fireflyEntity.isRedstoneActivated(true) ? EYES_REDSTONE : EYES;
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, T fireflyEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getEntityModel().render(matrixStack, buffer.getBuffer(RenderType.getEyes(this.getEntityTexture(fireflyEntity))),
                15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}