package fireflies.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import fireflies.Fireflies;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

import static fireflies.client.AbdomenAnimationManager.ANIMATIONS;

public class AbdomenAnimationLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public AbdomenAnimationLoader() {
        super(GSON, "firefly_abdomen_animations");
    }

    public static void addFireflyAnimationsReloadListener() {
        ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(new AbdomenAnimationLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        pObject.forEach((rsc, element) -> {
            String name = rsc.getPath();
            AbdomenAnimation.Frame[] frames = GSON.fromJson(element, AbdomenAnimation.Frame[].class);
            if (ANIMATIONS.containsKey(name)) {
                ANIMATIONS.get(name).frames = frames;
            } else {
                ANIMATIONS.put(name, new AbdomenAnimation(name, frames));
            }
        });

        Fireflies.LOGGER.debug("Loaded abdomen animations: {}", ANIMATIONS.keySet().toString());
    }
}