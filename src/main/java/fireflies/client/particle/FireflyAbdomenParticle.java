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
public class FireflyAbdomenParticle extends SimpleAnimatedParticle {
    private final FireflyEntity firefly;

    public FireflyAbdomenParticle(ClientWorld world, double x, double y, double z, IAnimatedSprite sprites, float yAccel, FireflyEntity firefly) {
        super(world, x, y, z, sprites, yAccel);
        this.firefly = firefly;
        firefly.particleManager.abdomenParticle = this;
        this.setAlphaF(0f);
        this.setSprite(0);
        // Something's probably gone wrong if it has existed for this long.
        this.maxAge = 1000;
    }

    @Override
    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.age++ >= this.maxAge) {
            this.firefly.particleManager.destroyAbdomenParticle();
            return;
        }

        // Setting it in the tick() methods because the scale will need to change accordingly as a baby firefly grows (or shrinks?) // todo figure out what the fuck i meant by "shrink"
        this.particleScale = this.firefly.isChild() ? 0.2f : 0.45f;
        this.particleAlpha = this.firefly.abdomenAnimationManager.abdomenAnimationProperties.glow;

        // Keep at the exact point of the abdomen
        final double[] pos = this.firefly.particleManager.getAbdomenParticlePos();
        this.setPosition(pos[0], pos[1], pos[2]);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 240; // fullbright
    }

    public void setSprite(int idx) {
        this.setSprite(this.spriteWithAge.get(idx, 4)); // magic number: how many sprites this particle has, minus one.
    }

    @OnlyIn(Dist.CLIENT)
    private static abstract class AbstractAbdomenParticleFactory<T extends IParticleData> implements IParticleFactory<T> {
        protected final IAnimatedSprite sprites;

        public AbstractAbdomenParticleFactory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(T particleData, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            final Entity entity = clientWorld.getEntityByID(((FireflyParticleData.AbstractFireflyParticleData) particleData).fireflyId);
            return !(entity instanceof FireflyEntity) || !entity.isAlive() ? null : new FireflyAbdomenParticle(clientWorld, x, y, z, sprites, 0, (FireflyEntity) entity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenParticleFactory extends AbstractAbdomenParticleFactory<FireflyParticleData.Abdomen> {
        public AbdomenParticleFactory(IAnimatedSprite sprites) {
            super(sprites);
        }
    }
}