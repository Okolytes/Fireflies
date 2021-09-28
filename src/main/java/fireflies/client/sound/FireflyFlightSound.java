package fireflies.client.sound;

import fireflies.Registry;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * @see net.minecraft.client.audio.BeeSound
 */
@OnlyIn(Dist.CLIENT)
public class FireflyFlightSound extends TickableSound {
    private final FireflyEntity firefly;

    public FireflyFlightSound(FireflyEntity fireflyEntity) {
        super(Registry.FIREFLY_FLIGHT_LOOP.get(), SoundCategory.NEUTRAL);
        this.firefly = fireflyEntity;
        this.x = fireflyEntity.getPosX();
        this.y = fireflyEntity.getPosY();
        this.z = fireflyEntity.getPosZ();
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0F;
    }

    public static void beginFireflyFlightSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundHandler().playOnNextTick(new FireflyFlightSound(fireflyEntity));
    }

    @Override
    public boolean canBeSilent() {
        return true;
    }

    @Override
    public boolean shouldPlaySound() {
        return !this.firefly.isSilent();
    }

    @Override
    public void tick() {
        if (this.firefly.isAlive()) {
            this.x = this.firefly.getPosX();
            this.y = this.firefly.getPosY();
            this.z = this.firefly.getPosZ();
            final double v = Math.sqrt(Entity.horizontalMag(this.firefly.getMotion()));
            this.pitch = (float) MathHelper.clampedLerp(this.getMinPitch(), this.getMaxPitch(), v);
            this.volume = (float) MathHelper.clampedLerp(0.04f, 0.1f, v);
        } else {
            this.finishPlaying();
        }
    }

    private float getMinPitch() {
        return this.firefly.isChild() ? 1.1F : 0.9F;
    }

    private float getMaxPitch() {
        return this.firefly.isChild() ? 1.3F : 1.1F;
    }
}