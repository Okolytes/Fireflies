package fireflies.entity;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import java.util.Objects;
import java.util.Optional;

/**
 * Client class for animating the firefly abdomen flashing - takes the weight off the main firefly entity class a bit
 */
public class FireflyAbdomenAnimationHandler {
    private final FireflyEntity firefly;
    private int quickBlinksAnimationCount;

    public FireflyAbdomenAnimationHandler(FireflyEntity fireflyEntity) {
        this.firefly = fireflyEntity;
    }

    /**
     * Add / deplete the glow alpha, baby fireflies go a little faster.
     *
     * @return the amount to increase / decrease the glowAlpha by.
     */
    private float modifyAmount(float increaseAmount, float decreaseAmount) {
        return this.firefly.isGlowIncreasing ? (this.firefly.isChild() ? increaseAmount * 1.25f : increaseAmount) : (this.firefly.isChild() ? -decreaseAmount * 1.25f : -decreaseAmount);
    }

    /**
     * Handle logic for most of the abdomen animations
     *
     * @param increaseAmount           The amount in which the glow alpha should increase
     * @param decreaseAmount           The amount in which the glow alpha should decrease
     * @param startIncreasingChance    The chance for the glow alpha to begin increasing, once it's at 0.
     * @param startDecreasingChanceThe The chance for the glow alpha to begin decreasing, once it's at 1.
     * @see FireflyEntity#abdomenAnimation
     * @see FireflyEntity#glowAlpha
     * @see FireflyEntity#isGlowIncreasing
     */
    public void glowAnimation(float increaseAmount, float decreaseAmount, float startIncreasingChance, float startDecreasingChance) {
        this.firefly.glowAlpha += modifyAmount(increaseAmount, decreaseAmount);

        if (this.firefly.glowAlpha <= 0) {
            this.firefly.glowAlpha = 0; // If it goes under or over 0 or 1 it'll wrap back around to being on/off, we don't want that
            if (Math.random() <= startIncreasingChance) {
                this.firefly.isGlowIncreasing = true;
                this.firefly.spawnAbdomenParticle();
            }
        } else if (firefly.glowAlpha >= 1) {
            this.firefly.glowAlpha = 1;
            if (Math.random() <= startDecreasingChance) {
                this.firefly.isGlowIncreasing = false;
                this.firefly.playGlowSound();
            }
        }
    }

    /**
     * Handles the {@link FireflyAbdomenAnimation#QUICK_BLINKS} animation.
     */
    private void quickBlinksAnimation() {
        if (this.quickBlinksAnimationCount >= 2) {
            if (Math.random() > 0.035f) {
                return;
            }
            this.quickBlinksAnimationCount = 0;
        }

        this.firefly.glowAlpha += modifyAmount(0.3f, 0.25f);

        if (this.firefly.glowAlpha <= 0) {
            this.firefly.glowAlpha = 0;
            if (Math.random() <= 0.275f) {
                this.firefly.isGlowIncreasing = true;
                this.firefly.spawnAbdomenParticle();
                this.quickBlinksAnimationCount++;
            }
        } else if (this.firefly.glowAlpha >= 1) {
            this.firefly.glowAlpha = 1;
            this.firefly.isGlowIncreasing = false;
            this.firefly.playGlowSound();
        }
    }

    private void illuminatedAnimation() {
        this.firefly.glowAlpha += modifyAmount(0.1f, 0.1f);

        if (this.firefly.glowAlpha <= 0.15) {
            this.firefly.isGlowIncreasing = true;
            if (this.firefly.abdomenParticle != null) {
                this.firefly.abdomenParticle.setExpired();
            }
            this.firefly.spawnAbdomenParticle();
        } else if (firefly.glowAlpha >= 1) {
            this.firefly.glowAlpha = 1;
            this.firefly.isGlowIncreasing = false;
            this.firefly.playGlowSound();
        }
    }

