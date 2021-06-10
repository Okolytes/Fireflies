package fireflies.block;

import fireflies.fluid.GlassJarFluid;
import fireflies.init.Registry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SoupItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Random;

public class GlassJarBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public GlassJarBlock() {
        super(Properties.create(Material.GLASS).hardnessAndResistance(0.3f).sound(SoundType.GLASS).setAllowsSpawn((a, b, c, d) -> false).notSolid().setAllowsSpawn((a, b, c, d) -> false).setOpaque((a, b, c) -> false).setSuffocates((a, b, c) -> false).setBlocksVision((a, b, c) -> false));
        this.setDefaultState(this.stateContainer.getBaseState().with(LEVEL, 0).with(OPEN, Boolean.FALSE));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        // Only do stuff on the server
        if (world.isRemote) return ActionResultType.SUCCESS;
        // Make sure the tile is actually there
        GlassJarTile glassJar = this.getTile(world, pos);
        if (glassJar == null) return ActionResultType.PASS;
        // Open the jar if it's closed
        if (!state.get(OPEN)) {
            this.cycleOpened(world, state, pos);
            return ActionResultType.CONSUME;
        }

        ItemStack itemStack = player.getHeldItem(hand);
        Item item = itemStack.getItem();
        FluidTank tank = glassJar.getTank();
        // Forge handles water buckets n other stuff
        if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, hit.getFace())) return ActionResultType.CONSUME;
        else if (this.fillWithFluid(world, pos, player, hand, tank, item, itemStack)) return ActionResultType.CONSUME;
        else if (this.emptyFluid(world, pos, player, hand, tank, item, itemStack)) return ActionResultType.CONSUME;

        this.cycleOpened(world, state, pos);
        return ActionResultType.CONSUME;
    }

    private boolean fillWithFluid(World world, BlockPos pos, PlayerEntity player, Hand hand, FluidTank tank, Item item, ItemStack itemStack) {
        FluidStack fluidStack;
        ItemStack empty;
        if (item.equals(Items.MILK_BUCKET)) {
            fluidStack = ((GlassJarFluid) Registry.MILK_FLUID.get()).getFluidStack();
            empty = Items.BUCKET.getDefaultInstance();
        } else if (item.equals(Items.POTION) && itemStack.getTag() != null) {
            boolean isWater = PotionUtils.getPotionFromItem(itemStack).equals(Potions.WATER);
            fluidStack = new FluidStack(isWater ? Fluids.WATER : Registry.GENERIC_POTION_FLUID.get(), ((GlassJarFluid) Registry.GENERIC_POTION_FLUID.get()).getVolume(), itemStack.getTag());
            empty = Items.GLASS_BOTTLE.getDefaultInstance();
        } else if (item.equals(Items.HONEY_BOTTLE) || item.equals(Items.DRAGON_BREATH)) {
            fluidStack = itemStack.getItem().equals(Items.DRAGON_BREATH) ? ((GlassJarFluid) Registry.DRAGON_BREATH_FLUID.get()).getFluidStack() : ((GlassJarFluid) Registry.HONEY_FLUID.get()).getFluidStack();
            empty = Items.GLASS_BOTTLE.getDefaultInstance();
        } else if ((item instanceof SoupItem || item.equals(Items.SUSPICIOUS_STEW)) && item.getRegistryName() != null) {
            ItemStack copiedStack = itemStack.copy();
            CompoundNBT nbt = copiedStack.getOrCreateTag();
            nbt.putString("Soup", item.getRegistryName().toString());
            fluidStack = new FluidStack(itemStack.getItem().equals(Items.RABBIT_STEW) ? Registry.RABBIT_STEW_FLUID.get() : itemStack.getItem().equals(Items.SUSPICIOUS_STEW) ? Registry.SUS_STEW_FLUID.get() : Registry.GENERIC_SOUP_FLUID.get(), ((GlassJarFluid) Registry.GENERIC_SOUP_FLUID.get()).getVolume(), copiedStack.getTag());
            empty = Items.BOWL.getDefaultInstance();
        } else return false;
        if (tank.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) <= 0) return false;

        this.changeFluidContainer(player, itemStack, empty, hand);
        player.addStat(Registry.FILL_GLASS_JAR_STAT);
        world.playSound(null, pos, empty.getItem() == Items.BUCKET ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        tank.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private boolean emptyFluid(World world, BlockPos pos, PlayerEntity player, Hand hand, FluidTank tank, Item item, ItemStack empty) {
        ItemStack filled;
        if (item.equals(Items.BUCKET)) {
            if (!this.canDrain(tank, FluidAttributes.BUCKET_VOLUME) || !this.hasFluid(tank, Registry.MILK_FLUID.get())) return false;
            filled = Items.MILK_BUCKET.getDefaultInstance();
        } else if (item.equals(Items.GLASS_BOTTLE)) {
            if (!this.canDrain(tank, GlassJarFluid.BOTTLE_VOLUME)) return false;
            if (this.hasFluid(tank, Registry.GENERIC_POTION_FLUID.get())) {
                CompoundNBT tag = tank.getFluid().getTag();
                filled = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), tag == null ? Potions.WATER : PotionUtils.getPotionTypeFromNBT(tag));
            } else if (this.hasFluid(tank, Fluids.WATER)) {
                filled = Items.POTION.getDefaultInstance(); // As bruce lee would say...
            } else if (this.hasFluid(tank, Registry.HONEY_FLUID.get())) {
                filled = Items.HONEY_BOTTLE.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.DRAGON_BREATH_FLUID.get())) {
                filled = Items.DRAGON_BREATH.getDefaultInstance();
            } else return false;
        } else if (item.equals(Items.BOWL)) {
            if (!this.canDrain(tank, GlassJarFluid.BOTTLE_VOLUME)) return false;
            if (this.hasFluid(tank, Registry.GENERIC_SOUP_FLUID.get())) {
                CompoundNBT tag = tank.getFluid().getTag();
                if (tag == null) return false;
                Item soup = ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString("Soup")));
                if (soup == null) return false;
                filled = soup.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.RABBIT_STEW_FLUID.get())) {
                filled = Items.RABBIT_STEW.getDefaultInstance();
            } else if (this.hasFluid(tank, Registry.SUS_STEW_FLUID.get())) {
                filled = Items.SUSPICIOUS_STEW.getDefaultInstance();
            } else return false;
        } else return false;

        this.changeFluidContainer(player, empty, filled, hand);
        player.addStat(Registry.USE_GLASS_JAR_STAT);
        world.playSound(null, pos, item.equals(Items.BUCKET) ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        tank.drain(item.equals(Items.BUCKET) ? FluidAttributes.BUCKET_VOLUME : GlassJarFluid.BOTTLE_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

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

    private boolean hasFluid(FluidTank tank, Fluid fluid) {
        return !tank.isEmpty() && tank.getFluid().getFluid().isEquivalentTo(fluid);
    }

    private boolean canDrain(FluidTank tank, int amount) {
        return tank.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount() != 0;
    }

    private void cycleOpened(World world, BlockState state, BlockPos pos) {
        world.setBlockState(pos, state.cycleValue(OPEN), 3);
        world.playSound(null, pos, state.get(OPEN) ? Registry.GLASS_JAR_CLOSE.get() : Registry.GLASS_JAR_OPEN.get(), SoundCategory.BLOCKS, 0.5F, MathHelper.nextFloat(world.rand, 0.85f, 1.15f));
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        // FIXME
        GlassJarTile glassJar = this.getTile(world, pos);
        if (glassJar != null) {
            return glassJar.getTank().getFluid().getFluid().getAttributes().getLuminosity();
        }
        return super.getLightValue(state, world, pos);
    }

    @Nullable
    private GlassJarTile getTile(IBlockReader world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof GlassJarTile) return ((GlassJarTile) tileEntity);
        return null;
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
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, OPEN);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        GlassJarTile tile = this.getTile(worldIn, pos);
        if (tile != null && this.hasFluid(tile.getTank(), Registry.DRAGON_BREATH_FLUID.get())) {
            for (int i = 0; i < stateIn.get(LEVEL); ++i) {
                // Taken from ender chest code
                int j = rand.nextInt(2) * 2 - 1;
                int k = rand.nextInt(2) * 2 - 1;
                double d0 = (double) pos.getX() + 0.5D + 0.25D * (double) j;
                double d1 = (float) pos.getY() + rand.nextFloat();
                double d2 = (double) pos.getZ() + 0.5D + (stateIn.get(OPEN) ? 0.5f : 0.25f) * (double) k;
                double d3 = rand.nextFloat() * (float) j;
                double d4 = ((double) rand.nextFloat() - 0.5D) * 0.125D;
                double d5 = rand.nextFloat() * (float) k;
                worldIn.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
            }
        }
    }
}
