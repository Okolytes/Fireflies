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
import java.util.*;

public class FireflyAbdomenAnimationManager {
    public static final HashMap<String, AbdomenAnimation> ANIMATIONS = new HashMap<>();
    public static final HashMap<String, AbdomenAnimator> SYNCHRONIZED_ANIMATORS = new HashMap<>();
    private final FireflyEntity firefly;
    public AbdomenAnimator animator;

    public FireflyAbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
        this.animator = new AbdomenAnimator(firefly);
    }

    public static void syncFireflies(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START /* Runs on both START and END */ && !ClientStuff.isGamePaused() && Minecraft.getInstance().player != null) { //todo test on server
            SYNCHRONIZED_ANIMATORS.forEach((s, animator) -> animator.animate());
        }
    }

    public void setAnimation(@Nullable String name) {
        if (name == null) {
            this.animator.animation = null;
            this.stopAnimating();
        } else if (this.animator.animation == null || !this.animator.animation.name.equals(name)) { // Don't bother if the current animation is equal to the one passed
            if (SYNCHRONIZED_ANIMATORS.containsKey(name)) {
                this.animator = SYNCHRONIZED_ANIMATORS.get(name);
                this.animator.fireflies.add(this.firefly);
            } else {
                if (SYNCHRONIZED_ANIMATORS.containsValue(this.animator)) {
                    this.stopAnimating();
                    this.animator = new AbdomenAnimator(this.firefly);
                }
                this.animator.animation = ANIMATIONS.get(name);
            }
        }
    }

    /**
     * Remove this firefly from our current AbdomenAnimator
     */
    public void stopAnimating() {
        this.animator.fireflies.remove(this.firefly);
    }

    /**
     * Updates the abdomen animation based on its circumstances
     */
    public void updateAbdomenAnimation() {
        if (this.firefly.redstoneManager.isRedstoneCoated(true)) {
            if (this.firefly.redstoneManager.searchTime > 0) {
                this.setAnimation("lamp_searching");
            } else {
                this.setAnimation("redstone");
            }
            return;
        }

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
                this.setAnimation("slow");
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
        public final HashSet<FireflyEntity> fireflies = new HashSet<>();
        @Nullable
        public AbdomenAnimation animation;
        public float glow;
        private int frame;
        /**
         * How many frames the animation is currently paused for
         */
        private int delay;

        public AbdomenAnimator(@Nullable FireflyEntity firefly) {
            if (firefly != null) {
                this.fireflies.add(firefly);
            }
        }

        public void animate() {
            if (this.animation == null || this.fireflies.size() < 1) {
                this.glow = 0;
                this.frame = 0;
                this.delay = 0;
                return;
            }

            // Tick down the delay timer, pausing the animation at this frame.
            if (this.delay > 0) {
                this.delay--;
                return;
            }

            // Reset the frame counter once we're on the last frame
            if (this.frame >= this.animation.frames.length) {
                this.frame = 0;
            }

            if (this.frame == 0) {
                this.fireflies.forEach(firefly -> {
                    firefly.particleManager.resetAbdomenParticle();
                    firefly.playGlowSound();
                });
            }

            // Assign the glow amount to its corresponding frame
            this.glow = this.animation.frames[this.frame];

            // Check if the animation has any delays for this frame, if so assign the delay timer a random amount between the given minimum and maximum
            Arrays.stream(this.animation.delays).filter(delay -> delay.frame == this.frame).forEach(delay -> this.delay = MathHelper.nextInt(Fireflies.RANDOM, delay.min, delay.max));

            this.frame++;
        }
    }

    public static class AbdomenAnimation {
        public Delay[] delays;
        public float[] frames;
        public boolean sync;
        public String name;

        public static class Delay {
            public int frame;
            public int min;
            public int max;
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
                final AbdomenAnimation animation = GSON.fromJson(element, AbdomenAnimation.class);
                animation.name = rsc.getPath();
                animation.sync = animation.name.endsWith("synchronized");
                FireflyAbdomenAnimationManager.ANIMATIONS.put(animation.name, animation);
                if (animation.sync) {
                    final AbdomenAnimator animator = new AbdomenAnimator(null);
                    animator.animation = animation;
                    FireflyAbdomenAnimationManager.SYNCHRONIZED_ANIMATORS.put(animation.name, animator);
                }
            });
            Fireflies.LOGGER.info("Loaded {} firefly animations", FireflyAbdomenAnimationManager.ANIMATIONS.size());
        }
    }
}
