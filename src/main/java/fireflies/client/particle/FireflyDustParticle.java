package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particles.IParticleData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyDustParticle extends SpriteTexturedParticle {
    private final float rotSpeed;

    private FireflyDustParticle(FireflyEntity fireflyEntity, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite spriteWithAge) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
        this.particleAngle = this.rand.nextFloat() * ((float) Math.PI * 2f);
        this.rotSpeed = (this.rand.nextFloat() - 0.1f) * 0.05f;
        this.maxAge = (int) (20f / (this.rand.nextFloat() * 0.8f + 0.2f)) + 32;
        this.particleScale = 0.1f * (this.rand.nextFloat() * 0.25f + 0.1f);
        if (fireflyEntity.hasIllumerin()) {
            this.particleRed = 0.8f;
            this.particleBlue = 0.1f;
        }
        this.selectSpriteRandomly(spriteWithAge);
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        return 240; // fullbright
    }

    @Override
    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.age++ >= this.maxAge) {
            this.setExpired();
        } else {
            this.prevParticleAngle = this.particleAngle;
            this.particleAngle += (float) Math.PI * this.rotSpeed * 1.25f;

            this.move(this.motionX, this.motionY, this.motionZ);
            this.motionY -= 0.025f;
            this.motionY = Math.max(this.motionY, -0.025f);

            // Kil once it touches the ground
            if (this.onGround) {
                this.setExpired();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static abstract class AbstractDustParticleFactory<T extends IParticleData> implements IParticleFactory<T> {
        protected final IAnimatedSprite iAnimatedSprite;

        public AbstractDustParticleFactory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(T particleData, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            final Entity entity = clientWorld.getEntityByID(((FireflyParticleData.AbstractFireflyParticleData) particleData).fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            final FireflyDustParticle fireflyDustParticle = new FireflyDustParticle((FireflyEntity) entity, clientWorld, x, y, z, xSpeed, ySpeed, zSpeed, this.iAnimatedSprite);
            fireflyDustParticle.selectSpriteRandomly(this.iAnimatedSprite);
            return fireflyDustParticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DustParticleFactory extends AbstractDustParticleFactory<FireflyParticleData.Dust> {
        public DustParticleFactory(IAnimatedSprite iAnimatedSprite) {
            super(iAnimatedSprite);
        }
    }
}
