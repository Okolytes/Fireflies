package fireflies.misc;

import fireflies.Fireflies;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

public class GlassJarFluid extends EmptyFluid {
    public static final int BOTTLE_VOLUME = 250;

    private final String textureLocation;
    private final FluidStack fluidStack;
    private final int volume;
    private final int color;
    private final byte luminosity;

    public GlassJarFluid(String textureLocation, int volume) {
        this(textureLocation, volume, 0xffffffff, (byte) 0);
    }

    public GlassJarFluid(String textureLocation, int volume, int color) {
        this(textureLocation, volume, color, (byte) 0);
    }

    public GlassJarFluid(String textureLocation, int volume, byte luminosity) {
        this(textureLocation, volume, 0xffffffff, luminosity);
    }

    public GlassJarFluid(String textureLocation, int volume, int color, byte luminosity) {
        this.textureLocation = String.format("%s:block/%s", Fireflies.ID, textureLocation);
        this.volume = volume;
        this.color = color;
        this.fluidStack = new FluidStack(this, volume);
        this.luminosity = luminosity;
    }

    public FluidStack getFluidStack() {
        return this.fluidStack;
    }

    public int getVolume() {
        return this.volume;
    }

    @Override
    protected FluidAttributes createAttributes() {
        return FluidAttributes.builder(new ResourceLocation(this.textureLocation + "_side"), new ResourceLocation(this.textureLocation + "_top")).color(this.color).luminosity(this.luminosity).build(this);
    }
}
