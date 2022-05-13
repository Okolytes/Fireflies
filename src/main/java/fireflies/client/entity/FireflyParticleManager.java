package fireflies.client.entity;

import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

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
                this.firefly.getPosX() - -this.firefly.getWidth() * 0.35f * MathHelper.sin(this.firefly.renderYawOffset * ((float) Math.PI / 180F)),
                this.firefly.getPosYEye() + this.abdomenParticlePositionOffset + 0.3f,
                this.firefly.getPosZ() + -this.firefly.getWidth() * 0.35f * MathHelper.cos(this.firefly.renderYawOffset * ((float) Math.PI / 180F))
        };
    }

    /**
     * Spawns the appropiate abdomen particle for the firefly at the exact location of the abdomen
     */
    public void spawnAbdomenParticle() {
        if (this.firefly.world.isRemote && this.abdomenParticle == null) {
            final double[] pos = this.getAbdomenParticlePos();
            this.firefly.world.addOptionalParticle(this.firefly.hasIllumerin(true)
                            ? new FireflyParticleData.AbdomenIllumerin(this.firefly.getEntityId())
                            : new FireflyParticleData.Abdomen(this.firefly.getEntityId()),
                    true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    public void destroyAbdomenParticle() {
        if (this.firefly.world.isRemote && this.abdomenParticle != null) {
            this.abdomenParticle.setExpired();
            this.abdomenParticle = null;
        }
    }

    public void resetAbdomenParticle() {
        this.destroyAbdomenParticle();
        this.spawnAbdomenParticle();
    }

    /**
     * @return The appropiate dust particle for this firefly.
     */
    public IParticleData getDustParticle() {
        return new FireflyParticleData.Dust(this.firefly.getEntityId());
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        final Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.firefly.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    /**
     * Spawn falling particles every so often, at the abdomen's position. Falling angle depends on fireflies speed.
     * This is called every tick at {@link FireflyEntity#livingTick()}
     */
    public void spawnFallingDustParticles() {
        if (this.firefly.ticksExisted % 10 == 0 && this.firefly.animationManager.animator.glow > 0f && this.firefly.getRNG().nextFloat() > 0.33f && !this.firefly.isInvisible()) {
            // abdomens position, taken from SquidEntity#squirtInk()
            // just don't touch this I forgot how it works
            final Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.firefly.getPosX(), this.firefly.getPosY(), this.firefly.getPosZ());
            final Vector3d vector3d1 = this.rotateVector(new Vector3d(this.firefly.getRNG().nextFloat(), -1.0D, this.firefly.getRNG().nextFloat() * Math.abs(this.firefly.getMotion().getZ()) * 10 + 2));
            final Vector3d vector3d2 = vector3d1.scale(-5f + this.firefly.getRNG().nextFloat() * 2.0F);

            // Small random offset around the abdomen, baby fireflies don't have it
            final float offset = this.firefly.isChild() ? 0f : MathHelper.nextFloat(this.firefly.getRNG(), -0.2f, 0.2f);

            this.firefly.world.addParticle(this.getDustParticle(),
                    vector3d.x + offset, vector3d.y + (this.firefly.isChild() ? 1.1f : 1.35f) + offset, vector3d.z + offset,
                    vector3d2.x, vector3d2.y * -16, vector3d2.z);
        }
    }
}
