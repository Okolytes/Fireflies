package fireflies.client.particle;

import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyAbdomenParticle extends SpriteTexturedParticle {
    private final FireflyEntity fireflyEntity;

    protected FireflyAbdomenParticle(ClientWorld clientWorld, double x, double y, double z, FireflyEntity fireflyEntity) {
        super(clientWorld, x, y, z);
        this.fireflyEntity = fireflyEntity;
        this.particleScale = fireflyEntity.isChild() ? 0.25f : 0.55f;
        this.particleAlpha = 0f;
    }

    @Override
    public void tick() {
        super.tick();

        this.particleAlpha = fireflyEntity.getGlowAlpha();
        this.setPosition(
                fireflyEntity.getPosX() - -fireflyEntity.getWidth() * 0.35f * (double) MathHelper.sin(fireflyEntity.renderYawOffset * ((float) Math.PI / 180F)),
                fireflyEntity.getPosYEye() + fireflyEntity.abdomenParticlePositionOffset + 0.3f,
                fireflyEntity.getPosZ() + -fireflyEntity.getWidth() * 0.35f * (double) MathHelper.cos(fireflyEntity.renderYawOffset * ((float) Math.PI / 180F)));

        // Destroy when the alpha reaches 0
        this.age = 0;
        if ((this.particleAlpha <= 0f && !fireflyEntity.getGlowIncreasing()) || !fireflyEntity.isAlive()) {
            this.setExpired();
        }
    }

    @Override
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
            if (entity == null || !entity.isAlive())
                return null;

            FireflyAbdomenParticle fireflyAbdomenParticle = new FireflyAbdomenParticle(clientWorld, x, y, z, (FireflyEntity) entity);
            fireflyAbdomenParticle.selectSpriteRandomly(this.iAnimatedSprite);
            return fireflyAbdomenParticle;
        }
    }
}
