package fireflies;

import fireflies.block.IllumerinBlock;
import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import fireflies.misc.FireflySpawnEgg;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.particles.ParticleType;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Direction;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.MODID);

    // Blocks
    public static final RegistryObject<Block> ILLUMERIN_BLOCK = BLOCKS.register("illumerin_block", IllumerinBlock::new);

    // Items
    public static final RegistryObject<Item> ILLUMERIN = ITEMS.register("illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    public static final RegistryObject<Item> SOUL_ILLUMERIN = ITEMS.register("soul_illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));

    // Entities / Spawn Eggs
    private static final EntityType<FireflyEntity> FIREFLY_ENTITY_TYPE = EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE).size(0.5f, 0.5f).trackingRange(10).build("firefly");
    //private static final EntityType<SoulFireflyEntity> SOUL_FIREFLY_ENTITY_TYPE = EntityType.Builder.create(SoulFireflyEntity::new, EntityClassification.CREATURE).size(0.5f, 0.5f).trackingRange(10).build("soul_firefly");
    private static final Item FIREFLY_SPAWN_EGG = new FireflySpawnEgg(FIREFLY_ENTITY_TYPE, 0x5B1313, 0xF7DD36, false).setRegistryName(Fireflies.MODID, "firefly_spawn_egg");
    //private static final Item SOUL_FIREFLY_SPAWN_EGG = new FireflySpawnEgg(SOUL_FIREFLY_ENTITY_TYPE, 0x5B1313, 0xF7DD36, true).setRegistryName(Fireflies.ID, "soul_firefly_spawn_egg");
    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> FIREFLY_ENTITY_TYPE);
    //public static final RegistryObject<EntityType<SoulFireflyEntity>> SOUL_FIREFLY = ENTITIES.register("soul_firefly", () -> SOUL_FIREFLY_ENTITY_TYPE);

    // Particles
    public static final RegistryObject<ParticleType<FireflyParticleData.Dust>> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", FireflyParticleData.Dust::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.Abdomen>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyParticleData.Abdomen::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.AbdomenIllumerin>> FIREFLY_ABDOMEN_ILLUMERIN_PARTICLE = PARTICLES.register("firefly_abdomen_illumerin_particle", FireflyParticleData.AbdomenIllumerin::get);

    // Sounds
    public static final RegistryObject<SoundEvent> FIREFLY_HURT = registerSoundEvent("firefly_hurt");
    public static final RegistryObject<SoundEvent> FIREFLY_DEATH = registerSoundEvent("firefly_death");
    // public static final RegistryObject<SoundEvent> FIREFLY_GLOW = registerSoundEvent("firefly_glow"); // unused for now
    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = registerSoundEvent("firefly_flight_loop");

    // Stats
    // todo some firefly related stat
    //public static final ResourceLocation FILL_GLASS_JAR_STAT = registerCustomStat("fill_glass_jar");

    public static void register(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        PARTICLES.register(bus);
        SOUNDS.register(bus);
    }

    public static void registerSpawnEggs(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                FIREFLY_SPAWN_EGG//, SOUL_FIREFLY_SPAWN_EGG
        );

        registerSpawnEggDispenserBehaviour(FIREFLY_SPAWN_EGG);
        //registerSpawnEggDispenserBehaviour(SOUL_FIREFLY_SPAWN_EGG);
    }

    /**
     * Registers the spawn egg's dispenser behaviour.
     * Taken from IDispenseItemBehavior#init L192
     */
    private static void registerSpawnEggDispenserBehaviour(IItemProvider egg) {
        DispenserBlock.registerDispenseBehavior(egg, new DefaultDispenseItemBehavior() {
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                final Direction direction = source.getBlockState().get(DispenserBlock.FACING);
                final EntityType<?> entityType = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
                entityType.spawn(source.getWorld(), stack, null, source.getBlockPos().offset(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
                stack.shrink(1);
                return stack;
            }
        });
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(Fireflies.MODID, name)));
    }

    private static ResourceLocation registerCustomStat(String name) {
        // https://forums.minecraftforge.net/topic/72454-custom-stats-1143/?do=findComment&comment=392617
        final ResourceLocation rsc = new ResourceLocation(Fireflies.MODID, name);
        net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.CUSTOM_STAT, name, rsc);
        Stats.CUSTOM.get(rsc, IStatFormatter.DEFAULT);
        return rsc;
    }
}
