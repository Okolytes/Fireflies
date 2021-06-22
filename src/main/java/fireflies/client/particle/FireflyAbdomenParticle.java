package fireflies.client.particle;

import fireflies.entity.FireflyEntity;
import fireflies.misc.FireflyParticleData;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenParticle extends SpriteTexturedParticle {
    private final FireflyEntity fireflyEntity;

    protected FireflyAbdomenParticle(ClientWorld clientWorld, double x, double y, double z, FireflyEntity fireflyEntity) {
        super(clientWorld, x, y, z);
        this.fireflyEntity = fireflyEntity;
        this.particleScale = fireflyEntity.isChild() ? 0.2f : 0.45f;
        // Somethings probably gone wrong if its existed for this long.
        this.maxAge = fireflyEntity.isRedstoneCoated(true) ? Integer.MAX_VALUE : 1000;
        fireflyEntity.abdomenParticle = this;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.fireflyEntity.isRedstoneCoated(true)) {
            this.particleAlpha = this.fireflyEntity.glowAlpha;
        }

        // Keep at the exact point of the abdomen
        double[] pos = this.fireflyEntity.getAbdomenParticlePos();
        this.setPosition(pos[0], pos[1], pos[2]);

        // Destroy when the alpha reaches 0, or firefly dies
        if ((this.particleAlpha <= 0f && !this.fireflyEntity.isGlowIncreasing) || !this.fireflyEntity.isAlive()) {
            this.setExpired();
        }
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        return 240; // fullbright
    }
    
    @OnlyIn(Dist.CLIENT)
    public static class AbdomenParticleFactory implements IParticleFactory<FireflyParticleData.Abdomen> {
        private final IAnimatedSprite iAnimatedSprite;

        public AbdomenParticleFactory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(FireflyParticleData.Abdomen abdomen, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Entity entity = clientWorld.getEntityByID(abdomen.fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(iAnimatedSprite);
            fireflyAbdomenParticle.setAlphaF(0);
            return fireflyAbdomenParticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenRedstoneParticleFactory implements IParticleFactory<FireflyParticleData.AbdomenRedstone> {
        private final IAnimatedSprite iAnimatedSprite;

        public AbdomenRedstoneParticleFactory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(FireflyParticleData.AbdomenRedstone abdomenRedstone, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Entity entity = clientWorld.getEntityByID(abdomenRedstone.fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(iAnimatedSprite);
            fireflyAbdomenParticle.setAlphaF(1f);
            return fireflyAbdomenParticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AbdomenIllumerinParticleFactory implements IParticleFactory<FireflyParticleData.AbdomenIllumerin> {
        private final IAnimatedSprite iAnimatedSprite;

        public AbdomenIllumerinParticleFactory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(FireflyParticleData.AbdomenIllumerin abdomenIllumerin, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Entity entity = clientWorld.getEntityByID(abdomenIllumerin.fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(iAnimatedSprite);
            fireflyAbdomenParticle.setAlphaF(0f);
            return fireflyAbdomenParticle;
        }
    }
}