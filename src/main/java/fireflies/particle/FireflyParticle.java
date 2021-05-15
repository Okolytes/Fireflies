package fireflies.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class FireflyParticle extends SpriteTexturedParticle {
    private final float rotSpeed;

    private FireflyParticle(ClientWorld worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.particleScale *= 0.15F;
        int i = (int) (8.0D / (Math.random()));
        this.maxAge = (int) Math.max((float) i, 2.0F);
        this.rotSpeed = ((float) Math.random() - 0.5F) * 0.1F;
        this.particleAngle = (float) Math.random() * ((float) Math.PI * 2F);
        this.particleGravity = 0.9f;
        this.canCollide = false;
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        return 240; // fullbright
    }

    public float getScale(float scaleFactor) {
        return this.particleScale * MathHelper.clamp(((float) this.age + scaleFactor) / (float) this.maxAge * 32.0F, 0.0F, 1.0F);
    }

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
            this.motionY -= 8F;
            this.motionY = Math.max(this.motionY, -0.15);
            this.motionZ -= Math.random() / 75f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSetIn) {
            this.spriteSet = spriteSetIn;
        }

        @Override
        public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            FireflyParticle particle = new FireflyParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.setColor(1.0f, 1.0f, 1.0f);
            particle.selectSpriteRandomly(this.spriteSet);
            return particle;
        }
    }
}
