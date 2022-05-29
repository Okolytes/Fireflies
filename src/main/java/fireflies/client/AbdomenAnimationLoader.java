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

import static fireflies.client.AbdomenAnimationManager.ANIMATIONS;

public class AbdomenAnimationLoader extends JsonReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public AbdomenAnimationLoader() {
        super(GSON, "firefly_abdomen_animations");
    }

    public static void addFireflyAnimationsReloadListener() {
        ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new AbdomenAnimationLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        objectIn.forEach((rsc, element) -> {
            String name = rsc.getPath();
            AbdomenAnimation.Frame[] frames = GSON.fromJson(element, AbdomenAnimation.Frame[].class);
            if (ANIMATIONS.containsKey(name)) {
                ANIMATIONS.get(name).frames = frames;
            } else {
                ANIMATIONS.put(name, new AbdomenAnimation(name, frames));
            }
        });

        Fireflies.LOGGER.debug("Loaded abdomen animations: {}", ANIMATIONS.toString());
    }
}