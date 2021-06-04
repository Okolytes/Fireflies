package fireflies.entity.firefly;

import java.util.ArrayList;

public class FireflySyncedAnimation {
    public ArrayList<FireflyEntity> syncedFireflies = new ArrayList<>();
    public float glowAlpha;
    public boolean glowIncreasing;

    /**
     * Do all of our logic for glowing
     *
     * @param fireflyEntity instance of the firefly
     * @see FireflyEntity#glowAnimation
     */
    public void glowAnimation(FireflyEntity fireflyEntity, float increaseAmount, float decreaseAmount, boolean tryStartIncreasing, boolean tryStartDecreasing) {
        this.glowAlpha += this.modifyAmount(fireflyEntity, increaseAmount, decreaseAmount);
        if (this.glowAlpha <= 0) {
            this.glowAlpha = 0; // If it goes under or over 0 or 1 it'll wrap back around to being on/off, we don't want that
            if (tryStartIncreasing) {
                this.glowIncreasing = true;
                fireflyEntity.spawnAbdomenParticle();
            }
        } else if (this.glowAlpha >= 1) {
            this.glowAlpha = 1;
            if (tryStartDecreasing) {
                this.glowIncreasing = false;
                //fireflyEntity.world.playSound(fireflyEntity.getPosX(), fireflyEntity.getPosY(), fireflyEntity.getPosZ(), Registry.FIREFLY_GLOW.get(), SoundCategory.NEUTRAL,
                //        MathHelper.nextFloat(fireflyEntity.world.rand, 0.25f, 0.5f), MathHelper.nextFloat(fireflyEntity.world.rand, 1f, 1.2f),
                //        true);
            }
        }
        fireflyEntity.glowAlpha = this.glowAlpha;
        fireflyEntity.isGlowIncreasing = this.glowIncreasing;
    }

    public float modifyAmount(FireflyEntity fireflyEntity, float increaseAmount, float decreaseAmount) {
        return fireflyEntity.isGlowIncreasing ? increaseAmount / this.syncedFireflies.size() : -decreaseAmount / this.syncedFireflies.size();
    }
}
