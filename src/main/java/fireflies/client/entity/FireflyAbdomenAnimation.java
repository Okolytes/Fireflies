package fireflies.client.entity;

import fireflies.entity.FireflyEntity;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

public class FireflyAbdomenAnimation {
    private static final Random RANDOM = new Random();
    public final HashSet<FireflyEntity> fireflies = new HashSet<>();
    public final FireflyAbdomenAnimationFrame[] frames;
    public final String name;
    public final boolean synced;
    @Nullable
    private final FireflyAbdomenAnimationProperties animationProperties;

    public FireflyAbdomenAnimation(String name, FireflyAbdomenAnimationFrame[] frames) {
        this.frames = frames;
        this.name = name;
        this.synced = name.endsWith("synced");
        this.animationProperties = this.synced ? new FireflyAbdomenAnimationProperties() : null;
    }

    public void animate() {
        if (this.animationProperties != null) {
            // todo this is scuffed
            this.animate(this.animationProperties, this.fireflies.toArray(new FireflyEntity[0]));
            for (FireflyEntity firefly : this.fireflies) {
                firefly.animationManager.animationProperties.frameCounter = this.animationProperties.frameCounter;
                firefly.animationManager.animationProperties.glow = this.animationProperties.glow;
                firefly.animationManager.animationProperties.delayTicks = this.animationProperties.delayTicks;
            }
        } else {
            for (FireflyEntity firefly : this.fireflies) {
                this.animate(firefly.animationManager.animationProperties, firefly);
            }
        }
    }

    private void animate(FireflyAbdomenAnimationProperties ap, FireflyEntity... fireflies) {
        // Tick down the delay timer, pausing the animation at this frame.
        if (ap.delayTicks > 0) {
            ap.delayTicks--;
            return;
        }

        // Reset the frame counter once we're on the last frame
        if (ap.frameCounter >= this.frames.length) {
            ap.frameCounter = 0;
        }

        if (ap.frameCounter == 0) {
            for (FireflyEntity firefly : fireflies) { // todo this is scuffed
                firefly.particleManager.resetAbdomenParticle();
                firefly.playGlowSound();
            }
        }

        final FireflyAbdomenAnimationFrame frame = this.frames[ap.frameCounter];
        ap.glow = frame.glow;

        if (frame.delay != null) {
            ap.delayTicks = MathHelper.nextInt(RANDOM, frame.delay[0], frame.delay[1]);
        }

        ap.frameCounter++;
    }

    public static class FireflyAbdomenAnimationFrame {
        public float glow;
        @Nullable
        public int[] delay;
    }
}

