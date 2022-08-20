package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyDustParticle extends TextureSheetParticle {
    private final float rotSpeed;

    private FireflyDustParticle(FireflyEntity fireflyEntity, ClientLevel clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteWithAge) {
        super(clientWorld, x, y, z, xSpeed, ySpeed, zSpeed);
        this.rotSpeed = Mth.nextFloat(this.random, 0.05f, 0.1f);
        this.lifetime = Mth.nextInt(this.random, 50, 100);
        this.quadSize = Mth.nextFloat(this.random,  0.025f, 0.04f);
        this.pickSprite(spriteWithAge);
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 240; // fullbright
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.oRoll = this.roll;
            this.roll += this.rotSpeed;

            this.move(this.xd, this.yd, this.zd);
            this.yd -= 0.05f;
            this.yd = Math.max(this.yd, -0.05f);

            if (this.onGround) {
                this.remove();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static abstract class AbstractDustParticleFactory<T extends ParticleOptions> implements ParticleProvider<T> {
        protected final SpriteSet spriteSet;

        public AbstractDustParticleFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(T particleData, ClientLevel clientLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            final Entity entity = clientLevel.getEntity(((FireflyParticleData.AbstractFireflyParticleData) particleData).fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            final FireflyDustParticle fireflyDustParticle = new FireflyDustParticle((FireflyEntity) entity, clientLevel, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
            fireflyDustParticle.pickSprite(this.spriteSet);
            return fireflyDustParticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DustParticleFactory extends AbstractDustParticleFactory<FireflyParticleData.Dust> {
        public DustParticleFactory(SpriteSet spriteSet) {
            super(spriteSet);
        }
    }
}
