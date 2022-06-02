package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

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
                this.firefly.getPosX() - -this.firefly.getWidth() * 0.25f * MathHelper.sin(this.firefly.renderYawOffset * ((float) Math.PI / 180F)),
                this.firefly.getPosYEye() + this.abdomenParticlePositionOffset + (this.firefly.hasIllumerin() ? 0.6f : 0.3f),
                this.firefly.getPosZ() + -this.firefly.getWidth() * 0.25f * MathHelper.cos(this.firefly.renderYawOffset * ((float) Math.PI / 180F))
        };
    }

    /**
     * Spawns the appropiate abdomen particle for the firefly at the exact location of the abdomen
     */
    public void spawnAbdomenParticle() {
        if (this.abdomenParticle == null) {
            final double[] pos = this.getAbdomenParticlePos();
            this.firefly.world.addOptionalParticle(new FireflyParticleData.Abdomen(this.firefly.getEntityId()), true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    public void destroyAbdomenParticle() {
        if (this.abdomenParticle != null) {
            this.abdomenParticle.setExpired();
            this.abdomenParticle = null;
        }
    }

    /**
     * @return The appropiate dust particle for this firefly.
     */
    public IParticleData getDustParticle() {
        return new FireflyParticleData.Dust(this.firefly.getEntityId());
    }

    public void trySpawnDustParticles() {
        if (this.canSpawnDustParticles() && this.firefly.getRNG().nextFloat() < .1f * this.firefly.abdomenAnimationManager.abdomenAnimationProperties.glow) {
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
        final Vector3d look = this.firefly.getLookVec();
        this.firefly.world.addParticle(this.getDustParticle(), abdomenPos[0], abdomenPos[1], abdomenPos[2],
                -look.x, 0, -look.z);
    }
}
