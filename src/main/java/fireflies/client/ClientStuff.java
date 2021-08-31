package fireflies.client;

import fireflies.Fireflies;
import fireflies.client.sound.FireflyFlightSound;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Java classloading forces us to put calls to the Minecraft class in here. (Since it only exists on the client)
 */
@Mod.EventBusSubscriber(modid = Fireflies.ID, value = Dist.CLIENT)
public class ClientStuff {
    public static void beginFireflyFlightSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundHandler().playOnNextTick(new FireflyFlightSound(fireflyEntity));
    }

    public static boolean isGamePaused() {
        return Minecraft.getInstance().isGamePaused();
    }
}
