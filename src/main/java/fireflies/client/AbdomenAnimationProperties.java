package fireflies.client;

import com.google.common.base.MoreObjects;

public class AbdomenAnimationProperties {
    public float glow;
    public int frameCounter;
    /**
     * How many frames the animation is currently paused for
     */
    public int delayTicks;


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("glow", glow)
                .add("frameCounter", frameCounter)
                .add("delayTicks", delayTicks)
                .toString();
    }
}