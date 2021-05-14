package fireflies.entity.firefly;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
// TODO fix this...
public class FireflyAbdomenSync {
    public static float calmGlowTime;
    public static float starryNightGlowTime;

    private static boolean calmAnimationFlag;
    private static boolean starryNightAnimationFlag;

    @SubscribeEvent
    public static void calmSynchronized(TickEvent.ClientTickEvent event) {
        calmGlowTime += calmAnimationFlag ? 0.02f : -0.02f;
        if (calmGlowTime <= 0) {
            calmGlowTime = 0;
            calmAnimationFlag = true;
        } else if (calmGlowTime >= 1) {
            calmGlowTime = 1;
            calmAnimationFlag = false;
        }
    }

    @SubscribeEvent
    public static void starryNightSynchronized(TickEvent.ClientTickEvent event) {
        starryNightGlowTime += starryNightAnimationFlag ? 0.05f : -0.01f;
        if (starryNightGlowTime <= 0) {
            starryNightGlowTime = 0;
            starryNightAnimationFlag = true;
        } else if (starryNightGlowTime >= 1) {
            starryNightGlowTime = 1;
            starryNightAnimationFlag = false;
        }
    }
}
