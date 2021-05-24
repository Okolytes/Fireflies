package fireflies.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import fireflies.Fireflies;
import fireflies.client.sound.FireflyFlightLoopSound;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Java classloading forces us to put calls to the Minecraft class in here I guess...
@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT)
public class DoClientStuff {
    public void playFireflyLoopSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundHandler().playOnNextTick(new FireflyFlightLoopSound(fireflyEntity));
    }

    public boolean isGamePaused() {
        return Minecraft.getInstance().isGamePaused();
    }

    public static FireflyEntity fireflyEntity;
    public static BlockRayTraceResult rayTraceResult;

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        if (fireflyEntity == null || rayTraceResult == null)
            return;

        IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IVertexBuilder builder = buffer.getBuffer(RenderType.LINES);

        Vector3d view = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        MatrixStack matrixStack = event.getMatrixStack();
        RenderSystem.lineWidth(2);

        matrixStack.push();
        matrixStack.translate(-view.x, -view.y, -view.z);
        Matrix4f matrix = matrixStack.getLast().getMatrix();

        // from
        builder.pos(matrix, (float) fireflyEntity.getPosX(), (float) fireflyEntity.getPosYEye(), (float) fireflyEntity.getPosZ())
                .color(1f, 0f, 0f, 1f)
                .endVertex();

        // to
        builder.pos(matrix, (float) rayTraceResult.getHitVec().x, (float) rayTraceResult.getHitVec().y, (float) rayTraceResult.getHitVec().z)
                .color(1f, 0f, 0f, 1f)
                .endVertex();

        matrixStack.pop();
        RenderSystem.disableDepthTest();
        buffer.finish(RenderType.LINES);
    }
}
