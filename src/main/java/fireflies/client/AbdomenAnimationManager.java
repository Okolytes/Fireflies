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
import java.util.Objects;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbdomenAnimationManager {
    public static final HashMap<String, AbdomenAnimation> ANIMATIONS = new HashMap<>();
    public final AbdomenAnimationProperties abdomenAnimationProperties = new AbdomenAnimationProperties();
    private final FireflyEntity firefly;
    @Nullable
    public String curAnimation;
    @Nullable
    public String prevAnimation;

    public AbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
    }

    @SubscribeEvent
    public static void tickAnimations(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isGamePaused() && mc.world != null && mc.player != null) {
            ANIMATIONS.values().forEach(AbdomenAnimation::tick);
        }
    }


    /**
     * @param newAnimation null = no animation
     */
    public void setAnimation(@Nullable String newAnimation) {
        final boolean hurt = "hurt".equals(newAnimation);
        if (hurt && "hurt".equals(this.curAnimation)) {
            this.resetAbdomenAnimationProperties();
            return;
        }

        if (Objects.equals(newAnimation, this.curAnimation)) {
            return;
        }

        if (this.curAnimation != null) {
            ANIMATIONS.get(this.curAnimation).fireflies.remove(this.firefly);
        }

        this.firefly.particleManager.destroyAbdomenParticle();
        if (newAnimation != null) {
            ANIMATIONS.get(newAnimation).fireflies.add(this.firefly);
            if (!hurt) {
                this.firefly.particleManager.spawnAbdomenParticle();
            }
        }

        this.prevAnimation = this.curAnimation;
        this.curAnimation = newAnimation;
        this.resetAbdomenAnimationProperties();
    }

    public void resetAbdomenAnimationProperties() {
        this.abdomenAnimationProperties.delayTicks = 0;
        this.abdomenAnimationProperties.glow = 0f;
        this.abdomenAnimationProperties.frameCounter = 0;
    }
}
