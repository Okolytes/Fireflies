package fireflies.client.entity;

import fireflies.entity.FireflyEntity;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;

public class FireflyAbdomenAnimator {
    private static final Random RANDOM = new Random();
    /**
     * A set of all fireflies that are using this animator.
     * Synced fireflies will share one animator instance that exists for each registered synced animation, regular fireflies have their own animator instances
     * So, every firefly will have an animator but not every animator will have a firefly
     */
    public final HashSet<FireflyEntity> fireflies = new HashSet<>();
    /**
     * Animator instances created for synced animations should never have this field null
     */
    @Nullable
    public FireflyAbdomenAnimation animation;
    public float glow;
    private int frameCounter;
    /**
     * How many frames the animation is currently paused for
     */
    private int delayTicks;

    public FireflyAbdomenAnimator(@Nullable FireflyEntity firefly) {
        if (firefly != null) {
            this.fireflies.add(firefly);
        }
    }

    public void animate() {
        if (this.animation == null || this.fireflies.size() < 1) {
            this.glow = 0f;
            this.frameCounter = 0;
            this.delayTicks = 0;
            return;
        }

        // Tick down the delay timer, pausing the animation at this frame.
        if (this.delayTicks > 0) {
            this.delayTicks--;
            return;
        }

        // Reset the frame counter once we're on the last frame
        if (this.frameCounter >= this.animation.frames.length) {
            this.frameCounter = 0;
        }

        if (this.frameCounter == 0) {
            this.fireflies.forEach(firefly -> {
                firefly.particleManager.resetAbdomenParticle();
                firefly.playGlowSound();
            });
        }

        final FireflyAbdomenAnimation.Frame frame = this.animation.frames[this.frameCounter];
        this.glow = frame.glow;

        if (frame.delay != null) {
            this.delayTicks = MathHelper.nextInt(RANDOM, frame.delay[0], frame.delay[1]);
        }

        this.frameCounter++;
    }
}

