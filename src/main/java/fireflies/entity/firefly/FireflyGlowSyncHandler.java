package fireflies.entity.firefly;

import fireflies.Fireflies;
import fireflies.client.ClientStuff;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT)
public class FireflyGlowSyncHandler {
    public static final FireflySyncedAnimation calmSyncedFireflies = new FireflySyncedAnimation();
    public static final FireflySyncedAnimation starryNightSyncedFireflies = new FireflySyncedAnimation();

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END /* Runs on both START and END */ || ClientStuff.isGamePaused())
            return;

        if (!calmSyncedFireflies.syncedFireflies.isEmpty()) {
            // outside the loop(s) so each firefly does not impact the odds
            boolean calmSyncedTryStartIncreasing = Math.random() <= 0.075f;
            boolean calmSyncedTryStartDecreasing = Math.random() <= 0.05f;

            for (FireflyEntity fireflyEntity : calmSyncedFireflies.syncedFireflies) {
                if (shouldUpdateGlowAnimation(fireflyEntity)) {
                    boolean isMiddling = calmSyncedFireflies.glowAlpha < 0.75f && calmSyncedFireflies.glowAlpha > 0.25f;
                    float increaseAmount = isMiddling ? 0.02f : 0.075f;
                    float decreaseAmount = isMiddling ? 0.01f : 0.05f;
                    calmSyncedFireflies.glowAnimation(fireflyEntity, increaseAmount, decreaseAmount, calmSyncedTryStartIncreasing, calmSyncedTryStartDecreasing);
                }
            }
        }

        if (!starryNightSyncedFireflies.syncedFireflies.isEmpty()) {
            boolean starryNightSyncedTryStartIncreasing = Math.random() <= 0.075f;
            boolean starryNightSyncedTryStartDecreasing = Math.random() <= 0.9f;

            for (FireflyEntity fireflyEntity : starryNightSyncedFireflies.syncedFireflies) {
                if (shouldUpdateGlowAnimation(fireflyEntity)) {
                    starryNightSyncedFireflies.glowAnimation(fireflyEntity, 0.3f, 0.25f, starryNightSyncedTryStartIncreasing, starryNightSyncedTryStartDecreasing);
                }
            }
        }
    }

    private static boolean shouldUpdateGlowAnimation(FireflyEntity fireflyEntity) {
        return fireflyEntity.isAlive() && fireflyEntity.world.isRemote;
    }

    public static class FireflySyncedAnimation {
        public ArrayList<FireflyEntity> syncedFireflies = new ArrayList<>();
        public float glowAlpha;
        public boolean glowIncreasing;

        /**
         * Do all of our logic for glowing
         *
         * @param fireflyEntity instance of the firefly
         * @see FireflyAbdomenAnimationHandler#glowAnimation
         */
        public void glowAnimation(FireflyEntity fireflyEntity, float increaseAmount, float decreaseAmount, boolean tryStartIncreasing, boolean tryStartDecreasing) {
            this.glowAlpha += this.modifyAmount(fireflyEntity, increaseAmount, decreaseAmount);
            if (this.glowAlpha <= 0) {
                this.glowAlpha = 0; // If it goes under or over 0 or 1 it'll wrap back around to being on/off, we don't want that
                if (tryStartIncreasing) {
                    this.glowIncreasing = true;
                    fireflyEntity.spawnAbdomenParticle();
                }
            } else if (this.glowAlpha >= 1) {
                this.glowAlpha = 1;
                if (tryStartDecreasing) {
                    this.glowIncreasing = false;
                    //fireflyEntity.world.playSound(fireflyEntity.getPosX(), fireflyEntity.getPosY(), fireflyEntity.getPosZ(), Registry.FIREFLY_GLOW.get(), SoundCategory.NEUTRAL,
                    //        MathHelper.nextFloat(fireflyEntity.world.rand, 0.25f, 0.5f), MathHelper.nextFloat(fireflyEntity.world.rand, 1f, 1.2f),
                    //        true);
                }
            }
            fireflyEntity.glowAlpha = this.glowAlpha;
            fireflyEntity.isGlowIncreasing = this.glowIncreasing;
        }

        public float modifyAmount(FireflyEntity fireflyEntity, float increaseAmount, float decreaseAmount) {
            return fireflyEntity.isGlowIncreasing ? increaseAmount / this.syncedFireflies.size() : -decreaseAmount / this.syncedFireflies.size();
        }
    }
}
