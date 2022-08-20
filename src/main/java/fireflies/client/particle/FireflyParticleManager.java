package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Client class for handling the abdomen particle
 */
public class FireflyParticleManager {
    private final FireflyEntity firefly;

    /**
     * The current abdomen particle this firefly has. (if any)
     */
    @Nullable
    public FireflyAbdomenParticle abdomenParticle;

    /**
     * The Y offset of where the abdomen particle be at any given moment.
     *
     * @see fireflies.client.model.FireflyModel#setRotationAngles
     */
    public float abdomenParticlePositionOffset;

    public FireflyParticleManager(FireflyEntity fireflyEntity) {
        this.firefly = fireflyEntity;
    }

    /**
     * @return The precise position of where the abdomen particle should spawn / move to.
     */
    public double[] getAbdomenParticlePos() {
        return new double[] {
                this.firefly.getX() - -this.firefly.getBbWidth() * 0.5f * Mth.sin(this.firefly.yBodyRot * ((float) Math.PI / 180F)),
                this.firefly.getEyeY() + this.abdomenParticlePositionOffset + (this.firefly.hasIllumerin() ? 0.6f : 0.3f),
                this.firefly.getZ() + -this.firefly.getBbWidth() * 0.5f * Mth.cos(this.firefly.yBodyRot * ((float) Math.PI / 180F))
        };
    }

    /**
     * Spawns the appropiate abdomen particle for the firefly at the exact location of the abdomen
     */
    public void spawnAbdomenParticle() {
        if (this.abdomenParticle == null) {
            final double[] pos = this.getAbdomenParticlePos();
            this.firefly.level.addAlwaysVisibleParticle(new FireflyParticleData.Abdomen(this.firefly.getId()), true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    public void destroyAbdomenParticle() {
        if (this.abdomenParticle != null) {
            this.abdomenParticle.remove();
            this.abdomenParticle = null;
        }
    }

    /**
     * @return The appropiate dust particle for this firefly.
     */
    public ParticleOptions getDustParticle() {
        return new FireflyParticleData.Dust(this.firefly.getId());
    }

    public void trySpawnDustParticles() {
        if (this.canSpawnDustParticles() && this.firefly.getRandom().nextFloat() < .1f * this.firefly.abdomenAnimationManager.abdomenAnimationProperties.glow) {
            this.spawnDustParticle();
        }
    }

    public boolean canSpawnDustParticles() {
        return this.firefly.abdomenAnimationManager.abdomenAnimationProperties.glow > 0f
                && !this.firefly.isInvisible()
                && !Objects.equals(this.firefly.abdomenAnimationManager.curAnimation, "hurt");
    }

    public void spawnDustParticle() {
        final double[] abdomenPos = this.getAbdomenParticlePos();
        final Vec3 look = this.firefly.getLookAngle();
        this.firefly.level.addParticle(this.getDustParticle(), abdomenPos[0], abdomenPos[1], abdomenPos[2],
                -look.x, 0, -look.z);
    }
}
