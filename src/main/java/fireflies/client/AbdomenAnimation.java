package fireflies.client;

import com.google.common.base.MoreObjects;
import fireflies.entity.FireflyEntity;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

public class AbdomenAnimation {
    private static final Random RANDOM = new Random();
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
        // ultra scuffed way of avoiding CME
//        final ImmutableList<FireflyEntity> copy = ImmutableList.copyOf(this.fireflies);
//        final FireflyEntity[] copy1 = new FireflyEntity[copy.size()];
//        for (int i = 0; i < copy.size(); i++) {
//            FireflyEntity firefly = copy.get(i);
//            if (firefly != null) { // if we just use .toArray(new FireflyEntity[0]); we could get ArrayStoreException
//                copy1[i] = firefly;
//            }
//        }
        final FireflyEntity[] copy1 = this.fireflies.toArray(new FireflyEntity[0]);
        if (this.abdomenAnimationProperties != null) {
            if (copy1.length > 0) {
                this.animate(this.abdomenAnimationProperties, copy1);
                for (FireflyEntity firefly : copy1) {
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.frameCounter = this.abdomenAnimationProperties.frameCounter;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.glow = this.abdomenAnimationProperties.glow;
                    firefly.abdomenAnimationManager.abdomenAnimationProperties.delayTicks = this.abdomenAnimationProperties.delayTicks;
                }
            }
        } else for (FireflyEntity firefly : copy1) {
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
        return MoreObjects.toStringHelper(this)
                .add("frames count", frames.length)
                .add("name", name)
                .add("abdomenAnimationProperties", abdomenAnimationProperties)
                .toString();
    }

    public static class Frame {
        public float glow;
        @Nullable
        public int[] delay;
    }
}

