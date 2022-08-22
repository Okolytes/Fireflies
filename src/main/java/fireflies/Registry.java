package fireflies;

import fireflies.block.IllumerinBlock;
import fireflies.block.IllumerinLantern;
import fireflies.entity.FireflyEntity;
import fireflies.item.FireflyBottleItem;
import fireflies.item.IllumerinItem;
import fireflies.misc.FireflyParticleData;
import fireflies.misc.FireflySpawnEgg;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.MOD_ID);


    // Blocks
    public static final RegistryObject<Block> ILLUMERIN_BLOCK = BLOCKS.register("illumerin_block", IllumerinBlock::new);
    public static final RegistryObject<Item> ILLUMERIN_BLOCK_ITEM = fromBlock(ILLUMERIN_BLOCK, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
    public static final RegistryObject<Block> ILLUMERIN_LANTERN = BLOCKS.register("illumerin_lantern", IllumerinLantern::new);
    public static final RegistryObject<Item> ILLUMERIN_LANTERN_ITEM = fromBlock(ILLUMERIN_LANTERN, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));

    // Items
    public static final RegistryObject<Item> ILLUMERIN = ITEMS.register("illumerin", IllumerinItem::new);
    public static final RegistryObject<Item> FIREFLY_BOTTLE = ITEMS.register("firefly_bottle", FireflyBottleItem::new);

    // Entities / Spawn Eggs
    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITY_TYPES.register("firefly", () -> EntityType.Builder.of(FireflyEntity::new, MobCategory.CREATURE).sized(0.5f, 0.5f).clientTrackingRange(10).build("firefly"));
    private static final RegistryObject<ForgeSpawnEggItem> FIREFLY_SPAWN_EGG = ITEMS.register("firefly_spawn_egg", () -> new FireflySpawnEgg(FIREFLY, 0x5B1313, 0xF7DD36, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    // Particles
    public static final RegistryObject<ParticleType<FireflyParticleData.Dust>> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", FireflyParticleData.Dust::get);
    public static final RegistryObject<ParticleType<FireflyParticleData.Abdomen>> FIREFLY_ABDOMEN_PARTICLE = PARTICLES.register("firefly_abdomen_particle", FireflyParticleData.Abdomen::get);

    // Sounds
    public static final RegistryObject<SoundEvent> FIREFLY_HURT = registerSoundEvent("firefly_hurt");
    public static final RegistryObject<SoundEvent> FIREFLY_DEATH = registerSoundEvent("firefly_death");
    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = registerSoundEvent("firefly_flight_loop");

    // Stats
	// todo
	
    public static void register(final IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        PARTICLES.register(bus);
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(Fireflies.MOD_ID, name)));
    }

    private static ResourceLocation registerCustomStat(String name) {
        // https://forums.minecraftforge.net/topic/72454-custom-stats-1143/?do=findComment&comment=392617
        final ResourceLocation rsc = new ResourceLocation(Fireflies.MOD_ID, name);
        net.minecraft.core.Registry.register(net.minecraft.core.Registry.CUSTOM_STAT, name, rsc);
        Stats.CUSTOM.get(rsc, StatFormatter.DEFAULT);
        return rsc;
    }

    // https://github.com/McJty/TutorialV3/blob/7fe99a7fd4f03c71550c9fa61d75bbcf434805aa/src/main/java/com/example/tutorialv3/setup/Registration.java#L118
    // Conveniance function: Take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block, Item.Properties properties) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
    }
}
