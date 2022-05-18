package fireflies.client.model;

import com.google.common.collect.ImmutableList;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyModel<T extends FireflyEntity> extends AgeableModel<T> {

    public final ModelRenderer abdomen;
    private final ModelRenderer head;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer legs1;
    private final ModelRenderer legs2;

    public FireflyModel() {
        this.textureWidth = 32;
        this.textureHeight = 32;

        this.abdomen = new ModelRenderer(this);
        this.abdomen.setRotationPoint(0.0F, 19.0F, -1.5F);
        this.setRotationAngle(this.abdomen, -0.25F, 0.0F, 0.0F);
        this.abdomen.setTextureOffset(0, 0).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 5.0F, 0.0F, false);

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 18.5F, -1.0F);
        this.head.setTextureOffset(16, 24).addBox(-2.0F, -1.5F, -4.0F, 4.0F, 4.0F, 4.0F, 0.0F, false);

        final ModelRenderer antennae = new ModelRenderer(this);
        antennae.setRotationPoint(0.0F, -0.5F, -4.0F);
        this.head.addChild(antennae);
        this.setRotationAngle(antennae, 0.775F, 0.0F, 0.0F);
        antennae.setTextureOffset(0, 27).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 0.0F, 0.0F, false);

        final ModelRenderer wings = new ModelRenderer(this);
        wings.setRotationPoint(0.0F, -0.75F, -0.75F);
        this.head.addChild(wings);

        this.rightWing = new ModelRenderer(this);
        this.rightWing.setRotationPoint(0.0F, 0.0F, 0.0F);
        wings.addChild(this.rightWing);
        this.setRotationAngle(this.rightWing, 0.775F, -0.25F, 0.0F);
        this.rightWing.setTextureOffset(0, 16).addBox(-9.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, 0.0F, false);

        this.leftWing = new ModelRenderer(this);
        this.leftWing.setRotationPoint(0.0F, 0.0F, 0.0F);
        wings.addChild(this.leftWing);
        this.setRotationAngle(this.leftWing, 0.775F, 0.25F, 0.0F);
        this.leftWing.setTextureOffset(0, 10).addBox(0.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, 0.0F, false);

        this.legs1 = new ModelRenderer(this);
        this.legs1.setRotationPoint(0.0F, 1.5F, -1.0F);
        this.head.addChild(this.legs1);
        this.setRotationAngle(this.legs1, 0.25f, 0.0F, 0.0F);
        this.legs1.setTextureOffset(17, 0).addBox(-2.0F, 0.5F, -1.0F, 4.0F, 2.0F, 0.0F, 0.0F, false);

        this.legs2 = new ModelRenderer(this);
        this.legs2.setRotationPoint(0.0F, 2.5F, -3.0F);
        this.head.addChild(this.legs2);
        this.setRotationAngle(this.legs2, 0.25f, 0.0F, 0.0F);
        this.legs2.setTextureOffset(26, 0).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 2.0F, 0.0F, 0.0F, false);
    }

    @Override
    public void setRotationAngles(FireflyEntity fireflyEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Bob head
        this.head.rotateAngleX = 0.75f - ((float) Math.PI / 4F) + this.animSpeed(ageInTicks, 0.1F) * 0.1F;

        // Bob abdomen
        this.abdomen.rotateAngleX = .1f - ((float) Math.PI / 4F) + this.animSpeed(ageInTicks, 0.1F) * 0.05F;
        // Update the particle position offset
        fireflyEntity.particleManager.abdomenParticlePositionOffset = this.abdomen.rotateAngleX;

        // Flap wings
        this.rightWing.rotateAngleZ = this.animSpeed(ageInTicks, 3F) * (float) Math.PI * 0.15F;
        this.leftWing.rotateAngleZ = -this.rightWing.rotateAngleZ;

        // Swag legs
        this.legs1.rotateAngleX = (0.25f - this.animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.0075F);
        this.legs2.rotateAngleX = (0.25f - this.animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.005F);
    }

    private float animSpeed(float ageInTicks, float speed) {
        return MathHelper.cos(ageInTicks * speed);
    }

    @Override
    protected Iterable<ModelRenderer> getHeadParts() {
        return ImmutableList.of();
    }

    @Override
    protected Iterable<ModelRenderer> getBodyParts() {
        return ImmutableList.of(this.head, this.abdomen);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
