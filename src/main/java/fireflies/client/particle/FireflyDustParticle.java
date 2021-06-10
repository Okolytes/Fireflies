package fireflies.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyDustParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteWithAge;
    private final boolean redstone;
    private final float rotSpeed;

    private FireflyDustParticle(ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite spriteWithAge, boolean redstone) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
        this.redstone = redstone;
        this.spriteWithAge = spriteWithAge;
        this.particleAngle = this.rand.nextFloat() * ((float) Math.PI * 2F);
        this.rotSpeed = (this.rand.nextFloat() - 0.1F) * 0.05F;
        this.canCollide = false;
        if (redstone) {
            this.particleScale *= 0.5F;
            int i = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
            this.maxAge = (int)Math.max((float)i * 2.5f, 1.0F);
            float f = (float) (Math.random() * 0.4F + 0.6F);
            float r = 0.97f;
            float g = 0.02f;
            float b = 0.01f;
            this.particleRed = ((float) (Math.random() * 0.2F) + 0.8F) * r * f;
            this.particleGreen = ((float) (Math.random() * 0.2F) + 0.8F) * g * f;
            this.particleBlue = ((float) (Math.random() * 0.2F) + 0.8F) * b * f;
            this.selectSpriteWithAge(spriteWithAge);
        } else {
            this.maxAge = (int) (20f / (this.rand.nextFloat() * 0.8f + 0.2f)) + 32;
            this.particleScale = 0.1F * (this.rand.nextFloat() * 0.25F + 0.1F);
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
                this.selectSpriteWithAge(spriteWithAge);
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
    public static class DustParticleFactory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public DustParticleFactory(IAnimatedSprite spriteSetIn) {
            this.spriteSet = spriteSetIn;
        }

        @Override
        public Particle makeParticle(BasicParticleType basicParticleType, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FireflyDustParticle(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DustRedstoneParticleFactory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public DustRedstoneParticleFactory(IAnimatedSprite spriteSetIn) {
            this.spriteSet = spriteSetIn;
        }

        @Override
        public Particle makeParticle(BasicParticleType basicParticleType, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FireflyDustParticle(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, true);
        }
    }
}
