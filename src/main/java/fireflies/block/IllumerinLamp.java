package fireflies.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public class IllumerinLamp extends Block {
    /**
     * Only powerable by a redstone firefly.
     */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /**
     * Is this lamp synced to a redstone firefly?
     */
    public static final BooleanProperty SYNCED = BooleanProperty.create("synced");

    public IllumerinLamp() {
        super(Properties.create(Material.REDSTONE_LIGHT).hardnessAndResistance(0.3f).sound(SoundType.GLASS).setAllowsSpawn((a, b, c, d) -> false).setEmmisiveRendering(IllumerinLamp::isEmissive).setNeedsPostProcessing(IllumerinLamp::isEmissive));
        this.setDefaultState(this.stateContainer.getBaseState().with(POWERED, false).with(SYNCED, false));
    }

    private static boolean isEmissive(BlockState state, IBlockReader iBlockReader, BlockPos blockPos) {
        return state.get(IllumerinLamp.POWERED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        super.tick(state, world, pos, rand);
        final boolean flag = world.isBlockPowered(pos);
        if (flag != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, flag), 3);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        // Block removed
        if (!state.matchesBlock(newState.getBlock()) && state.get(POWERED)) {
            world.notifyNeighborsOfStateChange(pos, this);
        }

        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(BlockState state) {
        return state.get(POWERED);
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return state.get(POWERED) ? 1 : 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
        return blockState.get(POWERED) ? 15 : 0;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(POWERED, SYNCED);
    }
}
