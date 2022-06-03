package fireflies.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.Fireflies;
import fireflies.client.model.FireflyAbdomenLayer;
import fireflies.client.model.FireflyEyesLayer;
import fireflies.client.model.FireflyModel;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyRenderer extends MobRenderer<FireflyEntity, FireflyModel<FireflyEntity>> {
    public static final ResourceLocation ILLUMERIN_GOOP = new ResourceLocation(Fireflies.MODID, "entity/firefly_illumerin_goop");
    private static final ResourceLocation DEFAULT = new ResourceLocation(Fireflies.MODID, "textures/entity/firefly.png");

    public FireflyRenderer(EntityRendererManager manager) {
        super(manager, new FireflyModel<>(), 0.3f);
        this.addLayer(new FireflyEyesLayer<>(this));
        this.addLayer(new FireflyAbdomenLayer<>(this));
    }

    @Override
    public ResourceLocation getEntityTexture(FireflyEntity firefly) {
        return DEFAULT;
    }

    @Override
    public void render(FireflyEntity firefly, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        super.render(firefly, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        if (!firefly.hasIllumerin() || firefly.isInvisible() || !firefly.isAlive())
            return;

        matrixStackIn.push();

        matrixStackIn.translate(
                firefly.getWidth() * -0.1f * MathHelper.sin(firefly.renderYawOffset * (float) Math.PI / 180F),
                this.entityModel.abdomen.rotateAngleX + firefly.getEyeHeight() + 0.375f,
                -(firefly.getWidth() * -0.1f * MathHelper.cos(firefly.renderYawOffset * (float) Math.PI / 180F))
        );

        final Vector3f[] vertices = new Vector3f[] {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        final float scale = firefly.isChild() ? 0.125f : 0.25f;
        // Face the camera
        for (int i = 0; i < 4; ++i) {
            vertices[i].transform(Vector3f.YP.rotationDegrees(-this.renderManager.info.getYaw()));
            vertices[i].mul(scale);
        }

        final TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(ILLUMERIN_GOOP);
        final IVertexBuilder buffer = bufferIn.getBuffer(RenderType.getCutout());
        final MatrixStack.Entry entry = matrixStackIn.getLast();
        final float minU = sprite.getMinU();
        final float maxU = sprite.getMaxU();
        final float minV = sprite.getMinV();
        final float maxV = sprite.getMaxV();
        final int light = 240; // Fullbright
        buffer.pos(entry.getMatrix(), vertices[0].getX(), vertices[0].getY(), vertices[0].getZ()).color(1f, 1f, 1f, 1f).tex(maxU, maxV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vertices[1].getX(), vertices[1].getY(), vertices[1].getZ()).color(1f, 1f, 1f, 1f).tex(maxU, minV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vertices[2].getX(), vertices[2].getY(), vertices[2].getZ()).color(1f, 1f, 1f, 1f).tex(minU, minV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vertices[3].getX(), vertices[3].getY(), vertices[3].getZ()).color(1f, 1f, 1f, 1f).tex(minU, maxV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();

        matrixStackIn.pop();
    }

    @Override
    protected void applyRotations(FireflyEntity fireflyEntity, MatrixStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        // Bob firefly up and down
        matrixStack.translate(0.0D, MathHelper.cos(ageInTicks * 0.1F) * 0.05F, 0.0D);

        super.applyRotations(fireflyEntity, matrixStack, ageInTicks, rotationYaw, partialTicks);
    }
}
