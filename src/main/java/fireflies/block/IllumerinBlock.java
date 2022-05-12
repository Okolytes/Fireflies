package fireflies.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;

public class IllumerinBlock extends RotatedPillarBlock {
    private static final int ILLUMERIN_RADIUS = 8;

    public IllumerinBlock() {
        super(Properties.create(Material.GLASS, MaterialColor.SAND).hardnessAndResistance(2f).sound(SoundType.BASALT).harvestTool(ToolType.PICKAXE).setAllowsSpawn((a, b, c, d) -> false).setEmmisiveRendering((a, b, c) -> true).setNeedsPostProcessing((a, b, c) -> true));
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }

    public static void stopMobSpawning(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld() == null || event.getWorld().isRemote() || event.getEntity() == null)
            return;

        // Only disable spawner and natural spawns
        if (event.getSpawnReason() != SpawnReason.NATURAL && event.getSpawnReason() != SpawnReason.SPAWNER)
            return;

        // Only disable monster spawns and bats
        if (!event.getEntity().getType().getClassification().equals(EntityClassification.MONSTER) && !(event.getEntity() instanceof BatEntity))
            return;

        // Cancel the spawn if an illumerin / powered illumerin block is within radius
        boolean cancelMobSpawn = false;
        final BlockPos mobPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        for (double x = event.getX() - ILLUMERIN_RADIUS; x < event.getX() + ILLUMERIN_RADIUS; x++) {
            for (double y = event.getY() - ILLUMERIN_RADIUS; y < event.getY() + ILLUMERIN_RADIUS; y++) {
                for (double z = event.getZ() - ILLUMERIN_RADIUS; z < event.getZ() + ILLUMERIN_RADIUS; z++) {
                    final BlockPos blockPos = new BlockPos(x, y, z);
                    if (!blockPos.withinDistance(mobPos, ILLUMERIN_RADIUS)) {
                        continue;
                    }

                    final BlockState state = event.getWorld().getBlockState(blockPos);
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
            event.getEntity().getPassengers().forEach(Entity::remove);

            // Finally, cancel the mob spawn
            event.setResult(Event.Result.DENY);
        }
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return 1; // Needs at least a light level of 1 for the emissiveness to work
    }
}
