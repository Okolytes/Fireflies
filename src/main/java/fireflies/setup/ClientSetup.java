package fireflies.setup;

import fireflies.Fireflies;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static ResourceLocation ABDOMEN_LIGHT = new ResourceLocation(Fireflies.MOD_ID, "entity/firefly_abdomen_light");
    public static ResourceLocation REDSTONE_ABDOMEN_LIGHT = new ResourceLocation(Fireflies.MOD_ID, "entity/firefly_abdomen_redstone_light");

    public static void init(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerParticles(ParticleFactoryRegisterEvent event) {
        // Register all of our particles
        ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get(), FireflyDustParticle.DustRedstoneParticleFactory::new);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        // Register all of our sprites
        if (event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
            event.addSprite(ABDOMEN_LIGHT);
            event.addSprite(REDSTONE_ABDOMEN_LIGHT);
        }
    }
}
