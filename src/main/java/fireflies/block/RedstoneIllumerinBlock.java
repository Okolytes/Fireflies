package fireflies.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;

import java.util.Random;

public class RedstoneIllumerinBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /**
     * When the block is activated by a redstone firefly, it cannot be overridden by other signals
     */
    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;

    public RedstoneIllumerinBlock() {
        super(Properties.create(Material.IRON).hardnessAndResistance(1f).sound(SoundType.GLASS).harvestTool(ToolType.PICKAXE)
                .setAllowsSpawn((a, b, c, d) -> false).setEmmisiveRendering(RedstoneIllumerinBlock::isEmissive).setNeedsPostProcessing(RedstoneIllumerinBlock::isEmissive));

        this.setDefaultState(this.stateContainer.getBaseState().with(POWERED, Boolean.FALSE).with(LOCKED, Boolean.FALSE));
    }

    private static boolean isEmissive(BlockState state, IBlockReader iBlockReader, BlockPos blockPos) {
        return state.get(RedstoneIllumerinBlock.POWERED);
    }

    private void updatePower(BlockPos pos, World world, BlockState state) {
        if (state.get(LOCKED))
            return;

        boolean flag = world.isBlockPowered(pos);
        if (flag != state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, flag), 3);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        super.tick(state, worldIn, pos, rand);
        this.updatePower(pos, worldIn, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        this.updatePower(pos, worldIn, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && context.getWorld().isBlockPowered(context.getPos()) && !state.get(LOCKED)) {
            state = state.with(POWERED, Boolean.TRUE);
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.matchesBlock(newState.getBlock())) {
            if (state.get(POWERED)) {
                world.notifyNeighborsOfStateChange(pos, this);
            }
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
        builder.add(POWERED, LOCKED);
    }
}
