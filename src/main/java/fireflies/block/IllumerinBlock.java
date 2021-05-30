package fireflies.block;

import fireflies.Fireflies;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.SpawnReason;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class IllumerinBlock extends RotatedPillarBlock {
    public static final ArrayList<AxisAlignedBB> ILLUMERIN_BOUNDS = new ArrayList<>();
    private static final int ILLUMERIN_RADIUS = 8;

    public IllumerinBlock() {
        super(Properties.create(Material.IRON).hardnessAndResistance(2f).hardnessAndResistance(0.3F).sound(SoundType.NETHERRACK)
                .setNeedsPostProcessing(IllumerinBlock::needsPostProcessing).setEmmisiveRendering(IllumerinBlock::needsPostProcessing));
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return 1;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new IllumerinBlockTile();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
        addBounds(pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onReplaced(state, worldIn, pos, newState, isMoving);
        ILLUMERIN_BOUNDS.remove(getBounds(pos));
    }

    public static void addBounds(BlockPos pos) {
        AxisAlignedBB bounds = getBounds(pos);
        if (!ILLUMERIN_BOUNDS.contains(bounds)) {
            ILLUMERIN_BOUNDS.add(bounds);
        }
    }

    public static AxisAlignedBB getBounds(BlockPos pos) {
        return new AxisAlignedBB(
                pos.getX() + ILLUMERIN_RADIUS, pos.getY() + ILLUMERIN_RADIUS, pos.getZ() + ILLUMERIN_RADIUS,
                pos.getX() - ILLUMERIN_RADIUS, pos.getY() - ILLUMERIN_RADIUS, pos.getZ() - ILLUMERIN_RADIUS);
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
        long start = System.nanoTime();
        if (ILLUMERIN_BOUNDS.stream().noneMatch(axisAlignedBB ->
                axisAlignedBB.intersects(e.getX(), e.getY(), e.getZ(), e.getX(), e.getY(), e.getZ()))) {

            // Remove any passengers that come with it
            for (Entity passenger : e.getEntity().getPassengers()) {
                passenger.remove();
            }

            // Finally, deny the mob spawn
            e.setResult(Event.Result.DENY);
        }
        //System.out.println(System.nanoTime() - start);
    }

    private static boolean needsPostProcessing(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }
}
