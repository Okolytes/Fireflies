package fireflies.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.block.GlassJarBlock;
import fireflies.block.GlassJarTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public class GlassJarRenderer extends TileEntityRenderer<GlassJarTile> {
    @SuppressWarnings("ConstantConditions")
    private static final String BEETROOT_SOUP = Items.BEETROOT_SOUP.getRegistryName().toString();

    public GlassJarRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(GlassJarTile glassJar, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int light, int combinedOverlayIn) {
        if (glassJar.isRemoved() || glassJar.getTank().isEmpty() || glassJar.getWorld() == null) return;

        FluidStack fluidStack = glassJar.getTank().getFluid();
        FluidAttributes fluidAttributes = fluidStack.getFluid().getAttributes();
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidAttributes.getStillTexture(fluidStack));
        if (sprite == null) return;

        matrixStackIn.push();
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();
        IVertexBuilder buffer = bufferIn.getBuffer(RenderType.getText(sprite.getAtlasTexture().getTextureLocation()));

        if (glassJar.getBlockState().get(GlassJarBlock.OPEN)) matrixStackIn.translate(0.0625f, 0, 0.1875f);
        float x = 0.2f;
        float y = 0.7f * fluidStack.getAmount() / glassJar.getTank().getCapacity();
        float z = 0.8f;

        float minU = sprite.getMinU() + 0.00055f;
        float maxU = Math.min(minU + (sprite.getMaxU() - minU) * 0.575f, sprite.getMaxU());
        float minV = sprite.getMinV() + 0.00195f;
        float maxV = Math.min(minV + (sprite.getMaxV() - minV) * y, sprite.getMaxV());

        // If it's a potion we'll use its color, if it's beetroot soup we'll use a custom colour
        int color = fluidStack.getTag() == null ? fluidAttributes.getColor(glassJar.getWorld(), glassJar.getPos()) : fluidStack.getTag().contains("Potion") ? PotionUtils.getPotionColor(PotionUtils.getPotionTypeFromNBT(fluidStack.getTag())) : fluidStack.getTag().getString("Soup").equals(BEETROOT_SOUP) ? 0xFFA4272C : fluidAttributes.getColor();
        float r = (color >> 16 & 0xff) / 255f;
        float g = (color >> 8 & 0xff) / 255f;
        float b = (color & 0xff) / 255f;
        float a = (color >> 24 & 0xff) / 255f;
        if (a <= 0) a = 1f;

        // Top
        buffer.pos(matrix4f, x, y, x).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, y, z).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, y, z).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, y, x).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();

        // North
        float yOffset = 0.001f; // Because the model is JANK!
        buffer.pos(matrix4f, x, y, z).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, yOffset, z).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, yOffset, z).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, y, z).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();

        // South
        buffer.pos(matrix4f, z, y, x).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, yOffset, x).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, yOffset, x).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, y, x).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();

        // West
        buffer.pos(matrix4f, z, y, z).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, yOffset, z).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, yOffset, x).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, z, y, x).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();

        // East
        buffer.pos(matrix4f, x, y, x).color(r, g, b, a).tex(minU, minV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, yOffset, x).color(r, g, b, a).tex(minU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, yOffset, z).color(r, g, b, a).tex(maxU, maxV).lightmap(light).endVertex();
        buffer.pos(matrix4f, x, y, z).color(r, g, b, a).tex(maxU, minV).lightmap(light).endVertex();

        matrixStackIn.pop();
    }
}
