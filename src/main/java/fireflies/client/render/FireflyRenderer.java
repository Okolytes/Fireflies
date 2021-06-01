package fireflies.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.Fireflies;
import fireflies.client.model.FireflyAbdomenLayer;
import fireflies.client.model.FireflyEyesLayer;
import fireflies.client.model.FireflyModel;
import fireflies.entity.firefly.FireflyEntity;
import fireflies.setup.ClientSetup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
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
    private static final ResourceLocation DEFAULT = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly.png");
    private static final ResourceLocation REDSTONE = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_redstone.png");

    public FireflyRenderer(EntityRendererManager manager) {
        super(manager, new FireflyModel<>(), 0.3f);
        this.addLayer(new FireflyEyesLayer<>(this));
        this.addLayer(new FireflyAbdomenLayer<>(this));
    }

    @Override
    public void render(FireflyEntity fireflyEntity, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        // Render the abdomen light sprite.
        super.render(fireflyEntity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        if (fireflyEntity.isInvisible() || !fireflyEntity.isAlive())
            return;

        matrixStackIn.push();

        // Position at the abdomen
        float x = (float) -fireflyEntity.getLookVec().normalize().getX() * (fireflyEntity.isChild() ? 0.075f : 0.175f);
        float y = this.entityModel.abdomen.rotateAngleX + fireflyEntity.getEyeHeight() + (fireflyEntity.isChild() ? 0.25f : 0.275f);
        float z = (float) -fireflyEntity.getLookVec().normalize().getZ() * (fireflyEntity.isChild() ? 0.075f : 0.175f);
        matrixStackIn.translate(x, y, z);

        // Rotate to face camera
        Vector3f[] vector3f1 = new Vector3f[] { new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F) };
        float scale = fireflyEntity.isChild() ? 0.225f : 0.45f;
        for (int i = 0; i < 4; ++i) {
            vector3f1[i].transform(this.renderManager.info.getRotation());
            vector3f1[i].mul(scale);
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE)
                .apply(fireflyEntity.isRedstoneCoated(true) ? ClientSetup.REDSTONE_ABDOMEN_LIGHT : ClientSetup.ABDOMEN_LIGHT);

        IVertexBuilder buffer = bufferIn.getBuffer(Atlases.getItemEntityTranslucentCullType());
        MatrixStack.Entry entry = matrixStackIn.getLast();
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();
        int light = 240; // Fullbright
        buffer.pos(entry.getMatrix(), vector3f1[0].getX(), vector3f1[0].getY(), vector3f1[0].getZ()).color(1f, 1f, 1f, fireflyEntity.glowAlpha).tex(maxU, maxV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vector3f1[1].getX(), vector3f1[1].getY(), vector3f1[1].getZ()).color(1f, 1f, 1f, fireflyEntity.glowAlpha).tex(maxU, minV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vector3f1[2].getX(), vector3f1[2].getY(), vector3f1[2].getZ()).color(1f, 1f, 1f, fireflyEntity.glowAlpha).tex(minU, minV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();
        buffer.pos(entry.getMatrix(), vector3f1[3].getX(), vector3f1[3].getY(), vector3f1[3].getZ()).color(1f, 1f, 1f, fireflyEntity.glowAlpha).tex(minU, maxV).overlay(0, 10).lightmap(light).normal(entry.getNormal(), 0.0F, 1.0F, 0.0F).endVertex();

        matrixStackIn.pop();
    }

    @Override
    public ResourceLocation getEntityTexture(FireflyEntity fireflyEntity) {
        return fireflyEntity.isRedstoneCoated(true) ? REDSTONE : DEFAULT;
    }

    protected void applyRotations(FireflyEntity fireflyEntity, MatrixStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        // Bob firefly up and down
        matrixStack.translate(0.0D, (MathHelper.cos(ageInTicks * 0.1F) * 0.05F), 0.0D);

        super.applyRotations(fireflyEntity, matrixStack, ageInTicks, rotationYaw, partialTicks);
    }
}
