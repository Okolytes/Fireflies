package fireflies.setup;

import fireflies.Fireflies;
import fireflies.entity.firefly.FireflyAbdomenParticleData;
import fireflies.entity.firefly.FireflyEntity;
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
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
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

    private static final Item FIREFLY_SPAWN_EGG = new SpawnEggItem(FIREFLY_BUILDER, 0x330400, 0x3E3E5B, new Item.Properties().group(ItemGroup.MISC))
            .setRegistryName(Fireflies.MOD_ID, "firefly_spawn_egg");

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
                FIREFLY_SPAWN_EGG
        );

        // IDispenseItemBehavior#init L192
        DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior() {
            /**
             * Dispense the specified stack, play the dispense sound and spawn particles.
             */
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().get(DispenserBlock.FACING);
                EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
                entitytype.spawn(source.getWorld(), stack, null, source.getBlockPos().offset(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
                stack.shrink(1);
                return stack;
            }
        };
        DispenserBlock.registerDispenseBehavior(FIREFLY_SPAWN_EGG, defaultDispenseItemBehavior);
    }
}
