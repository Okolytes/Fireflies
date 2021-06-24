package fireflies.block;

import fireflies.Registry;
import fireflies.misc.GlassJarFluid;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.INameable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class GlassJarTile extends TileEntity implements ITickableTileEntity, INameable {
    public static final String DISPLAY_NAME = "DisplayName";
    public static final int CAPACITY = FluidAttributes.BUCKET_VOLUME;
    private final FluidTank fluidTank = new FluidTank(CAPACITY) {
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
    public int cachedLevel;
    public int cachedIllumerinLevel;
    public boolean cachedOpen;
    public Direction cachedDirection = Direction.NORTH;
    public int cachedFluidColor = -69; // I think -1 is already taken ¯\_(ツ)_/¯
    public boolean cachedAttached;
    public int luminosity;

    private int updateTicks;
    private int hopperSlot;
    @Nullable
    private ITextComponent name;

    public GlassJarTile() {
        super(Registry.GLASS_JAR_TILE_ENTITY.get());
    }

    @Override
    public void tick() {
        this.updateTicks++;
        if (this.updateTicks % 20 == 0 && this.world != null && !this.world.isRemote && GlassJarBlock.shouldBeAttached(this.world, this.pos, this.getBlockState())) {
            IInventory inventory = HopperTileEntity.getInventoryAtPosition(world, pos.up());
            if (inventory == null) return;
            GlassJarBlock glassJar = (GlassJarBlock) this.getBlockState().getBlock();

            // Iterate through each slot every 20 ticks
            if (this.hopperSlot >= inventory.getSizeInventory()) {
                this.hopperSlot = 0;
            }

            ItemStack fluidContainer = inventory.getStackInSlot(hopperSlot);
            ItemStack emptyContainer = glassJar.fillWithFluid(world, pos, null, null, fluidTank, fluidContainer.getItem(), fluidContainer, false);
            if (emptyContainer == null) {
                this.hopperSlot++;
                return;
            }

            boolean flag = false;
            if (!HopperTileEntity.putStackInInventoryAllSlots(inventory, inventory, emptyContainer, null).isEmpty()) {
                ItemStack fluidContainerCopy = fluidContainer.copy();
                fluidContainerCopy.shrink(1);
                if (inventory.isItemValidForSlot(this.hopperSlot, emptyContainer) && fluidContainerCopy.isEmpty()) {
                    flag = true;
                } else {
                    this.hopperSlot++;
                    return;
                }
            }

            // Fill the jar
            glassJar.fillWithFluid(world, pos, null, null, fluidTank, fluidContainer.getItem(), fluidContainer, true);
            if (!flag) {
                // Remove the fluid container item from the inventory
                fluidContainer.shrink(1);
            } else {
                fluidContainer = emptyContainer;
                inventory.setInventorySlotContents(this.hopperSlot, fluidContainer);
            }
            // Update
            inventory.markDirty();
            this.hopperSlot++;
        }
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
        return new SUpdateTileEntityPacket(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        if (this.world != null) {
            this.read(this.world.getBlockState(this.pos), pkt.getNbtCompound());
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.fluidTank.readFromNBT(nbt);
        if (nbt.contains("CustomName", 8)) {
            this.name = ITextComponent.Serializer.getComponentFromJson(nbt.getString("CustomName"));
        }
        this.handleUUID(nbt);
        this.updateLuminosity();
        this.updateCachedProperties();
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt = super.write(nbt);
        this.fluidTank.writeToNBT(nbt);
        if (this.name != null) {
            nbt.putString("CustomName", ITextComponent.Serializer.toJson(this.name));
        }
        this.handleUUID(nbt);
        this.updateLuminosity();
        this.updateCachedProperties();
        return nbt;
    }

    public ITextComponent getName() {
        return this.name != null ? this.name : new TranslationTextComponent("block.fireflies.glass_jar");
    }

    public void setName(ITextComponent name) {
        this.name = name;
    }

    @Nullable
    public ITextComponent getCustomName() {
        return this.name;
    }

    public void updateLuminosity() {
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

    public void updateCachedProperties() {
        if (this.world != null) {
            BlockState state = this.getBlockState();
            this.cachedLevel = state.get(GlassJarBlock.LEVEL);
            this.cachedIllumerinLevel = state.get(GlassJarBlock.ILLUMERIN_LEVEL);
            this.cachedOpen = state.get(GlassJarBlock.OPEN);
            this.cachedDirection = state.get(GlassJarBlock.HORIZONTAL_FACING);
            this.cachedFluidColor = -69;
            this.cachedAttached = state.get(GlassJarBlock.ATTACHED);
        }
    }

    private void putDisplayName(CompoundNBT nbt) {
        if (GlassJarBlock.hasFluid(this.fluidTank, Registry.GENERIC_POTION_FLUID.get())) {
            nbt.putString(DISPLAY_NAME, PotionUtils.addPotionToItemStack(Items.POTION.getDefaultInstance(), Potion.getPotionTypeForName(this.fluidTank.getFluid().getTag().getString("Potion"))).getDisplayName().getString());
        } else if (GlassJarBlock.hasFluid(this.fluidTank, Registry.GENERIC_SOUP_FLUID.get())) {
            Item soup = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryCreate(this.fluidTank.getFluid().getTag().getString("Soup")));
            if (soup != null) {
                nbt.putString(DISPLAY_NAME, new ItemStack(soup).getDisplayName().getString());
            }
        } else {
            nbt.putString(DISPLAY_NAME, this.fluidTank.getFluid().getDisplayName().getString());
        }
    }

    private void handleUUID(CompoundNBT nbt){
        // So non empty glass jars aren't stackable.
        if (!this.fluidTank.isEmpty() && !nbt.hasUniqueId("UUID")) {
            nbt.putUniqueId("UUID", UUID.randomUUID());
        } else if (this.fluidTank.isEmpty() && nbt.hasUniqueId("UUID")) {
            nbt.remove("UUID");
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

    public ItemStack getItem(BlockState state) {
        ItemStack itemStack = new ItemStack(state.getBlock());

        if (this.name != null) {
            itemStack.setDisplayName(this.name);
        }

        if (!this.fluidTank.isEmpty()) {
            CompoundNBT nbt = itemStack.getOrCreateChildTag("BlockEntityTag");
            this.fluidTank.writeToNBT(nbt);
            this.handleUUID(nbt);
        }

        return itemStack;
    }

    public FluidTank getTank() {
        return this.fluidTank;
    }
}
