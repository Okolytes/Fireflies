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

        // outside the for loop so each firefly does not impact the odds..
        boolean calmSyncedTryStartIncreasing = Math.random() > 0.95f;
        boolean calmSyncedTryStartDecreasing = Math.random() > 0.95f;
        boolean starryNightSyncedTryStartIncreasing = Math.random() > 0.925f;

        for (FireflyEntity fireflyEntity : calmSyncedFireflies.syncedFireflies) {
            if (fireflyEntity.world.isRemote || !fireflyEntity.isAlive()) {
                continue;
            }

            float calmSyncedSpeed = calmSyncedFireflies.modifyAmount(0.075f, -0.05f);
            if (calmSyncedFireflies.glowAlpha < 0.8f && calmSyncedFireflies.glowAlpha > 0.2f) {
                calmSyncedSpeed = calmSyncedFireflies.modifyAmount(0.02f, -0.01f);
            }

            calmSyncedFireflies.glowAlpha += calmSyncedSpeed;
            if (calmSyncedFireflies.glowAlpha <= 0) {
                calmSyncedFireflies.glowAlpha = 0;
                if (calmSyncedTryStartIncreasing) {
                    calmSyncedFireflies.glowIncreasing = true;
                    fireflyEntity.spawnAbdomenParticle();
                }
            } else if (calmSyncedFireflies.glowAlpha >= 1) {
                calmSyncedFireflies.glowAlpha = 1;
                if (calmSyncedTryStartDecreasing) {
                    calmSyncedFireflies.glowIncreasing = false;
                }
            }

            fireflyEntity.setGlowAlpha(calmSyncedFireflies.glowAlpha);
            fireflyEntity.setGlowIncreasing(calmSyncedFireflies.glowIncreasing);
        }

      for (FireflyEntity fireflyEntity : starryNightSyncedFireflies.syncedFireflies) {
          if (fireflyEntity.world.isRemote || !fireflyEntity.isAlive()) {
              continue;
          }

          starryNightSyncedFireflies.glowAlpha += starryNightSyncedFireflies.modifyAmount(0.3f, -0.25f);
          if (starryNightSyncedFireflies.glowAlpha <= 0) {
              starryNightSyncedFireflies.glowAlpha = 0;
              if (starryNightSyncedTryStartIncreasing) {
                  starryNightSyncedFireflies.glowIncreasing = true;
                  fireflyEntity.spawnAbdomenParticle();
              }
          } else if (starryNightSyncedFireflies.glowAlpha >= 1) {
              starryNightSyncedFireflies.glowAlpha = 1;
              starryNightSyncedFireflies.glowIncreasing = false;
          }

          fireflyEntity.setGlowAlpha(starryNightSyncedFireflies.glowAlpha);
          fireflyEntity.setGlowIncreasing(starryNightSyncedFireflies.glowIncreasing);
      }
    }
}
