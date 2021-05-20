package fireflies.setup;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.Fireflies;
import fireflies.client.particle.FireflyAbdomenParticleData;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FirefliesRegistration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.MOD_ID);

    private static final EntityType<FireflyEntity> FIREFLY_BUILDER = EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE)
            .size(0.5f, 0.5f)
            .trackingRange(10)
            .build("firefly");
    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> FIREFLY_BUILDER);

    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", () -> new BasicParticleType(false));
    public static final RegistryObject<ParticleType<FireflyAbdomenParticleData>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyAbdomenParticleData::get);

    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = SOUNDS.register("firefly_flight_loop",
            () -> new SoundEvent(new ResourceLocation(Fireflies.MOD_ID, "firefly_flight_loop")));

    public static void init() {
        IEventBus iEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(iEventBus);
        ITEMS.register(iEventBus);
        ENTITIES.register(iEventBus);
        PARTICLES.register(iEventBus);
        SOUNDS.register(iEventBus);
    }

    @SubscribeEvent
    public static void registerSpawnEggs(final RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new SpawnEggItem(FIREFLY_BUILDER, 0xEDE73A, 0x4D0F0F, new Item.Properties().group(ItemGroup.MISC))
                        .setRegistryName(Fireflies.MOD_ID, "firefly_spawn_egg")
        );
    }
}
