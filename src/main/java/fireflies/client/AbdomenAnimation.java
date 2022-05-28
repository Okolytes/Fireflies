package fireflies.client;

import fireflies.entity.FireflyEntity;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

public class AbdomenAnimation {
    private static final Random RANDOM = new Random();
    public final HashSet<FireflyEntity> fireflies = new HashSet<>();
    public final Frame[] frames;
    public final String name;
    @Nullable
    private final AbdomenAnimationProperties abdomenAnimationProperties;

    public AbdomenAnimation(String name, Frame[] frames) {
        this.frames = frames;
        this.name = name;
        this.abdomenAnimationProperties = name.endsWith("synced") ? new AbdomenAnimationProperties() : null;
    }

    public void tick() {
        final FireflyEntity[] copy = this.fireflies.toArray(new FireflyEntity[0]); // sometimes the fireflies set will be modified while we're iterating it
        if (this.abdomenAnimationProperties != null) {
            if (copy.length > 0) {
                this.animate(this.abdomenAnimationProperties, copy);
                for (FireflyEntity firefly : copy) {
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.frameCounter = this.abdomenAnimationProperties.frameCounter;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.glow = this.abdomenAnimationProperties.glow;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.delayTicks = this.abdomenAnimationProperties.delayTicks;
                }
            }
        } else {
            for (FireflyEntity firefly : copy) {
                this.animate(firefly.abdomenAnimationManager.abdomenAnimationProperties, firefly);
            }
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
            ap.delayTicks = MathHelper.nextInt(RANDOM, frame.delay[0], frame.delay[1]);
        }

        if (ap.frameCounter++ >= this.frames.length - 1) {
            ap.frameCounter = 0;

            for (FireflyEntity firefly : fireflies) {
                if (firefly == null) continue;

                firefly.particleManager.destroyAbdomenParticle();
                firefly.particleManager.spawnAbdomenParticle();

                // Hurt animation ended, return to previous animation
                if (this.name.equals("hurt")) {
                    firefly.abdomenAnimationManager.setAnimation(firefly.abdomenAnimationManager.prevAnimation);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AbdomenAnimation(" +
                "fireflies=" + this.fireflies.size() +
                ", name='" + this.name + '\'' +
                ", animationProperties=" + this.abdomenAnimationProperties +
                ", frames=" + this.frames.length +
                ')';
    }

    public static class Frame {
        public float glow;
        @Nullable
        public int[] delay;
    }
}

