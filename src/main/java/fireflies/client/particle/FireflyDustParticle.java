package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyDustParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteWithAge;
    private final boolean redstone;
    private final float rotSpeed;

    private FireflyDustParticle(FireflyEntity fireflyEntity, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite spriteWithAge, boolean redstone) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
        this.redstone = redstone;
        this.spriteWithAge = spriteWithAge;
        this.particleAngle = this.rand.nextFloat() * ((float) Math.PI * 2f);
        this.rotSpeed = (this.rand.nextFloat() - 0.1f) * 0.05f;
        if (redstone) {
            this.particleScale *= 0.5f;
            final int i = (int) (8.0f / (this.rand.nextFloat() * 0.8f + 0.2f));
            this.maxAge = (int) Math.max((float) i * 2.5f, 1.0f);
            final float f = this.rand.nextFloat() * 0.4f + 0.6f;
            final float r = 0.97f;
            final float g = 0.02f;
            final float b = 0.01f;
            this.particleRed = ((this.rand.nextFloat() * 0.2f) + 0.8f) * r * f;
            this.particleGreen = ((this.rand.nextFloat() * 0.2f) + 0.8f) * g * f;
            this.particleBlue = ((this.rand.nextFloat() * 0.2f) + 0.8f) * b * f;
            this.selectSpriteWithAge(spriteWithAge);
        } else {
            this.maxAge = (int) (20f / (this.rand.nextFloat() * 0.8f + 0.2f)) + 32;
            this.particleScale = 0.1f * (this.rand.nextFloat() * 0.25f + 0.1f);
            if (fireflyEntity.hasIllumerin(true)) {
                this.particleRed = 0.9f;
                this.particleGreen = 0.55f;
                this.particleBlue = 0.18f;
            }
            this.selectSpriteRandomly(spriteWithAge);
        }
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public float getScale(float scaleFactor) {
        return this.redstone ? this.particleScale * MathHelper.clamp(((float) this.age + scaleFactor) / (float) this.maxAge * 32.0F, 0.0F, 1.0F) : this.particleScale;
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
            if (this.redstone) {
                this.selectSpriteWithAge(this.spriteWithAge);
            }
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

            final FireflyDustParticle fireflyDustParticle = new FireflyDustParticle((FireflyEntity) entity, clientWorld, x, y, z, xSpeed, ySpeed, zSpeed, this.iAnimatedSprite, false);
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

    @OnlyIn(Dist.CLIENT)
    public static class DustRedstoneParticleFactory extends AbstractDustParticleFactory<FireflyParticleData.DustRedstone> {
        public DustRedstoneParticleFactory(IAnimatedSprite iAnimatedSprite) {
            super(iAnimatedSprite);
        }
    }
}
