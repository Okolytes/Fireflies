package fireflies.client.entity;

import javax.annotation.Nullable;

public class FireflyAbdomenAnimation {
    public Frame[] frames;
    public String name;
    public boolean sync;

    public static class Frame {
        public float glow;
        @Nullable
        public int[] delay;
    }
}