package fireflies.client;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.client.sound.FireflyFlightLoopSound;
import net.minecraft.client.Minecraft;

// Java classloading forces us to put calls to the Minecraft class in here I guess...
public class DoClientStuff {
    public void playFireflyLoopSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundHandler().playOnNextTick(new FireflyFlightLoopSound(fireflyEntity));
    }

    public boolean isGamePaused() {
        return Minecraft.getInstance().isGamePaused();
    }
}