    /**
     * Update the glow animation, this is done every tick.
     *
     * @see FireflyAbdomenAnimationHandler#glowAnimation
     */
    public void updateGlowAnimation() {
        if (firefly.abdomenAnimation == null) return;
        switch (firefly.abdomenAnimation) {
            case OFF:
                firefly.glowAlpha = 0;
                firefly.isGlowIncreasing = false;
                break;
            case ON:
                firefly.glowAlpha = 1f;
                firefly.isGlowIncreasing = false;
                break;
            case DEFAULT:
                this.glowAnimation(0.1f, 0.05f, 0.05f, 0.075f);
                break;
            case CALM:
                boolean isMiddling = firefly.glowAlpha < 0.75f && firefly.glowAlpha > 0.25f;
                float increaseAmount = isMiddling ? 0.02f : 0.075f;
                float decreaseAmount = isMiddling ? 0.01f : 0.05f;
                this.glowAnimation(increaseAmount, decreaseAmount, 0.075f, 0.05f);
                break;
            case STARRY_NIGHT:
                this.glowAnimation(0.3f, 0.25f, 0.075f, 0.95f);
                break;
            case FRANTIC:
                this.glowAnimation(0.35f, 0.4f, 0.2f, 0.35f);
                break;
            case SLOW:
                this.glowAnimation(0.05f, 0.015f, 0.025f, 0.025f);
                break;
            case QUICK_BLINKS:
                this.quickBlinksAnimation();
                break;
            case ILLUMINATED:
                this.illuminatedAnimation();
                break;
        }
    }


    /**
     * Set the fireflies abdomen animation, also changes the sync lists accordingly.
     *
     * @param newAnimation The new animation to change to.
     * @see FireflyEntity#abdomenAnimation
     */
    public void setAbdomenAnimation(FireflyAbdomenAnimation newAnimation) {
        if (firefly.abdomenAnimation == newAnimation) // Dont access the lists if we don't have to!
            return;

        switch (newAnimation) {
            case CALM_SYNCHRONIZED:
                FireflyGlowSyncHandler.starryNightSyncedFireflies.syncedFireflies.remove(firefly);
                if (!FireflyGlowSyncHandler.calmSyncedFireflies.syncedFireflies.contains(firefly)) {
                    FireflyGlowSyncHandler.calmSyncedFireflies.syncedFireflies.add(firefly);
                }
                break;
            case STARRY_NIGHT_SYNCHRONIZED:
                FireflyGlowSyncHandler.calmSyncedFireflies.syncedFireflies.remove(firefly);
                if (!FireflyGlowSyncHandler.starryNightSyncedFireflies.syncedFireflies.contains(firefly)) {
                    FireflyGlowSyncHandler.starryNightSyncedFireflies.syncedFireflies.add(firefly);
                }
                break;
            default:
                FireflyGlowSyncHandler.calmSyncedFireflies.syncedFireflies.remove(firefly);
                FireflyGlowSyncHandler.starryNightSyncedFireflies.syncedFireflies.remove(firefly);
                break;
        }
        firefly.abdomenAnimation = newAnimation;
    }

    /**
     * Update the abdomen animation based on the biome it's in, among other conditions, every tick.
     *
     * @see FireflyEntity#abdomenAnimation
     */
    public void updateAbdomenAnimation() {
        // The redstone firefly always has its abdomen on.
        if (firefly.isRedstoneCoated(true)) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.ON);
            return;
        }

        // Fireflies with illumerin have their own special animation.
        if (firefly.hasIllumerin(true)) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.ILLUMINATED);
            return;
        }

        // It's scary in the void...
        if (firefly.getPosition().getY() <= 0 || firefly.getPosition().getY() >= 256) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.FRANTIC);
            return;
        }

        // Set the animation based on the current biome.
        Biome currentBiome = firefly.world.getBiome(firefly.getPosition());
        switch (currentBiome.getCategory()) {
            case SWAMP:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM);
                break;
            case FOREST:
                Optional<RegistryKey<Biome>> biomeRegistryKey = firefly.world.func_242406_i(firefly.getPosition());
                if (Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST)) || Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST_HILLS))) {
                    this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM_SYNCHRONIZED);
                } else {
                    this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT_SYNCHRONIZED);
                }
                break;
            case TAIGA:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT_SYNCHRONIZED);
                break;
            case PLAINS:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT);
                break;
            case NETHER:
            case THEEND:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.FRANTIC);
                break;
            case DESERT:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.SLOW);
                break;
            case JUNGLE:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.QUICK_BLINKS);
                break;
            default:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.DEFAULT);
                break;
        }
    }
}
