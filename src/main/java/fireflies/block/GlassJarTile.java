package fireflies.block;

import fireflies.fluid.GlassJarFluid;
import fireflies.init.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GlassJarTile extends TileEntity {
    public int cachedLevel;
    public boolean cachedOpen;
    public Direction cachedDirection = Direction.NORTH;
    public int cachedFluidColor = -69; // I think -1 is already taken ¯\_(ツ)_/¯
    public int luminosity;

    private final FluidTank fluidTank = new FluidTank(FluidAttributes.BUCKET_VOLUME) {
        @Override
        protected void onContentsChanged() {
            if (world != null) {
                BlockState state = world.getBlockState(pos);
                BlockState newState = state.with(GlassJarBlock.LEVEL, this.getFluidAmount() / GlassJarFluid.BOTTLE_VOLUME);
                world.setBlockState(pos, newState, 3);
                world.notifyBlockUpdate(pos, state, newState, 3);
                markDirty();
            }
        }
    };
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> fluidTank);

    public GlassJarTile() {
        super(Registry.GLASS_JAR_TILE_ENTITY.get());
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.read(state, tag);
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.getPos(), 0, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        if (this.world != null) {
            this.read(this.world.getBlockState(this.getPos()), pkt.getNbtCompound());
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.fluidTank.readFromNBT(nbt);
        this.updateLuminosity();
        this.updateCachedProperties();
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt = super.write(nbt);
        this.fluidTank.writeToNBT(nbt);
        this.updateLuminosity();
        this.updateCachedProperties();
        return nbt;
    }

    private void updateLuminosity() {
        if (this.world != null) {
            FluidStack fluidStack = this.getTank().getFluid();
            int jarLevel = this.getBlockState().get(GlassJarBlock.LEVEL);
            int fluidLuminosity;
            CompoundNBT tag = fluidStack.getTag();
            // Potions with glowing effect get a bit of light
            fluidLuminosity = ((tag != null) && tag.contains("Potion")) ? (PotionUtils.getPotionTypeFromNBT(tag).getEffects().stream().anyMatch(effectInstance -> effectInstance.getPotion().equals(Effects.GLOWING)) ? 2 : 0) : fluidStack.getFluid().getAttributes().getLuminosity(fluidStack);
            this.luminosity = jarLevel > 0 && fluidLuminosity > 0 ? MathHelper.clamp(fluidLuminosity / 2 + jarLevel, 0, 15) : 0;
            this.world.getLightManager().checkBlock(this.getPos());
        }
    }

    private void updateCachedProperties() {
        if (this.world != null) {
            BlockState state = this.world.getBlockState(this.getPos());
            this.cachedLevel = state.get(GlassJarBlock.LEVEL);
            this.cachedOpen = state.get(GlassJarBlock.OPEN);
            this.cachedDirection = state.get(GlassJarBlock.HORIZONTAL_FACING);
            this.cachedFluidColor = -69;
        }
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public FluidTank getTank() {
        return this.fluidTank;
    }
}
