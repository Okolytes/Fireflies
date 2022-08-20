package fireflies.client.sound;

import fireflies.Registry;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @see net.minecraft.client.resources.sounds.BeeSoundInstance
 */
@OnlyIn(Dist.CLIENT)
public class FireflyFlightSound extends AbstractTickableSoundInstance {
    private final FireflyEntity firefly;

    public FireflyFlightSound(FireflyEntity fireflyEntity) {
        super(Registry.FIREFLY_FLIGHT_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.firefly = fireflyEntity;
        this.x = fireflyEntity.getX();
        this.y = fireflyEntity.getY();
        this.z = fireflyEntity.getZ();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    public static void beginFireflyFlightSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundManager().queueTickingSound(new FireflyFlightSound(fireflyEntity));
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !this.firefly.isSilent();
    }

    @Override
    public void tick() {
        if (this.firefly.isAlive()) {
            this.x = this.firefly.getX();
            this.y = this.firefly.getY();
            this.z = this.firefly.getZ();
            final double v = this.firefly.getDeltaMovement().horizontalDistance();
            this.pitch = (float) Mth.clampedLerp(this.getMinPitch(), this.getMaxPitch(), v);
            this.volume = (float) Mth.clampedLerp(0.04f, 0.1f, v);
        } else {
            this.stop();
        }
    }

    private float getMinPitch() {
        return this.firefly.isBaby() ? 1.1F : 0.9F;
    }

    private float getMaxPitch() {
        return this.firefly.isBaby() ? 1.3F : 1.1F;
    }
}