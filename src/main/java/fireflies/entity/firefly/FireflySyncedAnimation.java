package fireflies.entity.firefly;

import java.util.ArrayList;

public class FireflySyncedAnimation {
    public ArrayList<FireflyEntity> syncedFireflies = new ArrayList<>();
    public float glowAlpha;
    public boolean glowIncreasing;

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
            }
        }
        fireflyEntity.glowAlpha = this.glowAlpha;
        fireflyEntity.glowIncreasing = this.glowIncreasing;
    }

    public float modifyAmount(FireflyEntity fireflyEntity, float increaseAmount, float decreaseAmount) {
        return fireflyEntity.glowIncreasing ? increaseAmount / this.syncedFireflies.size() : -decreaseAmount / this.syncedFireflies.size();
    }
}
