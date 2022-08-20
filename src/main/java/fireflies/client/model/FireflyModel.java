package fireflies.client.model;

import com.google.common.collect.ImmutableList;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// Made with Blockbench 4.0.3
// Exported for Minecraft version 1.17 with Mojang mappings
// Paste this class into your mod and generate all required imports

@OnlyIn(Dist.CLIENT)
public class FireflyModel<T extends FireflyEntity> extends AgeableListModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static ModelLayerLocation FIREFLY_LAYER = new ModelLayerLocation(new ResourceLocation(Fireflies.MOD_ID, "firefly"), "firefly");

    public final ModelPart abdomen;
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart legs1;
    private final ModelPart legs2;

    public FireflyModel(ModelPart root) {
        this.abdomen = root.getChild("Firefly").getChild("Abdomen");
        this.head = root.getChild("Firefly").getChild("Head");
        this.rightWing = head.getChild("Wings").getChild("rightWing");
        this.leftWing = head.getChild("Wings").getChild("leftWing");
        this.legs1 = head.getChild("Legs1");
        this.legs2 = head.getChild("Legs2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Firefly = partdefinition.addOrReplaceChild("Firefly", CubeListBuilder.create(), PartPose.offset(0.0F, 9.0F, -5.0F));
        Firefly.addOrReplaceChild("Abdomen", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 3.5F, -0.2618F, 0.0F, 0.0F));
        PartDefinition Head = Firefly.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(16, 24).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Head.addOrReplaceChild("Antennae", CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.7854F, 0.0F, 0.0F));
        PartDefinition Wings = Head.addOrReplaceChild("Wings", CubeListBuilder.create(), PartPose.offset(0.0F, 0.75F, 3.25F));
        Wings.addOrReplaceChild("rightWing", CubeListBuilder.create().texOffs(0, 16).addBox(-9.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, -0.2618F, 0.0F));
        Wings.addOrReplaceChild("leftWing", CubeListBuilder.create().texOffs(0, 10).addBox(0.4F, -0.35F, -0.15F, 9.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.2618F, 0.0F));
        Head.addOrReplaceChild("Legs1", CubeListBuilder.create().texOffs(17, 0).addBox(-2.0F, 0.5F, -1.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 3.0F, 0.2618F, 0.0F, 0.0F));
        Head.addOrReplaceChild("Legs2", CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 1.0F, 0.2618F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(FireflyEntity fireflyEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Bob head
        this.head.xRot = 0.75f - ((float) Math.PI / 4F) + this.animSpeed(ageInTicks, 0.1F) * 0.1F;

        // Bob abdomen
        this.abdomen.xRot = (fireflyEntity.hasIllumerin() ? .1f : 0.5f) - ((float) Math.PI / 4F) + this.animSpeed(ageInTicks, 0.1F) * 0.05F;
        // Update the particle position offset
        fireflyEntity.particleManager.abdomenParticlePositionOffset = this.abdomen.xRot;

        // Flap wings
        this.rightWing.zRot = this.animSpeed(ageInTicks, 3F) * (float) Math.PI * 0.15F;
        this.leftWing.zRot = -this.rightWing.zRot;

        // Swag legs
        this.legs1.xRot = (0.25f - this.animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.0075F);
        this.legs2.xRot = (0.25f - this.animSpeed(ageInTicks, 0.2F) * (float) Math.PI * 0.005F);
    }

    private float animSpeed(float ageInTicks, float speed) {
        return Mth.cos(ageInTicks * speed);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of();
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(this.head, this.abdomen);
    }
}
