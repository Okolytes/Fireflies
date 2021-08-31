package fireflies;

import fireflies.block.IllumerinBlock;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import fireflies.client.render.GlassJarRenderer;
import fireflies.entity.FireflyEntity;
import fireflies.entity.FireflyGlowSyncHandler;
import fireflies.item.GlassJarBlockItem;
import fireflies.misc.FireflyMasterPotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Potions;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(Fireflies.ID)
public class Fireflies {
    public static final String ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    private boolean changeSplashText = true;

    public Fireflies() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Registry.register(modEventBus);
        modEventBus.addGenericListener(Item.class, Registry::registerSpawnEggs);
        modEventBus.addGenericListener(Effect.class, Registry::registerEffects);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::createEntityAttributes);
        modEventBus.addListener(this::registerParticleFactories);
        modEventBus.addListener(GlassJarBlockItem::registerGlassJarColor);

        MinecraftForge.EVENT_BUS.addListener(this::splashTextEasterEgg);
        MinecraftForge.EVENT_BUS.addListener(IllumerinBlock::stopMobSpawning);
        MinecraftForge.EVENT_BUS.addListener(FireflyMasterPotion::hideFireflyMasterToolTip);
        MinecraftForge.EVENT_BUS.addListener(FireflyGlowSyncHandler::animateFireflies);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Splash, lingering, arrows etcetera get registered automatically by minecraft.
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Potions.AWKWARD, new ItemStack(Registry.ILLUMERIN.get()), Registry.FIREFLY_MASTER.get()));
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Potions.AWKWARD, new ItemStack(Registry.SOUL_ILLUMERIN.get()), Registry.FIREFLY_MASTER.get()));
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Registry.FIREFLY_MASTER.get(), new ItemStack(Items.REDSTONE), Registry.LONG_FIREFLY_MASTER.get()));
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Registry.FIREFLY_MASTER.get(), new ItemStack(Items.GLOWSTONE_DUST), Registry.STRONG_FIREFLY_MASTER.get()));
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RenderTypeLookup.setRenderLayer(Registry.GLASS_JAR.get(), RenderType.getCutout());
            GlassJarBlockItem.registerItemModelProperty();
        });
        ClientRegistry.bindTileEntityRenderer(Registry.GLASS_JAR_TILE_ENTITY.get(), GlassJarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    private void createEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
    }

    private void registerParticleFactories(final ParticleFactoryRegisterEvent event) {
        // Called only on the client.
        final ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get(), FireflyDustParticle.DustRedstoneParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get(), FireflyAbdomenParticle.AbdomenRedstoneParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_ILLUMERIN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenIllumerinParticleFactory::new);
    }

    private void splashTextEasterEgg(GuiOpenEvent event) {
        if (this.changeSplashText && event.getGui() instanceof MainMenuScreen) {
            // We don't want to have a chance of changing the splash text every time we open the main menu, just once when the game is opened
            this.changeSplashText = false;
            // 1 in 100 chance
            if (Math.random() > 0.99f) {
                final String[] splashes = {
                        "You would not believe your eyes...",
                        "If ten million fireflies...",
                        "I'd get a thousand hugs, from ten thousand lightning bugs",
                };
                // Choose a random splash text
                final String newSplashText = splashes[new Random().nextInt(splashes.length)];

                try {
                    ObfuscationReflectionHelper.setPrivateValue(MainMenuScreen.class, (MainMenuScreen) event.getGui(), newSplashText, "field_73975_c");
                } catch (ObfuscationReflectionHelper.UnableToAccessFieldException | ObfuscationReflectionHelper.UnableToFindFieldException e) {
                    Fireflies.LOGGER.error("Failed miserably at setting the splash text.", e);
                }
            }
        }
    }
}
