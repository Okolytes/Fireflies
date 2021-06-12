package fireflies.fluid;

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
    private final int luminosity;

    public GlassJarFluid(String textureLocation, int volume){
        this(textureLocation, volume, 0xffffffff, 0);
    }

    public GlassJarFluid(String textureLocation, int volume, int color) {
        this(textureLocation, volume, color, 0);
    }

    public GlassJarFluid(String textureLocation, int volume, int color, int luminosity) {
        this.textureLocation = textureLocation;
        this.volume = volume;
        this.color = color;
        this.fluidStack = new FluidStack(this, volume);
        this.luminosity = luminosity;
    }

    public FluidStack getFluidStack() {
        return this.fluidStack;
    }

    public int getVolume() {
        return volume;
    }

    @Override
    protected FluidAttributes createAttributes() {
        return FluidAttributes.builder(new ResourceLocation(textureLocation), new ResourceLocation(textureLocation)).color(color).luminosity(luminosity).build(this);
    }
}
