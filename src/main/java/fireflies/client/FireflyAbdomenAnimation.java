package fireflies.client;

import fireflies.entity.FireflyEntity;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
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

    public void tick() {
        // Avoiding the rare occasions we can get a ConcurrentModificationException while iterating this.fireflies (e.g. firefly dies and is removed from the set, or a new firefly added to the set)
        FireflyAbdomenAnimationManager.WANTS_OUT.entrySet().removeIf(entry -> entry.getKey().equals(this.name) && this.fireflies.remove(entry.getValue()));
        FireflyAbdomenAnimationManager.WANTS_IN.entrySet().removeIf(entry -> entry.getKey().equals(this.name) && this.fireflies.add(entry.getValue()));

        if (this.animationProperties != null) {
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

        // Last frame of animation
        if (ap.frameCounter >= this.frames.length) {
            // Reset the frame counter
            ap.frameCounter = 0;

            // Hurt animation ended, return to previous animation
            if (this.name.equals("hurt")) {
                for (FireflyEntity firefly : fireflies) {
                    firefly.animationManager.setAnimation(firefly.animationManager.prevAnimation);
                }
            }
        }

        if (ap.frameCounter == 0) {
            for (FireflyEntity firefly : fireflies) {
                if (Objects.equals(firefly.animationManager.curAnimation, "hurt")) {
                    firefly.particleManager.destroyAbdomenParticle();
                } else {
                    firefly.particleManager.resetAbdomenParticle();
                }
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

