package fireflies.client;

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
        FireflyAbdomenAnimationManager.ANIMATIONS.clear();

        objectIn.forEach((rsc, element) -> FireflyAbdomenAnimationManager.ANIMATIONS.put(rsc.getPath(),
                new FireflyAbdomenAnimation(rsc.getPath(), GSON.fromJson(element, FireflyAbdomenAnimation.FireflyAbdomenAnimationFrame[].class))));

        Fireflies.LOGGER.info("Loaded {} firefly abdomen animations", FireflyAbdomenAnimationManager.ANIMATIONS.size());
    }
}