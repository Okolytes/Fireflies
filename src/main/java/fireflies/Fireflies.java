package fireflies;

import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import fireflies.client.render.GlassJarRenderer;
import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyMasterPotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MOD_ID)
public class Fireflies {
    public static final String MOD_ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Fireflies() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Registry.register(bus);
        bus.register(this);
    }

    @SubscribeEvent
    public void createEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
    }

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Splash, lingering, arrows etc get registered automatically.
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Potions.AWKWARD, new ItemStack(Registry.ILLUMERIN.get()), Registry.FIREFLY_MASTER.get()));
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Registry.FIREFLY_MASTER.get(), new ItemStack(Items.REDSTONE), Registry.LONG_FIREFLY_MASTER.get()));
            BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterBrewingRecipe(Registry.FIREFLY_MASTER.get(), new ItemStack(Items.GLOWSTONE_DUST), Registry.STRONG_FIREFLY_MASTER.get()));
        });
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> RenderTypeLookup.setRenderLayer(Registry.GLASS_JAR.get(), RenderType.getCutout()));
        ClientRegistry.bindTileEntityRenderer(Registry.GLASS_JAR_TILE_ENTITY.get(), GlassJarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerParticles(final ParticleFactoryRegisterEvent event) {
        // Called only on the client.
        ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get(), FireflyDustParticle.DustRedstoneParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get(), FireflyAbdomenParticle.AbdomenRedstoneParticleFactory::new);
    }
}
