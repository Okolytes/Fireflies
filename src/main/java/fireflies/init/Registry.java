package fireflies.init;

import fireflies.Fireflies;
import fireflies.block.*;
import fireflies.entity.firefly.FireflyEntity;
import fireflies.fluid.GlassJarFluid;
import fireflies.misc.FireflyAbdomenParticleData;
import fireflies.misc.FireflyAbdomenRedstoneParticleData;
import fireflies.misc.FireflyMasterPotion;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.*;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MOD_ID);
    private static final DeferredRegister<Potion> POTION_TYPES = DeferredRegister.create(ForgeRegistries.POTION_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Fireflies.MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.MOD_ID);

    //region Blocks / BlockItems
    public static final RegistryObject<Block> ILLUMERIN_BLOCK = BLOCKS.register("illumerin_block", IllumerinBlock::new);
    public static final RegistryObject<Block> ILLUMERIN_LAMP = BLOCKS.register("illumerin_lamp", IllumerinLamp::new);
    public static final RegistryObject<Block> ECTO_ILLUMERIN_BLOCK = BLOCKS.register("ecto_illumerin_block", EctoIllumerinBlock::new);
    public static final RegistryObject<Block> GLASS_JAR = BLOCKS.register("glass_jar", GlassJarBlock::new);

    public static final RegistryObject<Item> ILLUMERIN_BLOCKITEM = ITEMS.register("illumerin_block", () -> new BlockItem(ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));
    public static final RegistryObject<Item> ILLUMERIN_LAMP_BLOCKITEM = ITEMS.register("illumerin_lamp", () -> new BlockItem(ILLUMERIN_LAMP.get(), new Item.Properties().group(ItemGroup.REDSTONE)));
    public static final RegistryObject<Item> ECTO_ILLUMERIN_BLOCKITEM = ITEMS.register("ecto_illumerin_block", () -> new BlockItem(ECTO_ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));
    public static final RegistryObject<Item> GLASS_JAR_BLOCKITEM = ITEMS.register("glass_jar", () -> new BlockItem(GLASS_JAR.get(), new Item.Properties().group(ItemGroup.REDSTONE)));
    //endregion Blocks / BlockItems

    //region Items
    public static final RegistryObject<Item> ILLUMERIN = ITEMS.register("illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    public static final RegistryObject<Item> ECTO_ILLUMERIN = ITEMS.register("ecto_illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    //endregion Items

    //region Potions & Effects
    private static final Effect HIDDEN_FIREFLY_MASTER_EFFECT = new FireflyMasterPotion.HiddenFireflyMasterEffect().setRegistryName("firefly_master_effect");

    public static final RegistryObject<Potion> FIREFLY_MASTER = POTION_TYPES.register("firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 320), new EffectInstance(Effects.POISON, 320), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 320, 69)));
    public static final RegistryObject<Potion> LONG_FIREFLY_MASTER = POTION_TYPES.register("long_firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 640), new EffectInstance(Effects.POISON, 640), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 320, 69)));
    public static final RegistryObject<Potion> STRONG_FIREFLY_MASTER = POTION_TYPES.register("strong_firefly_master", () -> new FireflyMasterPotion(new EffectInstance(Effects.GLOWING, 200, 1), new EffectInstance(Effects.POISON, 200, 1), new EffectInstance(HIDDEN_FIREFLY_MASTER_EFFECT, 320, 69)));
    //endregion Potions & Effects

    //region Fluids
    public static final RegistryObject<Fluid> GENERIC_POTION_FLUID = FLUIDS.register("potion_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME));
    public static final RegistryObject<Fluid> MILK_FLUID = FLUIDS.register("milk_fluid", () -> new GlassJarFluid("block/water_still", FluidAttributes.BUCKET_VOLUME, 0xFFF7F7F7));
    public static final RegistryObject<Fluid> HONEY_FLUID = FLUIDS.register("honey_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME, 0xFFFDD330));
    public static final RegistryObject<Fluid> DRAGON_BREATH_FLUID = FLUIDS.register("dragon_breath_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME, 0xFFE49BC4, 2));
    public static final RegistryObject<Fluid> GENERIC_SOUP_FLUID = FLUIDS.register("soup_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909));
    public static final RegistryObject<Fluid> RABBIT_STEW_FLUID = FLUIDS.register("rabbit_stew_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909));
    public static final RegistryObject<Fluid> SUS_STEW_FLUID = FLUIDS.register("sus_stew_fluid", () -> new GlassJarFluid("block/water_still", GlassJarFluid.BOTTLE_VOLUME, 0xFF533909)); // when the imposter is
    //endregion Fluids

    //region Tile Entities
    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<GlassJarTile>> GLASS_JAR_TILE_ENTITY = TILE_ENTITIES.register("glass_jar", () -> TileEntityType.Builder.create(GlassJarTile::new, GLASS_JAR.get()).build(null));
    //endregion Tile Entities

    //region Entities / Spawn Eggs
    private static final EntityType<FireflyEntity> FIREFLY_BUILDER = EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE).size(0.5f, 0.5f).trackingRange(10).build("firefly");

    private static final Item FIREFLY_SPAWN_EGG = new SpawnEggItem(FIREFLY_BUILDER, 0x5B1313, 0xF7DD36, new Item.Properties().group(ItemGroup.MISC)).setRegistryName(Fireflies.MOD_ID, "firefly_spawn_egg");

    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> FIREFLY_BUILDER);
    //endregion Entities / Spawn Eggs

    //region Particles
    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_REDSTONE_PARTICLE = PARTICLES.register("firefly_dust_redstone_particle", () -> new BasicParticleType(false));
    public static final RegistryObject<ParticleType<FireflyAbdomenParticleData>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyAbdomenParticleData::get);
    public static final RegistryObject<ParticleType<FireflyAbdomenRedstoneParticleData>> FIREFLY_ABDOMEN_REDSTONE_PARTICLE = PARTICLES.register("firefly_abdomen_redstone_particle", FireflyAbdomenRedstoneParticleData::get);
    //endregion Particles

    //region Sounds
    public static final RegistryObject<SoundEvent> FIREFLY_HURT = registerSoundEvent("firefly_hurt");
    public static final RegistryObject<SoundEvent> FIREFLY_DEATH = registerSoundEvent("firefly_death");
    //public static final RegistryObject<SoundEvent> FIREFLY_GLOW = registerSoundEvent("firefly_glow");
    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = registerSoundEvent("firefly_flight_loop");
    public static final RegistryObject<SoundEvent> FIREFLY_APPLY_REDSTONE = registerSoundEvent("firefly_apply_redstone");
    public static final RegistryObject<SoundEvent> GLASS_JAR_OPEN = registerSoundEvent("glass_jar_open");
    public static final RegistryObject<SoundEvent> GLASS_JAR_CLOSE = registerSoundEvent("glass_jar_close");
    //endregion Sounds

    //region Misc
    public static final ResourceLocation FILL_GLASS_JAR_STAT = registerCustomStat("fill_glass_jar");
    public static final ResourceLocation USE_GLASS_JAR_STAT = registerCustomStat("use_glass_jar");
    //endregion Misc

    public static void init() {
        // Register all of our stuffs
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        POTION_TYPES.register(bus);
        FLUIDS.register(bus);
        TILE_ENTITIES.register(bus);
        ENTITIES.register(bus);
        PARTICLES.register(bus);
        SOUNDS.register(bus);
    }

    @SubscribeEvent
    public static void registerSpawnEggs(final RegistryEvent.Register<Item> event) {
        // Register all of our spawn eggs.
        event.getRegistry().registerAll(
                FIREFLY_SPAWN_EGG
        );

        // Registers the spawn egg's dispenser behaviour.
        // Taken from IDispenseItemBehavior#init L192
        DispenserBlock.registerDispenseBehavior(FIREFLY_SPAWN_EGG, new DefaultDispenseItemBehavior() {
            // Dispense the specified stack, play the dispense sound and spawn particles.
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().get(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
                entityType.spawn(source.getWorld(), stack, null, source.getBlockPos().offset(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
                stack.shrink(1);
                return stack;
            }
        });
    }

    @SubscribeEvent
    public static void registerEffects(final RegistryEvent.Register<Effect> event) {
        event.getRegistry().register(HIDDEN_FIREFLY_MASTER_EFFECT);
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(Fireflies.MOD_ID, name)));
    }

    private static ResourceLocation registerCustomStat(String name) {
        // https://forums.minecraftforge.net/topic/72454-custom-stats-1143/?do=findComment&comment=392617
        ResourceLocation rsc = new ResourceLocation(Fireflies.MOD_ID, name);
        net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.CUSTOM_STAT, name, rsc);
        Stats.CUSTOM.get(rsc, IStatFormatter.DEFAULT);
        return rsc;
    }
}
