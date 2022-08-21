package fireflies.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import fireflies.Fireflies;
import fireflies.client.model.FireflyAbdomenLayer;
import fireflies.client.model.FireflyEyesLayer;
import fireflies.client.model.FireflyModel;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyRenderer extends MobRenderer<FireflyEntity, FireflyModel<FireflyEntity>> {
    public static final ResourceLocation ILLUMERIN_GOOP = new ResourceLocation(Fireflies.MOD_ID, "entity/firefly_illumerin_goop");
    private static final ResourceLocation DEFAULT = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly.png");

    public FireflyRenderer(EntityRendererProvider.Context context) {
        super(context, new FireflyModel<>(context.bakeLayer(FireflyModel.FIREFLY_LAYER)), 0.3f);
        this.addLayer(new FireflyEyesLayer<>(this));
        this.addLayer(new FireflyAbdomenLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FireflyEntity firefly) {
        return DEFAULT;
    }

    @Override
    public void render(FireflyEntity firefly, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(firefly, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        if (!firefly.hasIllumerin() || firefly.isInvisible() || !firefly.isAlive())
            return;

        matrixStackIn.pushPose();

        matrixStackIn.translate(
                firefly.getBbWidth() * -0.1f * Mth.sin(firefly.yBodyRot * (float) Math.PI / 180F),
                this.model.abdomen.xRot + firefly.getEyeHeight() + 0.375f,
                -(firefly.getBbWidth() * -0.1f * Mth.cos(firefly.yBodyRot * (float) Math.PI / 180F))
        );

        final Vector3f[] vertices = new Vector3f[] {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        final float scale = 0.25f;
        // Face the camera
        for (int i = 0; i < 4; ++i) {
            vertices[i].transform(Vector3f.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
            vertices[i].mul(scale);
        }

        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ILLUMERIN_GOOP);
        final VertexConsumer buffer = bufferIn.getBuffer(RenderType.cutout());
        final PoseStack.Pose entry = matrixStackIn.last();
        final float minU = sprite.getU0();
        final float maxU = sprite.getU1();
        final float minV = sprite.getV0();
        final float maxV = sprite.getV1();
        final int light = 240; // Fullbright
        buffer.vertex(entry.pose(), vertices[0].x(), vertices[0].y(), vertices[0].z()).color(1f, 1f, 1f, 1f).uv(maxU, maxV).overlayCoords(0, 10).uv2(light).normal(entry.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(entry.pose(), vertices[1].x(), vertices[1].y(), vertices[1].z()).color(1f, 1f, 1f, 1f).uv(maxU, minV).overlayCoords(0, 10).uv2(light).normal(entry.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(entry.pose(), vertices[2].x(), vertices[2].y(), vertices[2].z()).color(1f, 1f, 1f, 1f).uv(minU, minV).overlayCoords(0, 10).uv2(light).normal(entry.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(entry.pose(), vertices[3].x(), vertices[3].y(), vertices[3].z()).color(1f, 1f, 1f, 1f).uv(minU, maxV).overlayCoords(0, 10).uv2(light).normal(entry.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        matrixStackIn.popPose();
    }

    @Override
    protected void setupRotations(FireflyEntity fireflyEntity, PoseStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        // Bob firefly up and down
        matrixStack.translate(0.0D, Mth.cos(ageInTicks * 0.1F) * 0.05F, 0.0D);

        super.setupRotations(fireflyEntity, matrixStack, ageInTicks, rotationYaw, partialTicks);
    }
}
