package fireflies.entity.firefly;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class FireflyAbdomenSync {
    public static float calmGlowTime;
    public static float starryNightGlowTime;

    private static boolean calmAnimationFlag;
    private static boolean starryNightAnimationFlag;

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.ClientTickEvent event) {
        // Calm
        calmGlowTime += calmAnimationFlag ? 0.02f : -0.01f;
        if (calmGlowTime <= 0) {
            calmGlowTime = 0;
            if (Math.random() > 0.95f) {
                calmAnimationFlag = true;
            }
        } else if (calmGlowTime >= 1) {
            calmGlowTime = 1;
            calmAnimationFlag = false;
        }

        // Starry night
        starryNightGlowTime += starryNightAnimationFlag ? 0.05f : -0.01f;
        if (starryNightGlowTime <= 0) {
            starryNightGlowTime = 0;
            if (Math.random() > 0.95f) {
                starryNightAnimationFlag = true;
            }
        } else if (starryNightGlowTime >= 1) {
            starryNightGlowTime = 1;
            starryNightAnimationFlag = false;
        }
    }
}
