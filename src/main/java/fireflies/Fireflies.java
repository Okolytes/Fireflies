package fireflies;

import fireflies.client.AbdomenAnimationLoader;
import fireflies.client.render.FireflyRenderer;
import fireflies.entity.FireflyEntity;
import net.minecraft.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MODID)
public class Fireflies {
    public static final String MODID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Fireflies() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Registry.register(modEventBus);
        modEventBus.addGenericListener(Item.class, Registry::registerSpawnEggs);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::createEntityAttributes);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AbdomenAnimationLoader::addFireflyAnimationsReloadListener);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
    }

}
