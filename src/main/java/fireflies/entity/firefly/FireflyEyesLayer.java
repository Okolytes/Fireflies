package fireflies.entity.firefly;

import fireflies.Fireflies;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.AbstractEyesLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyEyesLayer<T extends FireflyEntity, M extends FireflyModel<T>> extends AbstractEyesLayer<T, M> {
    private static final RenderType RENDER_TYPE = RenderType.getEyes(new ResourceLocation(Fireflies.MOD_ID, "textures/entity/firefly_layer_eyes.png"));

    public FireflyEyesLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    public RenderType getRenderType() {
        return RENDER_TYPE;
    }
}