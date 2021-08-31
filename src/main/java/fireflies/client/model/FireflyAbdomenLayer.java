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
    private static final ResourceLocation ABDOMEN = new ResourceLocation(Fireflies.ID, "textures/entity/firefly_layer_abdomen.png");
    private static final ResourceLocation ABDOMEN_REDSTONE = new ResourceLocation(Fireflies.ID, "textures/entity/firefly_layer_abdomen_redstone.png");
    private static final ResourceLocation ABDOMEN_ILLUMERIN = new ResourceLocation(Fireflies.ID, "textures/entity/firefly_layer_abdomen_illumerin.png");

    public FireflyAbdomenLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    protected ResourceLocation getEntityTexture(T fireflyEntity) {
        return fireflyEntity.isRedstoneCoated(true) ? ABDOMEN_REDSTONE : fireflyEntity.hasIllumerin(true) ? ABDOMEN_ILLUMERIN : ABDOMEN;
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, T fireflyEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!fireflyEntity.isInvisible() && fireflyEntity.glowAlpha >= 0) {
            this.getEntityModel().render(matrixStack, buffer.getBuffer(RenderType.getEyes(this.getEntityTexture(fireflyEntity))), 15728640, OverlayTexture.NO_OVERLAY,
                    fireflyEntity.glowAlpha, fireflyEntity.glowAlpha, fireflyEntity.glowAlpha, fireflyEntity.glowAlpha);
        }
    }
}