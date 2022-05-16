package fireflies.client;

import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FireflyAbdomenAnimationManager {
    public static final HashSet<FireflyAbdomenAnimation> ANIMATIONS = new HashSet<>();
    public static final HashMap<String, FireflyEntity> WANTS_IN = new HashMap<>();
    public static final HashMap<String, FireflyEntity> WANTS_OUT = new HashMap<>();
    public final FireflyAbdomenAnimationProperties animationProperties = new FireflyAbdomenAnimationProperties();
    private final FireflyEntity firefly;
    @Nullable
    public String curAnimation;
    @Nullable
    public String prevAnimation;

    public FireflyAbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
    }

    @SubscribeEvent
    public static void tickAnimations(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START /* Runs on both START and END */ && !Minecraft.getInstance().isGamePaused() && Minecraft.getInstance().player != null) {
            ANIMATIONS.forEach(FireflyAbdomenAnimation::tick);
        }
    }

    /**
     * @param name null = no animation
     */
    public void setAnimation(@Nullable String name) {
        if (Objects.equals(name, this.curAnimation)) {
            return;
        }

        boolean flag = false;
        for (FireflyAbdomenAnimation animation : ANIMATIONS) {
            final boolean contains = animation.fireflies.contains(this.firefly);
            if (animation.name.equals(name) && !contains) {
                WANTS_IN.put(animation.name, this.firefly);
                this.prevAnimation = this.curAnimation;
                this.curAnimation = name;
                this.resetAnimationProperties();
                // No need to continue iterating
                if (flag) {
                    return;
                }
            } else if (contains) {
                flag = true;
                WANTS_OUT.put(animation.name, this.firefly);
                if (name == null) {
                    this.prevAnimation = this.curAnimation;
                    this.curAnimation = null;
                    this.resetAnimationProperties();
                    return;
                }
            }
        }
    }

    private void resetAnimationProperties() {
        this.animationProperties.delayTicks = 0;
        this.animationProperties.glow = 0f;
        this.animationProperties.frameCounter = 0;
    }
}
