package fireflies;

import fireflies.block.IllumerinBlock;
import fireflies.client.ClientStuff;
import fireflies.client.FireflyAbdomenAnimationLoader;
import fireflies.client.FireflyAbdomenAnimationManager;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(Fireflies.ID)
public class Fireflies {
    public static final String ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(ID);
    private boolean firstSplashTextChange = true;

    public Fireflies() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Registry.register(modEventBus);
        modEventBus.addGenericListener(Item.class, Registry::registerSpawnEggs);

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::createEntityAttributes);
        modEventBus.addListener(this::registerParticleFactories);

        MinecraftForge.EVENT_BUS.addListener(this::splashTextEasterEgg);
        MinecraftForge.EVENT_BUS.addListener(IllumerinBlock::stopMobSpawning);
        MinecraftForge.EVENT_BUS.addListener(FireflyAbdomenAnimationManager::syncFireflies);
        MinecraftForge.EVENT_BUS.addListener(FireflyAbdomenAnimationManager::debugSetAnimation);

        FireflyAbdomenAnimationLoader.addFireflyAnimationsReloadListener();
    }

    private void clientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
    }

    private void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        // Called only on the client.
        final ParticleManager pm = Minecraft.getInstance().particles;
        pm.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        pm.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        pm.registerFactory(Registry.FIREFLY_ABDOMEN_ILLUMERIN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenIllumerinParticleFactory::new);
    }

    private void splashTextEasterEgg(GuiOpenEvent event) {
        if (this.firstSplashTextChange && event.getGui() instanceof MainMenuScreen) {
            // We don't want to have a chance of changing the splash text every time we open the main menu, just once when the game is opened
            this.firstSplashTextChange = false;
            final boolean sus = ClientStuff.getMyUsername().equals("0rbic"); // don't worry about this
            // 1 in 100 chance
            if (Math.random() >= 0.99f || sus) {
                final String[] splashes = {
                        "There is an imposter among us.",
                        "You would not believe your eyes...",
                        "If ten million fireflies...",
                        "I'd get a thousand hugs, from ten thousand lightning bugs",
                        "OK."
                };
                // Choose a random splash text
                final String newSplashText = sus ? splashes[0] : splashes[new Random().nextInt(splashes.length)];

                try {
                    ObfuscationReflectionHelper.setPrivateValue(MainMenuScreen.class, (MainMenuScreen) event.getGui(), newSplashText, "field_73975_c");
                } catch (ObfuscationReflectionHelper.UnableToAccessFieldException | ObfuscationReflectionHelper.UnableToFindFieldException e) {
                    Fireflies.LOGGER.error("Failed miserably at setting the splash text.", e);
                }
            }
        }
    }
}
