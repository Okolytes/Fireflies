package fireflies.block;

import fireflies.Fireflies;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class IllumerinBlock extends RotatedPillarBlock {
    private static final int ILLUMERIN_RADIUS = 8;

    public IllumerinBlock() {
        super(Properties.create(Material.IRON).hardnessAndResistance(2f).sound(SoundType.NETHERRACK).setAllowsSpawn((a, b, c, d) -> false)
                .setEmmisiveRendering(IllumerinBlock::isEmissive).setNeedsPostProcessing(IllumerinBlock::isEmissive));
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }

    private static boolean isEmissive(BlockState state, IBlockReader iBlockReader, BlockPos blockPos) {
        // Don't be emissive in off state
        return state.getBlock() instanceof IllumerinBlock && (!(state.getBlock() instanceof RedstoneIllumerinBlock) || state.get(RedstoneIllumerinBlock.POWERED));
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return 1; // Needs at least a light level of 1 for the emissiveness to work
    }

    @SubscribeEvent
    public static void stopMobSpawning(LivingSpawnEvent.CheckSpawn e) {
        if (e.getWorld() == null || e.getWorld().isRemote() || e.getEntity() == null)
            return;

        // Only disable spawner and natural spawns
        if (e.getSpawnReason() != SpawnReason.NATURAL && e.getSpawnReason() != SpawnReason.SPAWNER)
            return;

        // Only disable monster spawns
        if (!e.getEntity().getType().getClassification().equals(EntityClassification.MONSTER))
            return;

        // Cancel the spawn if an illumerin / powered illumerin block is within an 8x8x8 radius
        boolean cancelMobSpawn = false;
        for (double x = e.getX() - ILLUMERIN_RADIUS; x < e.getX() + ILLUMERIN_RADIUS; x++) {
            for (double y = e.getY() - ILLUMERIN_RADIUS; y < e.getY() + ILLUMERIN_RADIUS; y++) {
                for (double z = e.getZ() - ILLUMERIN_RADIUS; z < e.getZ() + ILLUMERIN_RADIUS; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (!blockPos.withinDistance(new BlockPos(e.getX(), e.getY(), e.getZ()), ILLUMERIN_RADIUS)) {
                        continue;
                    }

                    BlockState state = e.getWorld().getBlockState(blockPos);
                    Block block = state.getBlock();
                    // Don't cancel if the block is unpowered
                    if (block instanceof IllumerinBlock && (!(block instanceof RedstoneIllumerinBlock) || state.get(RedstoneIllumerinBlock.POWERED))) {
                        cancelMobSpawn = true;
                        break;
                    }
                }
            }
        }

        if (cancelMobSpawn) {
            // Remove any passengers that come with it
            for (Entity passenger : e.getEntity().getPassengers()) {
                passenger.remove();
            }

            // Finally, cancel the mob spawn
            e.setResult(Event.Result.DENY);
        }
    }
}
