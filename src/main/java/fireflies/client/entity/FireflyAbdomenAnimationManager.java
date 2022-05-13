package fireflies.client.entity;

import fireflies.client.ClientStuff;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

public class FireflyAbdomenAnimationManager {
    public static final HashSet<FireflyAbdomenAnimation> ABDOMEN_ANIMATIONS = new HashSet<>();
    public static final HashSet<FireflyAbdomenAnimator> SYNCED_ANIMATORS = new HashSet<>();
    private final FireflyEntity firefly;
    public FireflyAbdomenAnimator animator;

    public FireflyAbdomenAnimationManager(FireflyEntity firefly) {
        this.firefly = firefly;
        this.animator = new FireflyAbdomenAnimator(firefly);
    }

    public static void syncFireflies(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START /* Runs on both START and END */ && !ClientStuff.isGamePaused() && Minecraft.getInstance().player != null) { //todo test on server
            SYNCED_ANIMATORS.forEach(FireflyAbdomenAnimator::animate);
        }
    }

    public static void debugSetAnimation(ClientChatEvent event) { //todo
        if (event.getMessage().startsWith("sus ")) {
            final Entity pointedEntity = Minecraft.getInstance().pointedEntity;
            if (pointedEntity instanceof FireflyEntity) {
                ((FireflyEntity) pointedEntity).animationManager.setAnimation(event.getMessage().split(" ")[1]);
            }
        }
    }

    /**
     * @param name if null, no animation will be used
     */
    public void setAnimation(@Nullable String name) {
        if (name == null) {
            this.animator.fireflies.remove(this.firefly);
            if (this.animator.animation != null && !this.animator.animation.sync) { // If our animator hosts a synced animation, don't set it to null
                this.animator.animation = null;
            }
            return;
        }

        if (this.animator.animation == null || !this.animator.animation.name.equals(name)) {
            final Optional<FireflyAbdomenAnimator> syncedAnimator = SYNCED_ANIMATORS.stream().filter(animator -> animator.animation.name.equals(name)).findFirst();
            // If name is a synced animation, find its animator and add ourselves to it
            if (syncedAnimator.isPresent()) {
                syncedAnimator.get().fireflies.add(this.firefly);
                this.animator.fireflies.remove(this.firefly);
                if (this.animator.animation != null && !this.animator.animation.sync) { // If our animator hosts a synced animation, don't set it to null
                    this.animator.animation = null;
                }
                return;
            }

            if (SYNCED_ANIMATORS.contains(this.animator)) {
                this.animator.fireflies.remove(this.firefly);
            } else {
                this.animator = new FireflyAbdomenAnimator(this.firefly);
            }

            this.animator.animation = ABDOMEN_ANIMATIONS.stream().filter(animation -> animation.name.equals(name)).findFirst().orElse(null);
        }
    }

    /**
     * Updates the abdomen animation based on its circumstances
     */
    public void updateAbdomenAnimation() {
        if(true)return;
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
