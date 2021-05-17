package fireflies.setup;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.Fireflies;
import fireflies.item.FireflySpawnEgg;
import fireflies.client.particle.FireflyAbdomenParticleData;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FirefliesRegistration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MOD_ID);

    public static final RegistryObject<Item> FIREFLY_SPAWN_EGG = ITEMS.register("firefly_spawn_egg", FireflySpawnEgg::new);

    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE)
            .size(.5f, .5f)
            .trackingRange(10)
            .build("firefly"));

    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", () -> new BasicParticleType(false));
    public static final RegistryObject<ParticleType<FireflyAbdomenParticleData>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyAbdomenParticleData::get);

    public static void init() {
        IEventBus iEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(iEventBus);
        ITEMS.register(iEventBus);
        ENTITIES.register(iEventBus);
        PARTICLES.register(iEventBus);
    }
}
