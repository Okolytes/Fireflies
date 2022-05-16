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
     * @param newAnimation null = no animation
     */
    public void setAnimation(@Nullable String newAnimation) {
        if (Objects.equals(newAnimation, this.curAnimation)) {
            return;
        }

        this.prevAnimation = this.curAnimation;
        this.curAnimation = newAnimation;
        this.resetAnimationProperties();
        WANTS_OUT.put(this.curAnimation, this.firefly);
        if (newAnimation != null) {
            WANTS_IN.put(newAnimation, this.firefly);
        }
    }

    private void resetAnimationProperties() {
        this.animationProperties.delayTicks = 0;
        this.animationProperties.glow = 0f;
        this.animationProperties.frameCounter = 0;
    }
}
