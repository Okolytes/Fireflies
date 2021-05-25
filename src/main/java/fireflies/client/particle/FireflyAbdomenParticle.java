package fireflies.client.particle;

import fireflies.entity.firefly.FireflyAbdomenParticleData;
import fireflies.entity.firefly.FireflyEntity;
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
        this.maxAge = 1000; // Somethings probably gone wrong if its existed for this long.
    }

    @Override
    public void tick() {
        super.tick();

        this.particleAlpha = this.fireflyEntity.glowAlpha;

        double[] pos = this.fireflyEntity.abdomenParticlePos();
        this.setPosition(pos[0], pos[1], pos[2]);

        // Destroy when the alpha reaches 0 (or firefly gets removed)
        if ((this.particleAlpha <= 0f && !this.fireflyEntity.glowIncreasing) || !this.fireflyEntity.isAlive()) {
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
    public static class Factory implements IParticleFactory<FireflyAbdomenParticleData> {
        private final IAnimatedSprite iAnimatedSprite;

        public Factory(IAnimatedSprite iAnimatedSprite) {
            this.iAnimatedSprite = iAnimatedSprite;
        }

        @Override
        public Particle makeParticle(FireflyAbdomenParticleData fireflyAbdomenParticleData, ClientWorld clientWorld, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Entity entity = clientWorld.getEntityByID(fireflyAbdomenParticleData.fireflyId);
            if (entity == null || !entity.isAlive() || !(entity instanceof FireflyEntity))
                return null;

            FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(this.iAnimatedSprite);
            fireflyAbdomenParticle.setAlphaF(0);
            return fireflyAbdomenParticle;
        }
    }
}
