package fireflies.entity.firefly;

import fireflies.Fireflies;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class FireflyGlowSync {
    public static final FireflySyncedAnimation calmSyncedFireflies = new FireflySyncedAnimation();
    public static final FireflySyncedAnimation starryNightSyncedFireflies = new FireflySyncedAnimation();

    @SubscribeEvent
    public static void serverTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) // Runs on both START and END
            return;

        // outside the loops so each firefly does not impact the odds
        boolean calmSyncedTryStartIncreasing = Math.random() <= 0.075f;
        boolean calmSyncedTryStartDecreasing = Math.random() <= 0.05f;
        boolean starryNightSyncedTryStartIncreasing = Math.random() <= 0.075f;
        boolean starryNightSyncedTryStartDecreasing = Math.random() <= 0.9f;

        for (FireflyEntity fireflyEntity : calmSyncedFireflies.syncedFireflies) {
            if (!shouldUpdateGlowAnimation(fireflyEntity)) {
                continue;
            }

            boolean isMiddling = calmSyncedFireflies.glowAlpha < 0.75f && calmSyncedFireflies.glowAlpha > 0.25f;
            float increaseAmount = isMiddling ? 0.02f : 0.05f;
            float decreaseAmount = isMiddling ? 0.01f : 0.025f;
            calmSyncedFireflies.glowAnimation(fireflyEntity, increaseAmount, decreaseAmount, calmSyncedTryStartIncreasing, calmSyncedTryStartDecreasing);
        }

      for (FireflyEntity fireflyEntity : starryNightSyncedFireflies.syncedFireflies) {
          if (!shouldUpdateGlowAnimation(fireflyEntity)) {
              continue;
          }

          starryNightSyncedFireflies.glowAnimation(fireflyEntity, 0.3f, 0.25f, starryNightSyncedTryStartIncreasing, starryNightSyncedTryStartDecreasing);
      }
    }

    private static boolean shouldUpdateGlowAnimation(FireflyEntity fireflyEntity){
        return fireflyEntity.isAlive() && !fireflyEntity.world.isRemote;
    }
}
