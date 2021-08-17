package fireflies.item;

import fireflies.Fireflies;
import fireflies.Registry;
import fireflies.block.GlassJarTile;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GlassJarBlockItem extends BlockItem {
    public GlassJarBlockItem() {
        super(Registry.GLASS_JAR.get(), new Item.Properties().group(ItemGroup.REDSTONE));
    }

    public static void registerItemModelProperty() {
        ItemModelsProperties.registerProperty(Registry.GLASS_JAR_BLOCKITEM.get(), new ResourceLocation(Fireflies.MOD_ID, "filled"), (stack, world, wielder) -> {
            CompoundNBT nbt = stack.getChildTag("BlockEntityTag");
            if (nbt == null || nbt.getInt("Amount") <= 0) return 0f;

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.tryCreate(nbt.getString("FluidName")));
            if (fluid == null) return 0f;

            return (float) nbt.getInt("Amount") / GlassJarTile.CAPACITY;
        });
    }

    @SubscribeEvent
    public static void registerGlassJarColor(final ColorHandlerEvent.Item event) {
        event.getItemColors().register((stack, color) -> {
            if (color == 0) return -1; // Don't colour the first layer
            CompoundNBT nbt = stack.getChildTag("BlockEntityTag");
            if (nbt == null || nbt.getInt("Amount") <= 0) return -1;

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.tryCreate(nbt.getString("FluidName")));
            if (fluid == null) return -1;

            if (fluid.isEquivalentTo(Registry.GENERIC_POTION_FLUID.get())) {
                return PotionUtils.getPotionColor(PotionUtils.getPotionTypeFromNBT(nbt.getCompound("Tag")));
            } else if (fluid.isEquivalentTo(Registry.GENERIC_SOUP_FLUID.get())) {
                return nbt.getCompound("Tag").getString("Soup").equals("minecraft:beetroot_soup") ? 0xFFA4272C : fluid.getAttributes().getColor();
            }
            return fluid.getAttributes().getColor();
        }, Registry.GLASS_JAR_BLOCKITEM.get());
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        CompoundNBT nbt = stack.getChildTag("BlockEntityTag");
        if (nbt == null || nbt.getInt("Amount") <= 0) return false;

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.tryCreate(nbt.getString("FluidName")));
        // todo recognise if the container item has an enchantment glint
        return fluid != null && fluid.isEquivalentTo(Registry.GENERIC_POTION_FLUID.get());
    }
}
