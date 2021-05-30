package fireflies.block;

import fireflies.Fireflies;
import fireflies.setup.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class IllumerinBlockTile extends TileEntity {
    public IllumerinBlockTile() {
        super(Registry.ILLUMERIN_BLOCK_TILE.get());
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) { // Load
        super.read(state, nbt);
        if (this.world != null && pos != null) {
            if (this.world.getBlockState(pos).getBlock() instanceof RedstoneIllumerinBlock && !this.world.getBlockState(pos).get(RedstoneIllumerinBlock.POWERED)) {
                return;
            }

            IllumerinBlock.addBounds(pos);
        }
    }

    @SubscribeEvent
    public static void onServerWorldLoad(WorldEvent.Load e) {
        if (e.getWorld() == null || e.getWorld().isRemote())
            return;

        // Remove any invalid blocks
        IllumerinBlock.ILLUMERIN_BOUNDS.removeIf(axisAlignedBB ->
                !(e.getWorld().getBlockState(new BlockPos(axisAlignedBB.getCenter())).getBlock() instanceof IllumerinBlock));
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return super.write(compound);
    }
}