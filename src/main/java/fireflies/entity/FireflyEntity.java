package fireflies.entity;

import fireflies.Registry;
import fireflies.client.AbdomenAnimationManager;
import fireflies.client.ClientStuff;
import fireflies.client.particle.FireflyParticleManager;
import fireflies.client.sound.FireflyFlightSound;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidType;

import javax.annotation.Nullable;

public class FireflyEntity extends Animal implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> HAS_ILLUMERIN = SynchedEntityData.defineId(FireflyEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * null on server
     */
    public final AbdomenAnimationManager abdomenAnimationManager;
    /**
     * null on server
     */
    public final FireflyParticleManager particleManager;
    public int timeUntilCanEatCompostAgain;
    /**
     * How many ticks this firefly has been underwater for
     */
    private int underWaterTicks;
    /**
     * How many ticks this firefly has been rained on for
     */
    private int rainedOnTicks;
    private boolean cachedHasIllumerin;
    private int timeUntilIllumerinDrop;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, Level level) {
        super(entityType, level);
        if (level.isClientSide) {
            abdomenAnimationManager = new AbdomenAnimationManager(this);
            particleManager = new FireflyParticleManager(this);
        } else {
            abdomenAnimationManager = null;
            particleManager = null;
        }
        //this.moveControl = new FireflyAI.FlyingMovementHelper(this);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.COCOA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.FLYING_SPEED, 0.25F)
                .add(Attributes.MOVEMENT_SPEED, 0.15F)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return Registry.FIREFLY.get().create(pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAS_ILLUMERIN, false);
    }

    public boolean hasIllumerin() {
        // This method is called every frame in our abdomen layer renderer, so this statement *will* pass
        if (this.tickCount % 10 == 0) {
            this.cachedHasIllumerin = this.entityData.get(HAS_ILLUMERIN);
        }
        return this.cachedHasIllumerin;
    }

    public void setHasIllumerin(boolean b) {
        if (b) {
            this.setRandomIllumerinDropTime();
        }
        this.cachedHasIllumerin = b;
        this.entityData.set(HAS_ILLUMERIN, b);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("HasIllumerin", this.hasIllumerin());
        nbt.putInt("IllumerinDropTime", this.timeUntilIllumerinDrop);
        nbt.putInt("EatCompostCooldown", this.timeUntilCanEatCompostAgain);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHasIllumerin(nbt.getBoolean("HasIllumerin"));
        this.timeUntilIllumerinDrop = nbt.getInt("IllumerinDropTime");
        this.timeUntilCanEatCompostAgain = nbt.getInt("EatCompostCooldown");
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals. (0 being the highest priority, of course -_-)
        this.goalSelector.addGoal(0, new PanicGoal(this, 2.5f));
        //this.goalSelector.addGoal(1, new FireflyAI.MateGoal(this, 1f));
        //this.goalSelector.addGoal(3, new FireflyAI.EatCompostGoal(this, 1f, 22));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.15f));
        //this.goalSelector.addGoal(6, new FireflyAI.WanderGoal(this));
        this.goalSelector.addGoal(7, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        final FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(BlockPos pos) {
                return !this.level.isEmptyBlock(pos.below());
            }
        };
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(false);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }


    @Override
    public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
        return pLevel.isEmptyBlock(pPos) ? 10.0F : 0.0F;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level.isClientSide) {
            if (!ClientStuff.isDayTime(this.level)) {
                this.particleManager.trySpawnDustParticles();
            } else {
                this.abdomenAnimationManager.setAnimation(null);
            }
        } else {
            if (!this.isNoAi()) {
                this.underWaterTicks = this.isInWaterOrBubble() ? this.underWaterTicks + 1 : 0;
                this.rainedOnTicks = this.level.isRainingAt(this.blockPosition()) ? this.rainedOnTicks + 1 : 0;

                if (this.underWaterTicks > 20) {
                    this.hurt(DamageSource.DROWN, 1.0F);
                }

                if (this.timeUntilCanEatCompostAgain > 0) {
                    this.timeUntilCanEatCompostAgain--;
                }

                if (this.hasIllumerin()) {
                    if (this.timeUntilIllumerinDrop-- <= 0) {
                        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, Mth.nextFloat(this.random, 0.5f, 1.5f));
                        this.spawnAtLocation(Registry.ILLUMERIN.get());
                        this.setHasIllumerin(false);
                        this.setRandomIllumerinDropTime();
                    }
                }
            }
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.level.isClientSide) {
            if (!this.isSilent()) {
                FireflyFlightSound.beginFireflyFlightSound(this);
            }
            if (this.abdomenAnimationManager.abdomenAnimationProperties.glow > 0) {
                this.particleManager.spawnAbdomenParticle(); // we don't want them popping in to view without a particle
            }
            this.abdomenAnimationManager.setAnimation("default");
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (this.level.isClientSide) {
            this.abdomenAnimationManager.setAnimation(null);
        }
        super.onRemovedFromWorld();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level.isClientSide && amount > 0) { // todo for some reason it runs twice, second time the source is 'generic' and amount is 0
            final int particleCount = (int) Mth.clamp(amount, 2, 8);
            for (int i = 0; i < particleCount; i++) {
                this.level.addParticle(this.particleManager.getDustParticle(), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
            this.abdomenAnimationManager.setAnimation("hurt");
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        if (this.level.isClientSide) {
            this.particleManager.destroyAbdomenParticle();
        }
        super.die(cause);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Registry.FIREFLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Registry.FIREFLY_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pDimensions) {
        return pDimensions.height * .5f;
    }

    // fixme
    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        // do nothing
    }

    // fixme
    //@Override
    //protected boolean isMovementNoisy() {
    //    return false;
    //}

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        // None
    }

    // fixme
//    @Override
//    protected boolean makeFlySound() {
//        return true;
//    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    /**
     * Adds motion to our current motion (removes if negative value).
     */
    public void addMotion(double x, double y, double z) {
        this.setDeltaMovement(this.getDeltaMovement().add(x, y, z));
    }

    @Override
    public void jumpInFluid(FluidType type) {
        this.addMotion(0f, 0.01f, 0f);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.2F);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // About 2x the render distance of players.
        final double d0 = 128 * getViewScale();
        return distance < d0 * d0;
    }

    private void setRandomIllumerinDropTime() {
        this.timeUntilIllumerinDrop = Mth.nextInt(this.random, 3600, 7200); // 3600, 7200
    }
}
