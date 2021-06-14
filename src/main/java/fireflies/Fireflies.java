package fireflies;

import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.client.particle.FireflyDustParticle;
import fireflies.client.render.FireflyRenderer;
import fireflies.client.render.GlassJarRenderer;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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
        Registry.init(bus);
        bus.register(this);
    }

    @SubscribeEvent
    public void createEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
    }

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        BrewingRecipeRegistry.addRecipe(Ingredient.fromStacks(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Potions.AWKWARD)), Ingredient.fromItems(Registry.ILLUMERIN.get()), PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Registry.FIREFLY_MASTER.get()));
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(Registry.GLASS_JAR.get(), RenderType.getCutout());
        ClientRegistry.bindTileEntityRenderer(Registry.GLASS_JAR_TILE_ENTITY.get(), GlassJarRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(Registry.FIREFLY.get(), FireflyRenderer::new);
    }

    @SubscribeEvent
    public void registerParticles(final ParticleFactoryRegisterEvent event) {
        ParticleManager particleManager = Minecraft.getInstance().particles;
        particleManager.registerFactory(Registry.FIREFLY_DUST_PARTICLE.get(), FireflyDustParticle.DustParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get(), FireflyDustParticle.DustRedstoneParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_PARTICLE.get(), FireflyAbdomenParticle.AbdomenParticleFactory::new);
        particleManager.registerFactory(Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get(), FireflyAbdomenParticle.AbdomenRedstoneParticleFactory::new);
    }
}
