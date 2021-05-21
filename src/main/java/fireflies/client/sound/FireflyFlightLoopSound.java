package fireflies.client.sound;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.setup.FirefliesRegistration;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// Taken directly from the BeeSound class
@OnlyIn(Dist.CLIENT)
public class FireflyFlightLoopSound extends TickableSound {
    private final FireflyEntity fireflyEntity;

    public FireflyFlightLoopSound(FireflyEntity fireflyEntity) {
        super(FirefliesRegistration.FIREFLY_FLIGHT_LOOP.get(), SoundCategory.NEUTRAL);
        this.fireflyEntity = fireflyEntity;
        this.x = fireflyEntity.getPosX();
        this.y = fireflyEntity.getPosY();
        this.z = fireflyEntity.getPosZ();
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0F;
    }

    @Override
    public boolean canBeSilent() {
        return true;
    }

    @Override
    public boolean shouldPlaySound() {
        return !this.fireflyEntity.isSilent();
    }

    @Override
    public void tick() {
        if (this.fireflyEntity.isAlive() ) {
            this.x = this.fireflyEntity.getPosX();
            this.y = this.fireflyEntity.getPosY();
            this.z = this.fireflyEntity.getPosZ();
            double v = Math.sqrt(Entity.horizontalMag(fireflyEntity.getMotion()));
            this.pitch = (float) MathHelper.clampedLerp(this.getMinPitch(), this.getMaxPitch(), v);
            this.volume = (float) MathHelper.clampedLerp(0.03f, 0.1f, v);
        } else {
            this.finishPlaying();
        }
    }

    private float getMinPitch() {
        return this.fireflyEntity.isChild() ? 1.1F : 0.9F;
    }

    private float getMaxPitch() {
        return this.fireflyEntity.isChild() ? 1.3F : 1.1F;
    }
}