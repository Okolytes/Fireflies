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
public class FireflyAbdomenParticle extends SpriteTexturedParticle {
    private final FireflyEntity firefly;

    protected FireflyAbdomenParticle(ClientWorld clientWorld, double x, double y, double z, FireflyEntity fireflyEntity) {
        super(clientWorld, x, y, z);
        this.firefly = fireflyEntity;
        fireflyEntity.particleManager.abdomenParticle = this;
        // Something's probably gone wrong if it has existed for this long.
        this.maxAge = 10000;
    }

    @Override
    public void tick() {
        super.tick();

        // Setting it here because the scale will need to change accordingly as a baby firefly grows (or shrinks?) // todo figure out what the fuck i meant by "shrink"
        this.particleScale = this.firefly.isChild() ? 0.2f : 0.45f;
        this.particleAlpha = this.firefly.animationManager.animationProperties.glow;

        // Keep at the exact point of the abdomen
        final double[] pos = this.firefly.particleManager.getAbdomenParticlePos();
        this.setPosition(pos[0], pos[1], pos[2]);
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        return 240; // fullbright
    }

    @OnlyIn(Dist.CLIENT)
    private static abstract class AbstractAbdomenParticleFactory<T extends IParticleData> implements IParticleFactory<T> {
        protected final IAnimatedSprite iAnimatedSprite;

        public AbstractAbdomenParticleFactory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(T particleData, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            final Entity entity = clientWorld.getEntityByID(((FireflyParticleData.AbstractFireflyParticleData) particleData).fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            final FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(this.iAnimatedSprite);
            fireflyAbdomenParticle.setAlphaF(0);
            return fireflyAbdomenParticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenParticleFactory extends AbstractAbdomenParticleFactory<FireflyParticleData.Abdomen> {
        public AbdomenParticleFactory(IAnimatedSprite iAnimatedSprite) {
            super(iAnimatedSprite);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenIllumerinParticleFactory extends AbstractAbdomenParticleFactory<FireflyParticleData.AbdomenIllumerin> {
        public AbdomenIllumerinParticleFactory(IAnimatedSprite iAnimatedSprite) {
            super(iAnimatedSprite);
        }
    }
}