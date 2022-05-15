package fireflies.client;

import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Fireflies.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FireflyAbdomenAnimationManager {
    public static final HashSet<FireflyAbdomenAnimation> ANIMATIONS = new HashSet<>();
    public final FireflyAbdomenAnimationProperties animationProperties = new FireflyAbdomenAnimationProperties();
    private final FireflyEntity firefly;
    @Nullable
    private String currentAnimation;

    public FireflyAbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
    }

    @SubscribeEvent
    public static void syncFireflies(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START /* Runs on both START and END */ && !Minecraft.getInstance().isGamePaused() && Minecraft.getInstance().player != null) {
            ANIMATIONS.forEach(FireflyAbdomenAnimation::animate);
        }
    }

    /**
     * @param name if null, no animation will be used
     */
    public void setAnimation(@Nullable String name) {
        if (Objects.equals(name, this.currentAnimation)) {
            return;
        }

        for (FireflyAbdomenAnimation animation : ANIMATIONS) {
            if (animation.name.equals(name) && animation.fireflies.add(this.firefly)) {
                this.currentAnimation = name;
            } else if (animation.fireflies.remove(this.firefly) && name == null) {
                this.currentAnimation = null;
                return;
            }
        }
    }

    /**
     * Updates the abdomen animation accordingly to the fireflies environment and condition
     */
    public void updateAbdomenAnimation() {
        if (this.firefly.hasIllumerin()) {
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
                    this.setAnimation("calm_synced");
                } else {
                    this.setAnimation("starry_night_synced");
                }
                break;
            case TAIGA:
                this.setAnimation("starry_night_synced");
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
}
