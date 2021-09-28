namespace FireflyAbdomenAnimationBuilder
{
    public class Animation
    {
        public Delay[] Delays;
        public float[] Frames;

        public class Delay
        {
            public int Frame;
            public int Min;
            public int Max;
        }
    }
}