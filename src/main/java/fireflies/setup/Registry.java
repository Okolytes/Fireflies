package fireflies.setup;

import fireflies.Fireflies;
import fireflies.block.EctoIllumerinBlock;
import fireflies.block.IllumerinBlock;
import fireflies.block.RedstoneIllumerinBlock;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.*;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
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
public class Registry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Fireflies.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fireflies.MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Fireflies.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Fireflies.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Fireflies.MOD_ID);

    //region Blocks / BlockItems
    public static final RegistryObject<Block> ILLUMERIN_BLOCK = BLOCKS.register("illumerin_block", IllumerinBlock::new);
    public static final RegistryObject<Item> ILLUMERIN_BLOCKITEM = ITEMS.register("illumerin_block", () -> new BlockItem(ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));

    public static final RegistryObject<Block> REDSTONE_ILLUMERIN_BLOCK = BLOCKS.register("redstone_illumerin_block", RedstoneIllumerinBlock::new);
    public static final RegistryObject<Item> REDSTONE_ILLUMERIN_BLOCKITEM = ITEMS.register("redstone_illumerin_block", () -> new BlockItem(REDSTONE_ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.REDSTONE)));

    public static final RegistryObject<Block> ECTO_ILLUMERIN_BLOCK = BLOCKS.register("ecto_illumerin_block", EctoIllumerinBlock::new);
    public static final RegistryObject<Item> ECTO_ILLUMERIN_BLOCKITEM = ITEMS.register("ecto_illumerin_block", () -> new BlockItem(ECTO_ILLUMERIN_BLOCK.get(), new Item.Properties().group(ItemGroup.DECORATIONS)));
    //endregion Blocks / BlockItems

    //region Items
    public static final RegistryObject<Item> ILLUMERIN = ITEMS.register("illumerin", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
    //endregion Items

    //region Entities / Spawn Eggs
    private static final EntityType<FireflyEntity> FIREFLY_BUILDER = EntityType.Builder.create(FireflyEntity::new, EntityClassification.CREATURE)
            .size(0.5f, 0.5f)
            .trackingRange(10)
            .build("firefly");

    private static final Item FIREFLY_SPAWN_EGG = new SpawnEggItem(FIREFLY_BUILDER, 0x5B1313, 0xF7DD36, new Item.Properties().group(ItemGroup.MISC)).setRegistryName(Fireflies.MOD_ID, "firefly_spawn_egg");

    public static final RegistryObject<EntityType<FireflyEntity>> FIREFLY = ENTITIES.register("firefly", () -> FIREFLY_BUILDER);
    //endregion Entities / Spawn Eggs

    //region Particles
    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_PARTICLE = PARTICLES.register("firefly_dust_particle", () -> new BasicParticleType(false));
    public static final RegistryObject<BasicParticleType> FIREFLY_DUST_REDSTONE_PARTICLE = PARTICLES.register("firefly_dust_redstone_particle", () -> new BasicParticleType(false));
    //endregion Particles

    //region Sounds
    public static final RegistryObject<SoundEvent> FIREFLY_HURT = registerSoundEvent("firefly_hurt");
    public static final RegistryObject<SoundEvent> FIREFLY_DEATH = registerSoundEvent("firefly_death");
    //public static final RegistryObject<SoundEvent> FIREFLY_GLOW = registerSoundEvent("firefly_glow");
    public static final RegistryObject<SoundEvent> FIREFLY_FLIGHT_LOOP = registerSoundEvent("firefly_flight_loop");
    public static final RegistryObject<SoundEvent> FIREFLY_APPLY_REDSTONE = registerSoundEvent("firefly_apply_redstone");
    //endregion Sounds

    public static void init() {
        // Register all of our stuffs
        final IEventBus iEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(iEventBus);
        ITEMS.register(iEventBus);
        TILE_ENTITIES.register(iEventBus);
        ENTITIES.register(iEventBus);
        PARTICLES.register(iEventBus);
        SOUNDS.register(iEventBus);
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

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(Fireflies.MOD_ID, name)));
    }
}
