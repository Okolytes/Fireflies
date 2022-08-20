package fireflies.client;

import fireflies.entity.FireflyEntity;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.HashSet;

public class AbdomenAnimation {
    private static final RandomSource RANDOM = RandomSource.create(694201337);
    public final HashSet<FireflyEntity> fireflies = new HashSet<>();
    public final String name;
    public Frame[] frames;
    @Nullable
    private final AbdomenAnimationProperties abdomenAnimationProperties;

    public AbdomenAnimation(String name, Frame[] frames) {
        this.frames = frames;
        this.name = name;
        this.abdomenAnimationProperties = name.endsWith("synced") ? new AbdomenAnimationProperties() : null;
    }

    public void tick() {
        final FireflyEntity[] copy = this.fireflies.toArray(new FireflyEntity[0]);
        if (this.abdomenAnimationProperties != null) {
            if (copy.length > 0) {
                this.animate(this.abdomenAnimationProperties, copy);
                for (FireflyEntity firefly : copy) {
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.frameCounter = this.abdomenAnimationProperties.frameCounter;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.glow = this.abdomenAnimationProperties.glow;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.delayTicks = this.abdomenAnimationProperties.delayTicks;
                }
            }
        } else for (FireflyEntity firefly : copy) {
            this.animate(firefly.abdomenAnimationManager.abdomenAnimationProperties, firefly);
        }
    }

    private void animate(AbdomenAnimationProperties ap, FireflyEntity... fireflies) {
        // Tick down the delay timer, pausing the animation at this frame.
        if (ap.delayTicks > 0) {
            ap.delayTicks--;
            return;
        }

        final Frame frame = this.frames[ap.frameCounter];
        ap.glow = frame.glow;

        if (frame.delay != null) {
            ap.delayTicks = Mth.nextInt(RANDOM, frame.delay[0], frame.delay[1]);
        }

        if (ap.frameCounter++ >= this.frames.length - 1) {
            ap.frameCounter = 0;
        }

        for (FireflyEntity firefly : fireflies) {
            if (firefly == null) continue;

            if (frame.particleIndex != -1 && firefly.particleManager.abdomenParticle != null) {
                firefly.particleManager.abdomenParticle.setSprite(frame.particleIndex);
            }

            if (ap.frameCounter == 0) {
                firefly.particleManager.destroyAbdomenParticle();
                firefly.particleManager.spawnAbdomenParticle();

                // Hurt animation ended, return to previous animation
                if (this.name.equals("hurt")) {
                    firefly.abdomenAnimationManager.setAnimation(firefly.abdomenAnimationManager.prevAnimation);
                }
            }
        }
    }

    public static class Frame {
        public float glow;
        @Nullable
        public int[] delay;
        public int particleIndex = -1;
    }
}

