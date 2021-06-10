package fireflies.fluid;

import net.minecraft.fluid.EmptyFluid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

public class GlassJarFluid extends EmptyFluid {
    public static final int BOTTLE_VOLUME = 250;

    private final String flowingTexture;
    private final String stillTexture;
    private final FluidStack fluidStack;
    private final int volume;
    private final int color;

    public GlassJarFluid(String stillTexture, String flowingTexture, int volume) {
        this(stillTexture, flowingTexture, volume, 0xffffffff);
    }

    public GlassJarFluid(String stillTexture, String flowingTexture, int volume, int color) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.volume = volume;
        this.color = color;
        this.fluidStack = new FluidStack(this, volume);
    }

    public FluidStack getFluidStack() {
        return this.fluidStack;
    }

    public int getVolume() {
        return volume;
    }

    @Override
    protected FluidAttributes createAttributes() {
        return FluidAttributes.builder(new ResourceLocation(stillTexture), new ResourceLocation(flowingTexture)).color(color).build(this);
    }
}
