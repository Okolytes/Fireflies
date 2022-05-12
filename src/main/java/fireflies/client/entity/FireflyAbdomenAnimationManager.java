package fireflies.client.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import fireflies.Fireflies;
import fireflies.client.ClientStuff;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class FireflyAbdomenAnimationManager {
    public static final HashSet<Animation> ANIMATIONS = new HashSet<>();
    public static final HashSet<AbdomenAnimator> SYNCHRONIZED_ANIMATORS = new HashSet<>();
    private final FireflyEntity firefly;
    public AbdomenAnimator animator;

    public FireflyAbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
        this.animator = new AbdomenAnimator(firefly);
    }

    public static void syncFireflies(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START /* Runs on both START and END */ && !ClientStuff.isGamePaused() && Minecraft.getInstance().player != null) { //todo test on server
            SYNCHRONIZED_ANIMATORS.forEach(AbdomenAnimator::animate);
        }
    }

    public void setAnimation(@Nullable String name) {
//        if (name == null) {
//            if (SYNCHRONIZED_ANIMATORS.containsValue(this.animator)) {
//                this.removeFromAnimator();
//            } else {
//                this.animator.animation = null;
//            }
//        } else if (this.animator.animation == null || !this.animator.animation.name.equals(name)) { // Don't bother if the current animation is equal to the one passed
//            if (SYNCHRONIZED_ANIMATORS.containsKey(name)) {
//                this.animator = SYNCHRONIZED_ANIMATORS.get(name);
//                this.animator.fireflies.add(this.firefly);
//            } else {
//                if (SYNCHRONIZED_ANIMATORS.containsValue(this.animator)) {
//                    this.removeFromAnimator();
//                    this.animator = new AbdomenAnimator(this.firefly);
//                }
//
//                for (Animation animation : ANIMATIONS) {
//                    if (animation.name.equals(name)) {
//                        this.animator.animation = animation;
//                    }
//                }
//            }
//        }
    }

    /**
     * Remove this firefly from our current AbdomenAnimator
     */
    public void removeFromAnimator() {
        this.animator.fireflies.remove(this.firefly);
    }

    /**
     * Updates the abdomen animation based on its circumstances
     */
    public void updateAbdomenAnimation() {
        if (this.firefly.hasIllumerin(true)) {
            this.setAnimation("illuminated");
            return;
        }

        // It's scary in the void...
        if (this.firefly.getPosition().getY() <= 0 || this.firefly.getPosition().getY() >= 256) {
            this.setAnimation("frantic");
            return;
        }

        // Set the animation based on the current biome.
        final Biome currentBiome = this.firefly.world.getBiome(this.firefly.getPosition());
        switch (currentBiome.getCategory()) {
            case SWAMP:
                this.setAnimation("calm");
                break;
            case FOREST:
                // todo is this performant?
                final Optional<RegistryKey<Biome>> biomeRegistryKey = this.firefly.world.func_242406_i(this.firefly.getPosition());
                if (Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST)) || Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST_HILLS))) {
                    this.setAnimation("calm_synchronized");
                } else {
                    this.setAnimation("starry_night_synchronized");
                }
                break;
            case TAIGA:
                this.setAnimation("starry_night_synchronized");
                break;
            case PLAINS:
                this.setAnimation("starry_night");
                break;
            case NETHER:
            case THEEND:
                this.setAnimation("frantic");
                break;
            case DESERT:
                this.setAnimation("slow_dim");
                break;
            case JUNGLE:
                this.setAnimation("quick_blinks");
                break;
            default:
                this.setAnimation("default");
                break;
        }
    }

    public static class AbdomenAnimator {
        /**
         * A set of all fireflies that are using this animator.
         * Synced fireflies will share one instance corresponding to each registered synchronized animation, regular fireflies have their own instances
         */
        public final HashSet<FireflyEntity> fireflies = new HashSet<>();
        /**
         * The AbdomenAnimator instances created for the synchronized animations shouldn't have this field null
         */
        @Nullable
        public Animation animation;
        public float glow;
        private int frameCounter;
        /**
         * How many frames the animation is currently paused for
         */
        private int delayTicks;

        public AbdomenAnimator(@Nullable FireflyEntity firefly) {
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

            final Animation.Frame frame = this.animation.frames[this.frameCounter];
            this.glow = frame.glow;

            if (frame.delay != null) {
                this.delayTicks = MathHelper.nextInt(Fireflies.RANDOM, frame.delay[0], frame.delay[1]);
            }

            this.frameCounter++;
        }
    }

    public static class Animation {
        public Frame[] frames;
        public String name;
        public boolean sync;

        public static class Frame {
            public float glow;
            @Nullable
            public int[] delay;
        }
    }


    public static class FireflyAnimationsLoader extends JsonReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        public FireflyAnimationsLoader() {
            super(GSON, "firefly_animations");
        }

        public static void addFireflyAnimationsReloadListener() {
            ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new FireflyAnimationsLoader());
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
            FireflyAbdomenAnimationManager.ANIMATIONS.clear();
            FireflyAbdomenAnimationManager.SYNCHRONIZED_ANIMATORS.clear();

            objectIn.forEach((rsc, element) -> {
                final Animation animation = new Animation();
                final Animation.Frame[] frames = GSON.fromJson(element, Animation.Frame[].class);
                animation.name = rsc.getPath();
                animation.sync = animation.name.endsWith("synchronized");
                animation.frames = frames;
                FireflyAbdomenAnimationManager.ANIMATIONS.add(animation);

                if (animation.sync) {
                    // Each synced animation gets its own AbdomenAnimator which fireflies can be added to
                    //FireflyAbdomenAnimationManager.SYNCHRONIZED_ANIMATORS.put(animation.name, new AbdomenAnimator(null));
                }
            });
            Fireflies.LOGGER.info("Loaded {} firefly animations", FireflyAbdomenAnimationManager.ANIMATIONS.size());
        }
    }
}
