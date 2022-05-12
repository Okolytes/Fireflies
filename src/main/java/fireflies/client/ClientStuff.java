package fireflies.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Java classloading forces us to put calls to the Minecraft class in here. (Since that class only exists on the client)
 */
public final class ClientStuff {

    public static boolean isGamePaused() {
        return Minecraft.getInstance().isGamePaused();
    }

    public static String getMyUsername() {
        return Minecraft.getInstance().getSession().getUsername();
    }

    /**
     * Taken from {@link World#calculateInitialSkylight} because {@link World#isDaytime} does not work when called from the client.
     *
     * @return Is it currently daytime in this dimension?
     */
    public static boolean isDayTime(World world) {
        final double d0 = 1.0D - (double) (world.getRainStrength(1.0F) * 5.0F) / 16.0D;
        final double d1 = 1.0D - (double) (world.getThunderStrength(1.0F) * 5.0F) / 16.0D;
        final double d2 = 0.5D + 2.0D * MathHelper.clamp(MathHelper.cos(world.func_242415_f(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        final int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !world.getDimensionType().doesFixedTimeExist() && skylightSubtracted < 4;
    }
}
