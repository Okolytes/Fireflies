package fireflies.init;

import fireflies.Fireflies;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import fireflies.client.render.GlassJarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
        ClientRegistry.bindTileEntityRenderer(Registry.GLASS_JAR_TILE_ENTITY.get(), GlassJarRenderer::new);
        event.enqueueWork(() -> {
            RenderTypeLookup.setRenderLayer(Registry.GLASS_JAR.get(), RenderType.getCutout());
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerParticles(ParticleFactoryRegisterEvent event) {
        // Register all of our particles
        ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get(), FireflyDustParticle.DustRedstoneParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get(), FireflyAbdomenParticle.AbdomenRedstoneParticleFactory::new);
    }
}
