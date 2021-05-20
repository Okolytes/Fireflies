package fireflies.entity.firefly;

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

    public void tick() {
        if (this.fireflyEntity.isAlive() ) {
            this.x = this.fireflyEntity.getPosX();
            this.y = this.fireflyEntity.getPosY();
            this.z = this.fireflyEntity.getPosZ();
            float f = MathHelper.sqrt(Entity.horizontalMag(this.fireflyEntity.getMotion()));
            if (f >= 0.01f) {
                this.pitch = MathHelper.lerp(MathHelper.clamp(f, this.getMinPitch(), this.getMaxPitch()), this.getMinPitch(), this.getMaxPitch());
                this.volume = MathHelper.lerp(MathHelper.clamp(f, 0.0F, 0.5F), 0.0F, 0.8F);
            } else {
                this.pitch = 0.0F;
                this.volume = 0.0F;
            }

        } else {
            this.finishPlaying();
        }
    }

    private float getMinPitch() {
        return this.fireflyEntity.isChild() ? 1.1F : 0.7F;
    }

    private float getMaxPitch() {
        return this.fireflyEntity.isChild() ? 1.5F : 1.1F;
    }

    @Override
    public boolean canBeSilent() {
        return true;
    }

    @Override
    public boolean shouldPlaySound() {
        return !this.fireflyEntity.isSilent();
    }
}