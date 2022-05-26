package fireflies.client;

public class FireflyAbdomenAnimationProperties {
    public float glow;
    public int frameCounter;
    /**
     * How many frames the animation is currently paused for
     */
    public int delayTicks;

    @Override
    public String toString() {
        return "FireflyAbdomenAnimationProperties{" +
                "glow=" + this.glow +
                ", frameCounter=" + this.frameCounter +
                ", delayTicks=" + this.delayTicks +
                '}';
    }
}