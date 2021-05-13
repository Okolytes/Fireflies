package fireflies.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyRenderer extends MobRenderer<FireflyEntity, FireflyModel<FireflyEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Fireflies.MOD_ID, "textures/entity/fireflyoff.png");

    public FireflyRenderer(EntityRendererManager manager) {
        super(manager, new FireflyModel<>(), 0.3f);
    }

    @Override
    public ResourceLocation getEntityTexture(FireflyEntity entity) {
        return TEXTURE;
    }

    protected void applyRotations(FireflyEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        // Bob up and down
        matrixStackIn.translate(0.0D, (MathHelper.cos(ageInTicks * 0.1F) * 0.05F), 0.0D);

        super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
    }
}
