package fireflies.client;

import fireflies.Fireflies;
import fireflies.Registry;
import fireflies.client.model.FireflyModel;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.Random;

public final class ClientStuff {

    /**
     * Taken from {@link Level#updateSkyBrightness} because {@link Level#isDay} does not work when called from the client.
     *
     * @return Is it currently daytime in this dimension?
     */
    public static boolean isDayTime(Level level) {
        final double d0 = 1.0D - (double) (level.getRainLevel(1.0F) * 5.0F) / 16.0D;
        final double d1 = 1.0D - (double) (level.getThunderLevel(1.0F) * 5.0F) / 16.0D;
        final double d2 = 0.5D + 2.0D * Mth.clamp(Mth.cos(level.getTimeOfDay(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        final int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !level.dimensionType().hasFixedTime() && skylightSubtracted < 4;
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {

        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.register(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
            event.register(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        }

        @SubscribeEvent
        public static void onTextureStitch(TextureStitchEvent.Pre event) {
            if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
                event.addSprite(FireflyRenderer.ILLUMERIN_GOOP);
            }
        }

        @SubscribeEvent
        public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Registry.FIREFLY.get(), FireflyRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(FireflyModel.FIREFLY_LAYER, FireflyModel::createBodyLayer);
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private static boolean firstSplashTextChange = true;

        @SubscribeEvent
        public static void splashTextEasterEgg(ScreenEvent.Opening event) {
            if (firstSplashTextChange && event.getNewScreen() instanceof TitleScreen) {
                // We don't want to have a chance of changing the splash text every time we open the main menu, just once when the game is opened
                firstSplashTextChange = false;
                final boolean sus = Minecraft.getInstance().getUser().getName().equals("0rbic"); // don't worry about this
                // 1 in 100 chance
                if (Math.random() >= 0.99f || sus) {
                    final String[] splashes = {
                            "There is an imposter among us.",
                            "You would not believe your eyes...",
                            "If ten million fireflies...",
                            "I'd get a thousand hugs, from ten thousand lightning bugs",
                            "OK."
                    };
                    final String newSplashText = sus ? splashes[0] : splashes[new Random().nextInt(splashes.length)];

                    try {
                        ObfuscationReflectionHelper.setPrivateValue(TitleScreen.class, (TitleScreen) event.getNewScreen(), newSplashText, "splash"); // todo remmeber to update this
                    } catch (ObfuscationReflectionHelper.UnableToAccessFieldException | ObfuscationReflectionHelper.UnableToFindFieldException e) {
                        Fireflies.LOGGER.error("Failed miserably at setting the splash text.", e);
                    }
                }
            }
        }
    }
}
