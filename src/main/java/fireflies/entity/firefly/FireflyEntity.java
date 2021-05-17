package fireflies.entity.firefly;

import fireflies.client.particle.FireflyAbdomenParticleData;
import fireflies.setup.FirefliesRegistration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ITag;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.EnumSet;

@SuppressWarnings({"deprecation"})
public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    private static final DataParameter<Integer> ANIMATION = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> GLOW_ALPHA = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> GLOW_INCREASING = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);

    public float abdomenParticlePositionOffset;
    private int underWaterTicks;

    public FireflyEntity(EntityType<? extends FireflyEntity> type, World world) {
        super(type, world);
        this.moveController = new FlyingMovementController(this, 20, true);
        this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathPriority(PathNodeType.COCOA, -1.0F);
        this.setPathPriority(PathNodeType.FENCE, -1.0F);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return LivingEntity.registerAttributes()
                .createMutableAttribute(Attributes.MAX_HEALTH, 6.0D)
                .createMutableAttribute(Attributes.FLYING_SPEED, 0.4F)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.25F)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(ANIMATION, FireflyAbdomenAnimation.OFF.ordinal());
        this.dataManager.register(GLOW_ALPHA, 0f);
        this.dataManager.register(GLOW_INCREASING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.fromItems(Items.HONEY_BOTTLE), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(8, new FireflyEntity.WanderGoal());
        this.goalSelector.addGoal(9, new SwimGoal(this));
    }

    @Override
    public float getBlockPathWeight(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    protected void updateAITasks() {
        if (this.isInWaterOrBubbleColumn()) {
            this.underWaterTicks++;
        } else {
            this.underWaterTicks = 0;
        }

        if (this.underWaterTicks > 20) {
            this.attackEntityFrom(DamageSource.DROWN, 1.0F);
        }
    }

    @Override
    protected PathNavigator createNavigator(World worldIn) {
        FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, worldIn) {
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };
        flyingpathnavigator.setCanOpenDoors(false);
        flyingpathnavigator.setCanSwim(false);
        flyingpathnavigator.setCanEnterDoors(true);
        return flyingpathnavigator;
    }

    public FireflyAbdomenAnimation getAnimation() {
        return FireflyAbdomenAnimation.values()[this.dataManager.get(ANIMATION)];
    }

    public void setAnimation(FireflyAbdomenAnimation animation) {
        if (!world.isRemote) {
            switch (animation) {
                case CALM_SYNCHRONIZED:
                    FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.remove(this);
                    if (!FireflyGlowSync.calmSyncedFireflies.syncedFireflies.contains(this)) {
                        FireflyGlowSync.calmSyncedFireflies.syncedFireflies.add(this);
                    }
                    break;
                case STARRY_NIGHT_SYNCHRONIZED:
                    FireflyGlowSync.calmSyncedFireflies.syncedFireflies.remove(this);
                    if (!FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.contains(this)) {
                        FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.add(this);
                    }
                    break;
                default:
                    FireflyGlowSync.calmSyncedFireflies.syncedFireflies.remove(this);
                    FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.remove(this);
                    break;
            }
        }
        this.dataManager.set(ANIMATION, animation.ordinal());
    }

    public float getGlowAlpha() {
        return this.dataManager.get(GLOW_ALPHA);
    }

    public void setGlowAlpha(float alpha) {
        this.dataManager.set(GLOW_ALPHA, alpha);
    }

    public boolean getGlowIncreasing() {
        return this.dataManager.get(GLOW_INCREASING);
    }

    public void setGlowIncreasing(boolean b) {
        this.dataManager.set(GLOW_INCREASING, b);
    }

    public Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    public void spawnAbdomenParticle() {
        if (!this.world.isRemote) {
            ((ServerWorld) (this.world)).spawnParticle(new FireflyAbdomenParticleData(this.getEntityId()), this.getPosX(), this.getPosY(), this.getPosZ(),
                    1, 0, 0, 0, 0);
        }
    }

    private void glowAnimation(float increase, float decrease, float startIncreasingChance, float startDecreasingChance) {
        this.setGlowAlpha(this.getGlowIncreasing() ? (this.getGlowAlpha() + increase) : (this.getGlowAlpha() - decrease));
        if (this.getGlowAlpha() <= 0) {
            this.setGlowAlpha(0); // If it goes under or over 0 or 1 it'll wrap back around to being on/off, we don't want that
            if (this.rand.nextFloat() <= startIncreasingChance) {
                this.setGlowIncreasing(true);
                this.spawnAbdomenParticle();
            }
        } else if (this.getGlowAlpha() >= 1) {
            this.setGlowAlpha(1);
            if (this.rand.nextFloat() <= startDecreasingChance) {
                this.setGlowIncreasing(false);
            }
        }
    }

    private void tickGlowAnimation() {
        switch (this.getAnimation()) {
            case OFF:
                this.setGlowAlpha(0);
                break;
            case DEFAULT:
                this.glowAnimation(0.1f, 0.05f, 0.05f, 0.075f);
                break;
            case CALM:
                boolean isMiddling = this.getGlowAlpha() < 0.75f && this.getGlowAlpha() > 0.25f;
                float increaseAmount = isMiddling ? 0.02f : 0.075f;
                float decreaseAmount = isMiddling ? 0.01f : 0.05f;
                this.glowAnimation(increaseAmount, decreaseAmount, 0.05f, 0.05f);
                break;
            case STARRY_NIGHT:
                this.glowAnimation(0.3f, 0.25f, 0.075f, 0.95f);
                break;
            case FRANTIC:
                this.glowAnimation(0.35f, 0.4f, 0.2f, 0.35f);
        }
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            // Falling dust particles (no i don't understand this i stole it from the squid code)
            if (this.getGlowAlpha() > 0.25f && this.rand.nextFloat() > 0.9f) {
                Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.getPosX(), this.getPosY(), this.getPosZ());
                Vector3d vector3d1 = this.rotateVector(new Vector3d(this.rand.nextFloat(), -1.0D, this.rand.nextFloat() * Math.abs(this.getMotion().getZ()) * 10 + 2));
                Vector3d vector3d2 = vector3d1.scale(-5f + this.rand.nextFloat() * 2.0F);

                float randPos = MathHelper.nextFloat(this.rand, -0.2f, 0.2f);
                if (this.isChild()) randPos /= 4f;
                this.world.addParticle(FirefliesRegistration.FIREFLY_DUST_PARTICLE.get(),
                        vector3d.x + randPos, vector3d.y + 1.35f + randPos, vector3d.z + randPos,
                        vector3d2.x, vector3d2.y * -16, vector3d2.z);
            }
        } else {
            if (this.world.isDaytime()) {
                this.setAnimation(FireflyAbdomenAnimation.OFF);
                this.setGlowAlpha(0);
                return;
            }

            Biome biome = this.world.getBiome(this.getPosition());
            switch (biome.getCategory()) {
                case SWAMP:
                    this.setAnimation(FireflyAbdomenAnimation.CALM);
                    break;
                case FOREST:
                    if (biome.getRegistryName() == Biomes.DARK_FOREST.getRegistryName()
                            || biome.getRegistryName() == Biomes.DARK_FOREST_HILLS.getRegistryName()) {
                        this.setAnimation(FireflyAbdomenAnimation.CALM_SYNCHRONIZED);
                    } else {
                        this.setAnimation(FireflyAbdomenAnimation.STARRY_NIGHT_SYNCHRONIZED);
                    }
                    break;
                case PLAINS:
                    this.setAnimation(FireflyAbdomenAnimation.STARRY_NIGHT);
                    break;
                case NETHER:
                case THEEND:
                    this.setAnimation(FireflyAbdomenAnimation.FRANTIC);
                    break;
                default:
                    this.setAnimation(FireflyAbdomenAnimation.DEFAULT);
                    break;
            }

            this.tickGlowAnimation();
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.HONEY_BOTTLE;
    }

    @Override
    protected void consumeItemFromStack(PlayerEntity player, ItemStack honeyBottle) {
        // Don't consume glass bottle
        if (!player.abilities.isCreativeMode) {
            honeyBottle.shrink(1);
            ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.inventory.addItemStackToInventory(glassBottle)) {
                player.dropItem(glassBottle, false);
            }
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!world.isRemote) {
            FireflyGlowSync.calmSyncedFireflies.syncedFireflies.remove(this);
            FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.remove(this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return sizeIn.height * 0.5F;
    }

    @Override
    public boolean onLivingFall(float distance, float damageMultiplier) {
        return false;
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
        // Do nothing
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        // None
    }

    @Override
    protected boolean makeFlySound() {
        return true;
    }

    @Override
    public CreatureAttribute getCreatureAttribute() {
        return CreatureAttribute.ARTHROPOD;
    }

    @Override
    protected void handleFluidJump(ITag<Fluid> fluidTag) {
        this.setMotion(this.getMotion().add(0.0D, 0.01D, 0.0D));
    }

    public FireflyEntity createChild(ServerWorld world, AgeableEntity mate) {
        return FirefliesRegistration.FIREFLY.get().create(world);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vector3d getLeashStartPosition() {
        return new Vector3d(0.0D, (0.5F * this.getEyeHeight()), (this.getWidth() * 0.2F));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double d0 = 128 * getRenderDistanceWeight();
        return distance < d0 * d0;
    }

    private class WanderGoal extends Goal {
        private WanderGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean shouldExecute() {
            return FireflyEntity.this.navigator.noPath() && FireflyEntity.this.rand.nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return FireflyEntity.this.navigator.hasPath();
        }

        @Override
        public void startExecuting() {
            Vector3d vector3d = this.getRandomLocation();
            if (vector3d != null) {
                FireflyEntity.this.navigator.setPath(FireflyEntity.this.navigator.getPathToPos(new BlockPos(vector3d), 1), 1.0D);
            }
        }

        @Nullable
        private Vector3d getRandomLocation() {
            Vector3d vector3d = FireflyEntity.this.getLook(0.0F);
            Vector3d vector3d2 = RandomPositionGenerator.findAirTarget(FireflyEntity.this, 8, 7, vector3d, ((float) Math.PI / 2F), 2, 1);
            return vector3d2 != null ? vector3d2 : RandomPositionGenerator.findGroundTarget(FireflyEntity.this, 8, 4, -2, vector3d, ((float) Math.PI / 2F));
        }
    }
}
