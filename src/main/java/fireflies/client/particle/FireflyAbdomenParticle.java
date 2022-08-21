package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenParticle extends SimpleAnimatedParticle {
    private final FireflyEntity firefly;

    public FireflyAbdomenParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, float yAccel, FireflyEntity firefly) {
        super(world, x, y, z, sprites, yAccel);
        this.firefly = firefly;
        firefly.particleManager.abdomenParticle = this;
        this.setAlpha(0f);
        this.setSprite(0);
        this.quadSize = 0.45f;
        // Something's probably gone wrong if it has existed for this long.
        this.lifetime = 1000;

    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.firefly.particleManager.destroyAbdomenParticle();
            return;
        }

        this.alpha = this.firefly.abdomenAnimationManager.abdomenAnimationProperties.glow;

        // Keep at the exact point of the abdomen
        final double[] pos = this.firefly.particleManager.getAbdomenParticlePos();
        this.setPos(pos[0], pos[1], pos[2]);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240; // fullbright
    }

    public void setSprite(int idx) {
        this.setSprite(this.sprites.get(idx, 4)); // magic number: how many sprites this particle has, minus one.
    }

    @OnlyIn(Dist.CLIENT)
    private static abstract class AbstractAbdomenParticleFactory<T extends ParticleOptions> implements ParticleProvider<T> {
        protected final SpriteSet sprites;

        public AbstractAbdomenParticleFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(T particleData, ClientLevel clientLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            final Entity entity = clientLevel.getEntity(((FireflyParticleData.AbstractFireflyParticleData) particleData).fireflyId);
            return !(entity instanceof FireflyEntity) || !entity.isAlive() ? null : new FireflyAbdomenParticle(clientLevel, x, y, z, sprites, 0, (FireflyEntity) entity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenParticleFactory extends AbstractAbdomenParticleFactory<FireflyParticleData.Abdomen> {
        public AbdomenParticleFactory(SpriteSet sprites) {
            super(sprites);
        }
    }
}