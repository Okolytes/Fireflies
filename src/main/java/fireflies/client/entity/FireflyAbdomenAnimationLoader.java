package fireflies.client.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import fireflies.Fireflies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public class FireflyAbdomenAnimationLoader extends JsonReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public FireflyAbdomenAnimationLoader() {
        super(GSON, "firefly_abdomen_animations");
    }

    public static void addFireflyAnimationsReloadListener() {
        ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new FireflyAbdomenAnimationLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        FireflyAbdomenAnimationManager.ABDOMEN_ANIMATIONS.clear();
        FireflyAbdomenAnimationManager.SYNCED_ANIMATORS.clear();

        objectIn.forEach((rsc, element) -> {
            final FireflyAbdomenAnimation animation = new FireflyAbdomenAnimation();
            final FireflyAbdomenAnimation.Frame[] frames = GSON.fromJson(element, FireflyAbdomenAnimation.Frame[].class);
            animation.name = rsc.getPath();
            animation.sync = animation.name.endsWith("synced");
            animation.frames = frames;
            FireflyAbdomenAnimationManager.ABDOMEN_ANIMATIONS.add(animation);

            if (animation.sync) {
                // Each synced animation gets its own animator which fireflies can be added to
                final FireflyAbdomenAnimator animator = new FireflyAbdomenAnimator(null);
                animator.animation = animation;
                FireflyAbdomenAnimationManager.SYNCED_ANIMATORS.add(animator);
            }
        });
        Fireflies.LOGGER.info("Loaded {} firefly animations, {} synced",
                FireflyAbdomenAnimationManager.ABDOMEN_ANIMATIONS.size(), FireflyAbdomenAnimationManager.SYNCED_ANIMATORS.size());
    }
}