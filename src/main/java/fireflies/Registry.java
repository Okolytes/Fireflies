package fireflies;

import fireflies.block.*;
import fireflies.entity.FireflyEntity;
import fireflies.item.GlassJarBlockItem;
import fireflies.misc.FireflyMasterPotion;
import fireflies.misc.FireflyParticleData;
import fireflies.misc.FireflySpawnEgg;
import fireflies.misc.GlassJarFluid;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.*;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Fireflies.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.ID);
    private static final DeferredRegister<Potion> POTION_TYPES = DeferredRegister.create(ForgeRegistries.POTION_TYPES, Fireflies.ID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Fireflies.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.ID);

    // Blocks
    public static final RegistryObject<Block> ILLUMERIN_BLOCK = BLOCKS.register("illumerin_block", IllumerinBlock::new);
    public static final RegistryObject<Block> ILLUMERIN_LAMP = BLOCKS.register("illumerin_lamp", IllumerinLamp::new);
    public static final RegistryObject<Block> SOUL_ILLUMERIN_BLOCK = BLOCKS.register("soul_illumerin_block", SoulIllumerinBlock::new);
    public static final RegistryObject<Block> GLASS_JAR = BLOCKS.register("glass_jar", GlassJarBlock::new);

    // Fluids
    public static final RegistryObject<Fluid> GENERIC_POTION_FLUID = FLUIDS.register("potion_fluid", () -> new GlassJarFluid("potion_fluid", GlassJarFluid.BOTTLE_VOLUME));
    public static final RegistryObject<Fluid> MILK_FLUID = FLUIDS.register("milk_fluid", () -> new GlassJarFluid("potion_fluid", FluidAttributes.BUCKET_VOLUME, 0xFFF7F7F7));
    public static final RegistryObject<Fluid> HONEY_FLUID = FLUIDS.register("honey_fluid", () -> new GlassJarFluid("honey_fluid", GlassJarFluid.BOTTLE_VOLUME, 0xFFFDD330));
    public static final RegistryObject<Fluid> DRAGON_BREATH_FLUID = FLUIDS.register("dragon_breath_fluid", () -> new GlassJarFluid("dragon_breath_fluid", GlassJarFluid.BOTTLE_VOLUME, (byte)2));
    public static final RegistryObject<Fluid> GENERIC_SOUP_FLUID = FLUIDS.register("soup_fluid", () -> new GlassJarFluid("potion_fluid", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909));
    public static final RegistryObject<Fluid> RABBIT_STEW_FLUID = FLUIDS.register("rabbit_stew_fluid", () -> new GlassJarFluid("potion_fluid", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909));
    public static final RegistryObject<Fluid> SUS_STEW_FLUID = FLUIDS.register("sus_stew_fluid", () -> new GlassJarFluid("potion_fluid", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909)); // when the imposter is

    // Items
    public static final RegistryObject<Item> ILLUMERIN = ITEMS.register("illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    public static final RegistryObject<Item> SOUL_ILLUMERIN = ITEMS.register("soul_illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    public static final RegistryObject<Item> ILLUMERIN_BLOCKITEM = ITEMS.register("illumerin_block", () -> new BlockItem(ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));
    public static final RegistryObject<Item> ILLUMERIN_LAMP_BLOCKITEM = ITEMS.register("illumerin_lamp", () -> new BlockItem(ILLUMERIN_LAMP.get(), new Item.Properties().group(ItemGroup.REDSTONE)));
    public static final RegistryObject<Item> SOUL_ILLUMERIN_BLOCKITEM = ITEMS.register("soul_illumerin_block", () -> new BlockItem(SOUL_ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));
    public static final RegistryObject<Item> GLASS_JAR_BLOCKITEM = ITEMS.register("glass_jar", GlassJarBlockItem::new);

    // Potions & Effects
    private static final Effect HIDDEN_FIREFLY_MASTER_EFFECT = new FireflyMasterPotion.HiddenFireflyMasterEffect().setRegistryName("firefly_master_effect");

    public static final RegistryObject<Potion> FIREFLY_MASTER = POTION_TYPES.register("firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 320), new EffectInstance(Effects.POISON, 320), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 320, 69)));
    public static final RegistryObject<Potion> LONG_FIREFLY_MASTER = POTION_TYPES.register("long_firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 640), new EffectInstance(Effects.POISON, 640), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 640, 69)));
    public static final RegistryObject<Potion> STRONG_FIREFLY_MASTER = POTION_TYPES.register("strong_firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 200, 1), new EffectInstance(Effects.POISON, 200, 1), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 200, 69)));

    // Tile Entities
    @SuppressWarnings("ConstantConditions") // It doesn't like me passing null to build(), but that's what every one else is doing /shrug
    public static final RegistryObject<TileEntityType<GlassJarTile>> GLASS_JAR_TILE_ENTITY = TILE_ENTITIES.register("glass_jar", () -> TileEntityType.Builder.create(GlassJarTile::new, GLASS_JAR.get()).build(null));

    // Entities / Spawn Eggs
    private static final EntityType<FireflyEntity> FIREFLY_ENTITY_TYPE = EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE).size(0.5f, 0.5f).trackingRange(10).build("firefly");
    //private static final EntityType<SoulFireflyEntity> SOUL_FIREFLY_ENTITY_TYPE = EntityType.Builder.create(SoulFireflyEntity::new, EntityClassification.CREATURE).size(0.5f, 0.5f).trackingRange(10).build("soul_firefly");
    private static final Item FIREFLY_SPAWN_EGG = new FireflySpawnEgg(FIREFLY_ENTITY_TYPE, 0x5B1313, 0xF7DD36, false).setRegistryName(Fireflies.ID, "firefly_spawn_egg");
    //private static final Item SOUL_FIREFLY_SPAWN_EGG = new FireflySpawnEgg(SOUL_FIREFLY_ENTITY_TYPE, 0x5B1313, 0xF7DD36, true).setRegistryName(Fireflies.ID, "soul_firefly_spawn_egg");
    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> FIREFLY_ENTITY_TYPE);
    //public static final RegistryObject<EntityType<SoulFireflyEntity>> SOUL_FIREFLY = ENTITIES.register("soul_firefly", () -> SOUL_FIREFLY_ENTITY_TYPE);

    // Particles
    public static final RegistryObject<ParticleType<FireflyParticleData.Dust>> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", FireflyParticleData.Dust::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.DustRedstone>> FIREFLY_DUST_REDSTONE_PARTICLE = PARTICLES.register("firefly_dust_redstone_particle", FireflyParticleData.DustRedstone::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.Abdomen>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyParticleData.Abdomen::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.AbdomenRedstone>> FIREFLY_ABDOMEN_REDSTONE_PARTICLE = PARTICLES.register("firefly_abdomen_redstone_particle", FireflyParticleData.AbdomenRedstone::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.AbdomenIllumerin>> FIREFLY_ABDOMEN_ILLUMERIN_PARTICLE = PARTICLES.register("firefly_abdomen_illumerin_particle", FireflyParticleData.AbdomenIllumerin::get);

    // Sounds
    public static final RegistryObject<SoundEvent> FIREFLY_HURT = registerSoundEvent("firefly_hurt");
    public static final RegistryObject<SoundEvent> FIREFLY_DEATH = registerSoundEvent("firefly_death");
    // public static final RegistryObject<SoundEvent> FIREFLY_GLOW = registerSoundEvent("firefly_glow"); // unused for now
    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = registerSoundEvent("firefly_flight_loop");
    public static final RegistryObject<SoundEvent> FIREFLY_APPLY_REDSTONE = registerSoundEvent("firefly_apply_redstone");
    public static final RegistryObject<SoundEvent> GLASS_JAR_OPEN = registerSoundEvent("glass_jar_open");
    public static final RegistryObject<SoundEvent> GLASS_JAR_CLOSE = registerSoundEvent("glass_jar_close");

    // Stats
    public static final ResourceLocation FILL_GLASS_JAR_STAT = registerCustomStat("fill_glass_jar");
    public static final ResourceLocation USE_GLASS_JAR_STAT = registerCustomStat("use_glass_jar");

    public static void register(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        POTION_TYPES.register(bus);
        FLUIDS.register(bus);
        TILE_ENTITIES.register(bus);
        ENTITIES.register(bus);
        PARTICLES.register(bus);
        SOUNDS.register(bus);
    }

    public static void registerSpawnEggs(final RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                FIREFLY_SPAWN_EGG//, SOUL_FIREFLY_SPAWN_EGG
        );

        registerSpawnEggDispenserBehaviour(FIREFLY_SPAWN_EGG);
        //registerSpawnEggDispenserBehaviour(SOUL_FIREFLY_SPAWN_EGG);
    }

    private static void registerSpawnEggDispenserBehaviour(IItemProvider egg){
        // Registers the spawn egg's dispenser behaviour.
        // Taken from IDispenseItemBehavior#init L192
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

    public static void registerEffects(final RegistryEvent.Register<Effect> event) {
        event.getRegistry().register(HIDDEN_FIREFLY_MASTER_EFFECT);
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(Fireflies.ID, name)));
    }

    private static ResourceLocation registerCustomStat(String name) {
        // https://forums.minecraftforge.net/topic/72454-custom-stats-1143/?do=findComment&comment=392617
        final ResourceLocation rsc = new ResourceLocation(Fireflies.ID, name);
        net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.CUSTOM_STAT, name, rsc);
        Stats.CUSTOM.get(rsc, IStatFormatter.DEFAULT);
        return rsc;
    }
}
