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
        AbdomenAnimationManager.ANIMATIONS.clear();

        objectIn.forEach((rsc, element) -> AbdomenAnimationManager.ANIMATIONS.put(rsc.getPath(),
                new AbdomenAnimation(rsc.getPath(), GSON.fromJson(element, AbdomenAnimation.Frame[].class))));

        Fireflies.LOGGER.info("Loaded {} firefly abdomen animations", AbdomenAnimationManager.ANIMATIONS.size());
    }
}