package fireflies.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.client.model.FireflyAbdomenLayer;
import fireflies.client.model.FireflyEyesLayer;
import fireflies.client.model.FireflyModel;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
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
    public ResourceLocation getEntityTexture(FireflyEntity fireflyEntity) {
        return fireflyEntity.isRedstoneCoated(true) ? REDSTONE : DEFAULT;
    }

    protected void applyRotations(FireflyEntity fireflyEntity, MatrixStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        // Bob firefly up and down
        matrixStack.translate(0.0D, (MathHelper.cos(ageInTicks * 0.1F) * 0.05F), 0.0D);

        super.applyRotations(fireflyEntity, matrixStack, ageInTicks, rotationYaw, partialTicks);
    }
}
