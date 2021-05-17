package fireflies.entity.firefly;

import java.util.ArrayList;

public class FireflySyncedAnimation {
    public ArrayList<FireflyEntity> syncedFireflies = new ArrayList<>();
    public float glowAlpha;
    public boolean glowIncreasing;

    public float modifyAmount(float increaseAmount, float decreaseAmount) {
        return this.glowIncreasing ? increaseAmount / syncedFireflies.size() : decreaseAmount / syncedFireflies.size();
    }
}
