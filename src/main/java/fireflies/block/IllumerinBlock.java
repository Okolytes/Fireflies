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
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.ID)
public class IllumerinBlock extends RotatedPillarBlock {
    private static final int ILLUMERIN_RADIUS = 8;
    private static final int ECTO_ILLUMERIN_RADIUS = 5;

    public IllumerinBlock() {
        super(Properties.create(Material.IRON).hardnessAndResistance(2f).sound(SoundType.BASALT).harvestTool(ToolType.PICKAXE).setAllowsSpawn((a, b, c, d) -> false).setEmmisiveRendering((a, b, c) -> true).setNeedsPostProcessing((a, b, c) -> true));
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }

    public static void stopMobSpawning(LivingSpawnEvent.CheckSpawn e) {
        if (e.getWorld() == null || e.getWorld().isRemote() || e.getEntity() == null)
            return;

        // Only disable spawner and natural spawns
        if (e.getSpawnReason() != SpawnReason.NATURAL && e.getSpawnReason() != SpawnReason.SPAWNER)
            return;

        // Only disable monster spawns and bats
        if (!e.getEntity().getType().getClassification().equals(EntityClassification.MONSTER) && !(e.getEntity() instanceof BatEntity))
            return;

        // Cancel the spawn if an illumerin / powered illumerin block is within radius
        boolean cancelMobSpawn = false;
        final BlockPos mobPos = new BlockPos(e.getX(), e.getY(), e.getZ());
        for (double x = e.getX() - ILLUMERIN_RADIUS; x < e.getX() + ILLUMERIN_RADIUS; x++) {
            for (double y = e.getY() - ILLUMERIN_RADIUS; y < e.getY() + ILLUMERIN_RADIUS; y++) {
                for (double z = e.getZ() - ILLUMERIN_RADIUS; z < e.getZ() + ILLUMERIN_RADIUS; z++) {
                    final BlockPos blockPos = new BlockPos(x, y, z);
                    if (!blockPos.withinDistance(mobPos, ILLUMERIN_RADIUS)) {
                        continue;
                    }

                    final BlockState state = e.getWorld().getBlockState(blockPos);
                    final Block block = state.getBlock();
                    if (block instanceof IllumerinBlock || (block instanceof IllumerinLamp && state.get(IllumerinLamp.POWERED))) {
                        if (block instanceof SoulIllumerinBlock && !blockPos.withinDistance(mobPos, ECTO_ILLUMERIN_RADIUS))
                            continue;

                        cancelMobSpawn = true;
                        break;
                    }
                }
            }
        }

        if (cancelMobSpawn) {
            // Remove any passengers that come with it
            e.getEntity().getPassengers().forEach(Entity::remove);

            // Finally, cancel the mob spawn
            e.setResult(Event.Result.DENY);
        }
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return 1; // Needs at least a light level of 1 for the emissiveness to work
    }
}
