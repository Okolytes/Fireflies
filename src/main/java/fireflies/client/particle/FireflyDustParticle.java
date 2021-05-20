package fireflies.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyDustParticle extends SpriteTexturedParticle {
    private final float rotSpeed;

    private FireflyDustParticle(ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
        this.particleScale = 0.1F * (this.rand.nextFloat() * 0.25F + 0.1F);
        this.maxAge = (int) (20f / (this.rand.nextFloat() * 0.8f + 0.2f)) + 16;
        this.rotSpeed = (this.rand.nextFloat() - 0.1F) * 0.05F;
        this.particleAngle = this.rand.nextFloat() * ((float) Math.PI * 2F);
        this.canCollide = false;
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            this.particleAngle += (float) Math.PI * this.rotSpeed * 2.0F;

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
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSetIn) {
            this.spriteSet = spriteSetIn;
        }

        @Override
        public Particle makeParticle(BasicParticleType basicParticleType, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            FireflyDustParticle particle = new FireflyDustParticle(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.selectSpriteRandomly(this.spriteSet);
            return particle;
        }
    }
}
