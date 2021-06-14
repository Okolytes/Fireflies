package fireflies.block;

import fireflies.init.Registry;
import fireflies.misc.GlassJarFluid;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Random;

public class GlassJarBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

    private static final VoxelShape SHAPE_CLOSED = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(3, 0, 3, 13, 13, 13), Block.makeCuboidShape(5, 13, 5, 11, 16, 11), IBooleanFunction.OR);
    private static final VoxelShape SHAPE_ATTACHED = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(3, 0, 3, 13, 13, 13), Block.makeCuboidShape(5, 13, 5, 11, 15, 11), IBooleanFunction.OR);
    private static final VoxelShape SHAPE_OPEN_NORTH = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(6, 12, 8, 12, 15, 14), Block.makeCuboidShape(4, 0, 6, 14, 13, 16), IBooleanFunction.OR);
    private static final VoxelShape SHAPE_OPEN_SOUTH = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(4, 12, 2, 10, 15, 8), Block.makeCuboidShape(2, 0, 0, 12, 13, 10), IBooleanFunction.OR);
    private static final VoxelShape SHAPE_OPEN_EAST = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(2, 12, 6, 8, 15, 12), Block.makeCuboidShape(0, 0, 4, 10, 13, 14), IBooleanFunction.OR);
    private static final VoxelShape SHAPE_OPEN_WEST = VoxelShapes.combineAndSimplify(Block.makeCuboidShape(8, 12, 4, 14, 15, 10), Block.makeCuboidShape(6, 0, 2, 16, 13, 12), IBooleanFunction.OR);

    public GlassJarBlock() {
        super(Properties.create(Material.GLASS).hardnessAndResistance(0.3f).sound(SoundType.GLASS).setAllowsSpawn((a, b, c, d) -> false).notSolid());
        this.setDefaultState(this.stateContainer.getBaseState().with(LEVEL, 0).with(OPEN, false).with(HORIZONTAL_FACING, Direction.NORTH).with(ATTACHED, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        // Only do stuff on the server
        if (world.isRemote) return ActionResultType.SUCCESS;
        // Make sure the jarTile is actually there
        GlassJarTile jarTile = this.getTile(world, pos);
        if (jarTile == null) return ActionResultType.PASS;
        // Open the jar if it's closed, and return
        if (!state.get(OPEN)) {
            this.cycleOpened(world, state, pos);
            return ActionResultType.CONSUME;
        }

        ItemStack itemStack = player.getHeldItem(hand);
        Item item = itemStack.getItem();
        FluidTank tank = jarTile.getTank();
        // Forge handles water buckets n other stuff
        if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getFace())) return ActionResultType.CONSUME;
            // If forge couldn't handle it, try to fill the tank
        else if (this.fillWithFluid(world, pos, player, hand, tank, item, itemStack, true) != null) return ActionResultType.CONSUME;
            // Else try to empty it
        else if (this.emptyFluid(world, pos, player, hand, tank, item, itemStack)) return ActionResultType.CONSUME;

        // If we couldn't do any of that we'll just open / close it
        this.cycleOpened(world, state, pos);
        return ActionResultType.CONSUME;
    }

    /**
     * Fill our jar with fluid!
     */
    @Nullable
    public ItemStack fillWithFluid(World world, BlockPos pos, @Nullable PlayerEntity player, @Nullable Hand hand, FluidTank tank, Item containerItem, ItemStack container, boolean doTransfer) {
        FluidStack fluidStack;
        // Declaring as a 1 element array so we can use in a lambda
        final ItemStack[] emptyContainer = new ItemStack[1]; // The empty fluid container that will be deposited in to the players inventory.
        if (containerItem.equals(Items.MILK_BUCKET)) {
            fluidStack = ((GlassJarFluid) Registry.MILK_FLUID.get()).getFluidStack();
            emptyContainer[0] = Items.BUCKET.getDefaultInstance();
        } else if (containerItem.equals(Items.POTION) && container.getTag() != null) {
            // If it's a bottle of water we wont use our potion fluid, nor give it any nbt data (so it can be picked up by a bucket)
            boolean isWater = PotionUtils.getPotionFromItem(container).equals(Potions.WATER);
            fluidStack = new FluidStack(isWater ? Fluids.WATER : Registry.GENERIC_POTION_FLUID.get(), ((GlassJarFluid) Registry.GENERIC_POTION_FLUID.get()).getVolume(), isWater ? null : container.getTag());
            emptyContainer[0] = Items.GLASS_BOTTLE.getDefaultInstance();
        } else if (containerItem.equals(Items.HONEY_BOTTLE) || containerItem.equals(Items.DRAGON_BREATH)) {
            // For our generic glass bottled items
            fluidStack = container.getItem().equals(Items.DRAGON_BREATH) ? ((GlassJarFluid) Registry.DRAGON_BREATH_FLUID.get()).getFluidStack() : ((GlassJarFluid) Registry.HONEY_FLUID.get()).getFluidStack();
            emptyContainer[0] = Items.GLASS_BOTTLE.getDefaultInstance();
        } else if ((containerItem instanceof SoupItem || containerItem.equals(Items.SUSPICIOUS_STEW)) && containerItem.getRegistryName() != null) {
            // We use the registry name as nbt data to identify the soup stored in the tank.
            // Note that Sus stew is not a SoupItem for some reason
            ItemStack copiedStack = container.copy(); // Copy the soup
            CompoundNBT nbt = copiedStack.getOrCreateTag(); // Apply a tag to the copied soup
            nbt.putString("Soup", containerItem.getRegistryName().toString());
            // Use the copied soup's nbt tag for the fluidstack
            // Rabbit stew & Sus stew have their own textures / diff fluids so ye
            fluidStack = new FluidStack(container.getItem().equals(Items.RABBIT_STEW) ? Registry.RABBIT_STEW_FLUID.get() : container.getItem().equals(Items.SUSPICIOUS_STEW) ? Registry.SUS_STEW_FLUID.get() : Registry.GENERIC_SOUP_FLUID.get(), ((GlassJarFluid) Registry.GENERIC_SOUP_FLUID.get()).getVolume(), copiedStack.getTag());
            emptyContainer[0] = Items.BOWL.getDefaultInstance();
        } else if (player == null && container.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
            // This is for the hopper interaction
            // Note that milk buckets have the fluid handler capability but won't do anything in FluidUtil.interactWithFluidHandler
            FluidUtil.getFluidHandler(world, pos, null).map(tankFluidHandler -> {
                ItemStack containerCopy = ItemHandlerHelper.copyStackWithSize(container, 1); // do not modify the input
                FluidUtil.getFluidHandler(containerCopy).map(containerFluidHandler -> {
                    // We are acting on a COPY of the stack, so performing changes is acceptable even if we are simulating.
                    FluidStack transfer = FluidUtil.tryFluidTransfer(tankFluidHandler, containerFluidHandler, tank.getCapacity(), doTransfer);
                    // Return if we can't transfer fluid
                    if (transfer.isEmpty()) return FluidActionResult.FAILURE;
                    // FluidUtil.tryFluidTransfer does not drain the passed fluidsource (our bucket) if doTransfer is false, so we'll do it ourselves
                    if (!doTransfer) containerFluidHandler.drain(tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);

                    emptyContainer[0] = containerFluidHandler.getContainer();
                    return FluidActionResult.FAILURE; // This doesn't matter, we only want emptyContainer
                });
                return true; // This doesn't matter
            });
            return emptyContainer[0]; // This is what we're actually returning
        } else return null; // If the given item isn't valid return null
        // If we can't deposit the fluid then return
        if (tank.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) <= 0) return null;

        if (doTransfer) {
            if (player != null && hand != null) {
                // Remove the full fluid container and replace it with its empty counterpart.
                this.changeFluidContainer(player, container, emptyContainer[0], hand);
                // Add our stat to the player
                player.addStat(Registry.FILL_GLASS_JAR_STAT);
                // Play a fill sound, buckets use their own sound
                world.playSound(null, pos, emptyContainer[0].getItem().equals(Items.BUCKET) ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            // Finally, fill the tank
            tank.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        }
        // ... and return the empty fluid container item
        return emptyContainer[0];
    }

    /**
     * Deplete fluid from our jar
     */
    private boolean emptyFluid(World world, BlockPos pos, PlayerEntity player, Hand hand, FluidTank tank, Item containerItem, ItemStack container) {
        ItemStack filledContainer; // The full fluid container that will be deposited in to the players inventory.
        if (containerItem.equals(Items.BUCKET)) {
            if (!this.canDrain(tank, FluidAttributes.BUCKET_VOLUME) || !this.hasFluid(tank, Registry.MILK_FLUID.get())) return false;
            filledContainer = Items.MILK_BUCKET.getDefaultInstance();
        } else if (containerItem.equals(Items.GLASS_BOTTLE)) {
            // Fill our glass bottled containers
            if (!this.canDrain(tank, GlassJarFluid.BOTTLE_VOLUME)) return false;
            if (this.hasFluid(tank, Registry.GENERIC_POTION_FLUID.get())) {
                CompoundNBT tag = tank.getFluid().getTag();
                // Fill our potion, if the tag is null for some reason we'll just give out some water.
                filledContainer = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), tag == null ? Potions.WATER : PotionUtils.getPotionTypeFromNBT(tag));
            } else if (this.hasFluid(tank, Fluids.WATER)) {
                filledContainer = Items.POTION.getDefaultInstance(); // As bruce lee would say... WOTAAHHHHH
            } else if (this.hasFluid(tank, Registry.HONEY_FLUID.get())) {
                filledContainer = Items.HONEY_BOTTLE.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.DRAGON_BREATH_FLUID.get())) {
                filledContainer = Items.DRAGON_BREATH.getDefaultInstance();
            } else return false;
        } else if (containerItem.equals(Items.BOWL)) {
            // Fill our bowl with soup
            if (!this.canDrain(tank, GlassJarFluid.BOTTLE_VOLUME)) return false;
            if (this.hasFluid(tank, Registry.GENERIC_SOUP_FLUID.get())) {
                // Use the tag which contains the registry key of the given soup
                CompoundNBT tag = tank.getFluid().getTag();
                if (tag == null) return false;
                Item soup = ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString("Soup")));
                if (soup == null) return false; // No soup for you
                filledContainer = soup.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.RABBIT_STEW_FLUID.get())) {
                filledContainer = Items.RABBIT_STEW.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.SUS_STEW_FLUID.get())) {
                filledContainer = Items.SUSPICIOUS_STEW.getDefaultInstance();
            } else return false;
        } else return false;

        // Remove the empty fluid container and replace it with its full counterpart.
        this.changeFluidContainer(player, container, filledContainer, hand);
        // Add our jar stat
        player.addStat(Registry.USE_GLASS_JAR_STAT);
        // Play an "empty" sound, buckets use their own one
        world.playSound(null, pos, containerItem.equals(Items.BUCKET) ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        // Finally drain the tank, using the volume of a bottle if it's not a bucket.
        tank.drain(containerItem.equals(Items.BUCKET) ? FluidAttributes.BUCKET_VOLUME : GlassJarFluid.BOTTLE_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /**
     * Helper method for replacing the itemstack in the players inventory with a new one.
     * Taken from {@link CauldronBlock#onBlockActivated}
     */
    private void changeFluidContainer(PlayerEntity player, ItemStack oldStack, ItemStack newStack, Hand hand) {
        if (!player.isCreative()) {
            oldStack.shrink(1);
            if (oldStack.isEmpty()) {
                player.setHeldItem(hand, newStack);
            } else if (!player.inventory.addItemStackToInventory(newStack)) {
                player.dropItem(newStack, false);
            } else if (player instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) player).sendContainerToPlayer(player.container);
            }
        }
    }

    /**
     * @return Has this fluid type in a tank?
     */
    private boolean hasFluid(FluidTank tank, Fluid fluid) {
        return !tank.isEmpty() && tank.getFluid().getFluid().isEquivalentTo(fluid);
    }

    /**
     * @return Can drain this amount of fluid from a tank?
     */
    private boolean canDrain(FluidTank tank, int amount) {
        return tank.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount() != 0;
    }

    /**
     * Cycles the value of {@link GlassJarBlock#OPEN}, sets the {@link GlassJarBlock#ATTACHED} property accordingly, and plays a sound.
     */
    private void cycleOpened(World world, BlockState state, BlockPos pos) {
        state = state.cycleValue(OPEN);
        world.setBlockState(pos, state, 3);

        boolean open = state.get(OPEN);
        if (!open && state.get(ATTACHED)) {
            this.setAttached(world, pos, state, false);
        } else if (shouldBeAttached(world, pos, state)) {
            this.setAttached(world, pos, state, true);
        }

        world.playSound(null, pos, open ? Registry.GLASS_JAR_CLOSE.get() : Registry.GLASS_JAR_OPEN.get(), SoundCategory.BLOCKS, 0.5F, MathHelper.nextFloat(world.rand, 0.85f, 1.15f));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        this.setAttached(world, pos, state, shouldBeAttached(world, pos, state));
    }

    /**
     * @return Should the jar should be attached to a hopper?
     */
    public static boolean shouldBeAttached(World world, BlockPos jarPos, BlockState jarState) {
        BlockState state = world.getBlockState(jarPos.up());
        if (state.getBlock() instanceof HopperBlock && jarState.get(OPEN)) {
            return state.get(HopperBlock.FACING).equals(Direction.DOWN) && state.get(HopperBlock.ENABLED);
        }
        return false;
    }

    /**
     * Set the {@link GlassJarBlock#ATTACHED} property, if its cached value is not equal to the new one.
     */
    private void setAttached(World world, BlockPos pos, BlockState state, boolean b) {
        GlassJarTile jarTile = this.getTile(world, pos);
        if (jarTile != null && jarTile.cachedAttached != b) {
            world.setBlockState(pos, state.with(ATTACHED, b), 3);
        }
    }

    /**
     * @return Instance of the jar TE.
     */
    @Nullable
    private GlassJarTile getTile(IBlockReader world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof GlassJarTile) return ((GlassJarTile) tileEntity);
        return null;
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        GlassJarTile glassJar = this.getTile(world, pos);
        if (glassJar != null) {
            return glassJar.luminosity;
        }
        return 0;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new GlassJarTile();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    @OnlyIn(Dist.CLIENT)
    public float getAmbientOcclusionLightValue(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
        return blockState.get(LEVEL);
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, OPEN, HORIZONTAL_FACING, ATTACHED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        boolean attached = state.get(ATTACHED);
        if (state.get(OPEN) && !attached) {
            switch (state.get(HORIZONTAL_FACING)) {
                case NORTH:
                    return SHAPE_OPEN_NORTH;
                case SOUTH:
                    return SHAPE_OPEN_SOUTH;
                case EAST:
                    return SHAPE_OPEN_EAST;
                case WEST:
                    return SHAPE_OPEN_WEST;
            }
        } else if (attached) {
            return SHAPE_ATTACHED;
        }

        return SHAPE_CLOSED;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rand) {
        GlassJarTile tile = this.getTile(world, pos);
        // Spawn ender particles if dragon breath is inside the jar.
        if (tile != null && this.hasFluid(tile.getTank(), Registry.DRAGON_BREATH_FLUID.get())) {
            // Amount of particles depends of the fullness of the jar.
            for (int i = 0; i < tile.cachedLevel; ++i) {
                // Taken from ender chest code
                int j = rand.nextInt(2) * 2 - 1;
                int k = rand.nextInt(2) * 2 - 1;
                double d0 = (double) pos.getX() + 0.5D + (tile.cachedOpen && !tile.cachedAttached ? tile.cachedDirection.getXOffset() * 0.25f : 0.25f) * (double) j;
                double d1 = (float) pos.getY() + rand.nextFloat();
                double d2 = (double) pos.getZ() + 0.5D + (tile.cachedOpen && !tile.cachedAttached ? tile.cachedDirection.getZOffset() * 0.25f : 0.25f) * (double) k;
                double d3 = rand.nextFloat() * (float) j;
                double d4 = ((double) rand.nextFloat() - 0.5D) * 0.125D;
                double d5 = rand.nextFloat() * (float) k;
                world.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
            }
        }
    }
}
