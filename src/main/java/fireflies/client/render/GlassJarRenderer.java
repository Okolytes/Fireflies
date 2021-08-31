package fireflies.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.Fireflies;
import fireflies.block.GlassJarTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public class GlassJarRenderer extends TileEntityRenderer<GlassJarTile> {

    public GlassJarRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    // https://youtu.be/F9UKNwlhhpo?t=111
    @Override
    public void render(GlassJarTile glassJar, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int light, int combinedOverlayIn) {
        if (glassJar.isRemoved() || glassJar.getTank().isEmpty() || glassJar.getWorld() == null) return;
        final FluidStack fluidStack = glassJar.getTank().getFluid();
        final FluidAttributes fluidAttributes = fluidStack.getFluid().getAttributes();
        final boolean isJarFluid = fluidStack.getFluid().getRegistryName() != null && fluidStack.getFluid().getRegistryName().getNamespace().equals(Fireflies.ID);
        final TextureAtlasSprite sideSprite = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidAttributes.getStillTexture(fluidStack));
        final TextureAtlasSprite topSprite = isJarFluid ? Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidAttributes.getFlowingTexture(fluidStack)) : sideSprite;
        if (sideSprite == null || topSprite == null) return;

        matrixStackIn.push();
        final Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();
        final IVertexBuilder bufferSide = bufferIn.getBuffer(RenderType.getText(sideSprite.getAtlasTexture().getTextureLocation()));
        final IVertexBuilder bufferTop = bufferIn.getBuffer(RenderType.getText(topSprite.getAtlasTexture().getTextureLocation()));

        if (glassJar.cachedOpen && !glassJar.cachedAttached) {
            final float x = 0.0625f;
            final float z = 0.1875f;
            switch (glassJar.cachedDirection) {
                case NORTH:
                    matrixStackIn.translate(x, 0, z);
                    break;
                case SOUTH:
                    matrixStackIn.translate(-x, 0, -z);
                    break;
                case WEST:
                    matrixStackIn.translate(z, 0, -x);
                    break;
                case EAST:
                    matrixStackIn.translate(-z, 0, x);
                    break;
            }
        }
        matrixStackIn.translate(0.5f, 0.5f, 0.5f);

        final int color;
        // todo unfuck this
        if (glassJar.cachedFluidColor == -69) {
            // If it's a potion we'll use its color, if it's beetroot soup we'll use a custom colour
            if (fluidStack.getTag() == null || fluidStack.getFluid().isEquivalentTo(Fluids.WATER)) {
                color = fluidAttributes.getColor(glassJar.getWorld(), glassJar.getPos());
            } else {
                if (fluidStack.getTag().contains("Potion")) {
                    color = PotionUtils.getPotionColor(PotionUtils.getPotionTypeFromNBT(fluidStack.getTag()));
                } else {
                    if (fluidStack.getTag().getString("Soup").equals("minecraft:beetroot_soup")) {
                        color = 0xFFA4272C;
                    } else {
                        color = fluidAttributes.getColor();
                    }
                }
            }
            glassJar.cachedFluidColor = color;
        } else {
            color = glassJar.cachedFluidColor;
        }

        final float r = (color >> 16 & 0xff) / 255f;
        final float g = (color >> 8 & 0xff) / 255f;
        final float b = (color & 0xff) / 255f;
        float a = isJarFluid ? 0.9f : (color >> 24 & 0xff) / 255f;
        if (a <= 0) a = 0.75f;

        final float yScale = 0.69f * fluidStack.getAmount() / GlassJarTile.CAPACITY;
        final float offset = 0.01f;
        final float width = 0.6f;
        final float height = 1f;

        float minU = sideSprite.getInterpolatedU(0);
        float maxU = sideSprite.getInterpolatedU(8);
        float minV = sideSprite.getInterpolatedV(0);
        float maxV = sideSprite.getInterpolatedV(15.75f * yScale);

        // Sides
        for (int i = 0; i < 4; i++) {
            bufferSide.pos(matrix4f, -width / 2 + offset, -height / 2 + height * yScale, -width / 2 - offset).color(r, g, b, a).tex(minU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).endVertex();
            bufferSide.pos(matrix4f, width / 2 + offset, -height / 2 + height * yScale, -width / 2 - offset).color(r, g, b, a).tex(maxU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).endVertex();
            bufferSide.pos(matrix4f, width / 2 + offset, -height / 2, -width / 2 - offset).color(r, g, b, a).tex(maxU, maxV).lightmap(light).overlay(OverlayTexture.NO_OVERLAY).endVertex();
            bufferSide.pos(matrix4f, -width / 2 + offset, -height / 2, -width / 2 - offset).color(r, g, b, a).tex(minU, maxV).lightmap(light).overlay(OverlayTexture.NO_OVERLAY).endVertex();
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(90));
        }

        // Top
        minU = topSprite.getInterpolatedU(0);
        maxU = topSprite.getInterpolatedU(10);
        minV = topSprite.getInterpolatedV(0);
        maxV = topSprite.getInterpolatedV(10);
        matrixStackIn.scale(1.04f, 1f, 1.04f);

        bufferTop.pos(matrix4f, -width / 2, -height / 2 + yScale * height, -width / 2).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();
        bufferTop.pos(matrix4f, -width / 2, -height / 2 + yScale * height, width / 2).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        bufferTop.pos(matrix4f, width / 2, -height / 2 + yScale * height, width / 2).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        bufferTop.pos(matrix4f, width / 2, -height / 2 + yScale * height, -width / 2).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();

        matrixStackIn.pop();
    }

}
