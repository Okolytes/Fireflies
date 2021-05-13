package fireflies.entity.firefly;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireflyModel<T extends FireflyEntity> extends AgeableModel<T> {

    private final ModelRenderer abdomen;
    private final ModelRenderer head;
    private final ModelRenderer antennae;
    private final ModelRenderer wings;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer legs1;
    private final ModelRenderer legs2;

    private static final float legsRotateAngleX = 0.25F;

    public FireflyModel() {
        textureWidth = 32;
        textureHeight = 32;

        abdomen = new ModelRenderer(this);
        abdomen.setRotationPoint(0.0F, 19.0F, -1.5F);
        setRotationAngle(abdomen, -0.25F, 0.0F, 0.0F);
        abdomen.setTextureOffset(0, 0).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 5.0F, 0.0F, false);

        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, 18.5F, -1.0F);
        head.setTextureOffset(16, 24).addBox(-2.0F, -1.5F, -4.0F, 4.0F, 4.0F, 4.0F, 0.0F, false);

        antennae = new ModelRenderer(this);
        antennae.setRotationPoint(0.0F, -0.5F, -4.0F);
        head.addChild(antennae);
        setRotationAngle(antennae, 0.775F, 0.0F, 0.0F);
        antennae.setTextureOffset(0, 27).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 0.0F, 0.0F, false);

        wings = new ModelRenderer(this);
        wings.setRotationPoint(0.0F, -0.75F, -0.75F);
        head.addChild(wings);

        rightWing = new ModelRenderer(this);
        rightWing.setRotationPoint(0.0F, 0.0F, 0.0F);
        wings.addChild(rightWing);
        setRotationAngle(rightWing, 0.775F, -0.25F, 0.0F);
        rightWing.setTextureOffset(0, 16).addBox(-9.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, 0.0F, false);

        leftWing = new ModelRenderer(this);
        leftWing.setRotationPoint(0.0F, 0.0F, 0.0F);
        wings.addChild(leftWing);
        setRotationAngle(leftWing, 0.775F, 0.25F, 0.0F);
        leftWing.setTextureOffset(0, 10).addBox(0.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, 0.0F, false);

        legs1 = new ModelRenderer(this);
        legs1.setRotationPoint(0.0F, 1.5F, -1.0F);
        head.addChild(legs1);
        setRotationAngle(legs1, legsRotateAngleX, 0.0F, 0.0F);
        legs1.setTextureOffset(17, 0).addBox(-2.0F, 0.5F, -1.0F, 4.0F, 2.0F, 0.0F, 0.0F, false);

        legs2 = new ModelRenderer(this);
        legs2.setRotationPoint(0.0F, 2.5F, -3.0F);
        head.addChild(legs2);
        setRotationAngle(legs2, legsRotateAngleX, 0.0F, 0.0F);
        legs2.setTextureOffset(26, 0).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 2.0F, 0.0F, 0.0F, false);
    }

    @Override
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Bob head
        this.head.rotateAngleX = 0.75f - ((float) Math.PI / 4F) + animSpeed(ageInTicks, 0.1F) * 0.1F;

        // Bob abdomen
        this.abdomen.rotateAngleX = 0.5F - ((float) Math.PI / 4F) + animSpeed(ageInTicks, 0.1F) * 0.05F;

        // Flap wings
        this.rightWing.rotateAngleZ = animSpeed(ageInTicks, 2.5F) * (float) Math.PI * 0.15F;
        this.leftWing.rotateAngleZ = -this.rightWing.rotateAngleZ;

        // Swag legs
        this.legs1.rotateAngleX = (legsRotateAngleX - animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.0075F);
        this.legs2.rotateAngleX = (legsRotateAngleX - animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.005F);
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
