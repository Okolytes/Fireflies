package fireflies.block;

import fireflies.Fireflies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class IllumerinBlock extends RotatedPillarBlock {
    private static final int ILLUMERIN_RADIUS = 8;

    public IllumerinBlock() {
        super(Properties.of(Material.GLASS, MaterialColor.SAND).strength(2f).sound(SoundType.BASALT).requiresCorrectToolForDrops().isValidSpawn((a, b, c, d) -> false).emissiveRendering((a, b, c) -> true).hasPostProcess((a, b, c) -> true));
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @SubscribeEvent
    public static void stopMobSpawning(LivingSpawnEvent.CheckSpawn event) {
        if (event.getLevel() == null || event.getLevel().isClientSide() || event.getEntity() == null)
            return;

        // Only disable spawner and natural spawns
        if (event.getSpawnReason() != MobSpawnType.NATURAL && event.getSpawnReason() != MobSpawnType.SPAWNER)
            return;

        // Only disable monster spawns and bats
        if (!event.getEntity().getType().getCategory().equals(MobCategory.MONSTER) && !(event.getEntity() instanceof Bat))
            return;

        // Cancel the spawn if an illumerin / powered illumerin block is within radius
        boolean cancelMobSpawn = false;
        final BlockPos mobPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        for (double x = event.getX() - ILLUMERIN_RADIUS; x < event.getX() + ILLUMERIN_RADIUS; x++) {
            for (double y = event.getY() - ILLUMERIN_RADIUS; y < event.getY() + ILLUMERIN_RADIUS; y++) {
                for (double z = event.getZ() - ILLUMERIN_RADIUS; z < event.getZ() + ILLUMERIN_RADIUS; z++) {
                    final BlockPos blockPos = new BlockPos(x, y, z);
                    if (!blockPos.closerThan(mobPos, ILLUMERIN_RADIUS)) {
                        continue;
                    }

                    final BlockState state = event.getLevel().getBlockState(blockPos);
                    final Block block = state.getBlock();
                    if (block instanceof IllumerinBlock) {
                        cancelMobSpawn = true;
                        break;
                    }
                }
            }
        }

        if (cancelMobSpawn) {
            // Remove any passengers that come with it
            for (var passenger : event.getEntity().getPassengers()){
                passenger.remove(Entity.RemovalReason.DISCARDED);
            }

            // Finally, cancel the mob spawn
            event.setResult(Event.Result.DENY);
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;// Needs at least a light level of 1 for the emissiveness to work
    }
}
