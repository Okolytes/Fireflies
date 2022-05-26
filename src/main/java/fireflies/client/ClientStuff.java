package fireflies.client;

import fireflies.Fireflies;
import fireflies.Registry;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Random;

public final class ClientStuff {

    /**
     * Taken from {@link World#calculateInitialSkylight} because {@link World#isDaytime} does not work when called from the client.
     *
     * @return Is it currently daytime in this dimension?
     */
    public static boolean isDayTime(World world) {
        final double d0 = 1.0D - (double) (world.getRainStrength(1.0F) * 5.0F) / 16.0D;
        final double d1 = 1.0D - (double) (world.getThunderStrength(1.0F) * 5.0F) / 16.0D;
        final double d2 = 0.5D + 2.0D * MathHelper.clamp(MathHelper.cos(world.func_242415_f(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        final int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !world.getDimensionType().doesFixedTimeExist() && skylightSubtracted < 4;
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {

        @SubscribeEvent
        public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
            final ParticleManager pm = Minecraft.getInstance().particles;
            pm.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
            pm.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        }

        @SubscribeEvent
        public static void onTextureStitch(TextureStitchEvent.Pre event) {
            if (event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
                event.addSprite(FireflyRenderer.ILLUMERIN_GOOP);
            }
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private static boolean firstSplashTextChange = true;

        @SubscribeEvent
        public static void splashTextEasterEgg(GuiOpenEvent event) {
            if (firstSplashTextChange && event.getGui() instanceof MainMenuScreen) {
                // We don't want to have a chance of changing the splash text every time we open the main menu, just once when the game is opened
                firstSplashTextChange = false;
                final boolean sus = Minecraft.getInstance().getSession().getUsername().equals("0rbic"); // don't worry about this
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
                        ObfuscationReflectionHelper.setPrivateValue(MainMenuScreen.class, (MainMenuScreen) event.getGui(), newSplashText, "field_73975_c"); // todo remmeber to update this
                    } catch (ObfuscationReflectionHelper.UnableToAccessFieldException | ObfuscationReflectionHelper.UnableToFindFieldException e) {
                        Fireflies.LOGGER.error("Failed miserably at setting the splash text.", e);
                    }
                }
            }
        }
    }
}
