package fireflies.entity;

import fireflies.Registry;
import fireflies.client.AbdomenAnimationManager;
import fireflies.client.ClientStuff;
import fireflies.client.particle.FireflyParticleManager;
import fireflies.client.sound.FireflyFlightSound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidType;

import javax.annotation.Nullable;
import java.util.EnumSet;

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
    public int eatCompostCooldown;
    /**
     * How many ticks this firefly has been underwater for
     */
    private int underWaterTicks;
    private boolean cachedHasIllumerin;
    public int illumerinDropTime;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, Level level) {
        super(entityType, level);
        if (level.isClientSide()) {
            abdomenAnimationManager = new AbdomenAnimationManager(this);
            particleManager = new FireflyParticleManager(this);
        } else {
            abdomenAnimationManager = null;
            particleManager = null;
        }
        this.moveControl = new FlyingMoveControl(this, 20, true);
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
                .add(Attributes.FOLLOW_RANGE, 48D);
    }


    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
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
        nbt.putInt("IllumerinDropTime", this.illumerinDropTime);
        nbt.putInt("EatCompostCooldown", this.eatCompostCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHasIllumerin(nbt.getBoolean("HasIllumerin"));
        this.illumerinDropTime = nbt.getInt("IllumerinDropTime");
        this.eatCompostCooldown = nbt.getInt("EatCompostCooldown");
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals. (0 being the highest priority, of course -_-)
        this.goalSelector.addGoal(0, new PanicGoal(this, 2.5f));
        this.goalSelector.addGoal(1, new EatCompostGoal(1f, 22));
        this.goalSelector.addGoal(2, new FlyingWanderGoal());
        this.goalSelector.addGoal(3, new FloatGoal(this));
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
    public void aiStep() {
        super.aiStep();

        if (this.level.isClientSide) {
            if (!ClientStuff.isDayTime(this.level)) {
                this.particleManager.trySpawnDustParticles();
            } else {
                this.abdomenAnimationManager.setAnimation(null);
            }
            return;
        }

        if (this.isNoAi()) {
            return;
        }

        this.underWaterTicks = this.isInWaterOrBubble() ? this.underWaterTicks + 1 : 0;

        if (this.underWaterTicks > 20) {
            this.hurt(DamageSource.DROWN, 1.0F);
        }

        if (this.eatCompostCooldown > 0) {
            this.eatCompostCooldown--;
        }

        if (this.hasIllumerin() && this.illumerinDropTime-- <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, Mth.nextFloat(this.random, 0.5f, 1.5f));
            this.spawnAtLocation(Registry.ILLUMERIN.get());
            this.setHasIllumerin(false);
            this.setRandomIllumerinDropTime();
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

    // region what does this do?
    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    @Override
    protected boolean isFlapping() {
        return !this.onGround;
    }
    //endregion

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        // do nothing
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        // None
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return false; // firefly doesn't accept any offerings
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
        this.illumerinDropTime = Mth.nextInt(this.random, 3600, 7200); // 3600, 7200
    }

    public class FlyingWanderGoal extends Goal {

        public FlyingWanderGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return FireflyEntity.this.getNavigation().isDone() && FireflyEntity.this.getRandom().nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return FireflyEntity.this.getNavigation().isInProgress();
        }

        @Override
        public void start() {
            Vec3 vec3 = this.findPos();
            if (vec3 != null) {
                FireflyEntity.this.getNavigation().moveTo(FireflyEntity.this.getNavigation().createPath(new BlockPos(vec3), 1), 1.0D);
            }
        }

        @Nullable
        private Vec3 findPos() {
            Vec3 viewVector = FireflyEntity.this.getViewVector(0.0F);
            Vec3 hoverPos = HoverRandomPos.getPos(FireflyEntity.this, 8, 7, viewVector.x, viewVector.z, ((float) Math.PI / 2F), 3, 1);
            return hoverPos != null ? hoverPos : AirAndWaterRandomPos.getPos(FireflyEntity.this, 8, 4, -2, viewVector.x, viewVector.z, (float) Math.PI / 2F);
        }
    }

    public class EatCompostGoal extends MoveToBlockGoal {
        private int startEatingTicks;

        public EatCompostGoal(double pSpeedModifier, int pSearchRange) {
            super(FireflyEntity.this, pSpeedModifier, pSearchRange);
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 100 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader worldIn, BlockPos pos) {
            if (isComposterDesirable(worldIn, pos)) {
                final BlockPos up = pos.above();
                final BlockState above = worldIn.getBlockState(up);
                return worldIn.isEmptyBlock(up) || (above.getBlock() instanceof TrapDoorBlock && above.getValue(TrapDoorBlock.OPEN));
            }
            return false;
        }

        public static boolean isComposterDesirable(LevelReader world, BlockPos pos) {
            final BlockState state = world.getBlockState(pos);
            return state.is(Blocks.COMPOSTER) && state.getValue(ComposterBlock.LEVEL) > 0;
        }

        private void lookAtCompost() {
            FireflyEntity.this.lookControl.setLookAt(Vec3.atCenterOf(this.blockPos));
        }

        @Override
        public void tick() {
            if (this.blockPos.closerThan(FireflyEntity.this.blockPosition(), 2f)) {
                this.lookAtCompost();
                if (this.startEatingTicks++ >= 40) {
                    this.eatCompost();
                }
            }

            super.tick();
        }

        private void eatCompost() {
            final BlockState state = FireflyEntity.this.level.getBlockState(this.blockPos);
            if (!state.is(Blocks.COMPOSTER)) {
                return;
            }

            final int i = state.getValue(ComposterBlock.LEVEL);
            if (i <= 0) {
                return;
            }

            final boolean eaten = FireflyEntity.this.getRandom().nextFloat() >= 0.5f;
            if (eaten) {
                FireflyEntity.this.level.setBlock(this.blockPos, state.setValue(ComposterBlock.LEVEL, i - (i == ComposterBlock.READY ? 2 : 1)), 3);
            }
            FireflyEntity.this.playSound(eaten ? SoundEvents.COMPOSTER_EMPTY : SoundEvents.COMPOSTER_FILL, 1.0F, 1.0F);

            final var center = Vec3.atCenterOf(this.blockPos);

            ((ServerLevel) FireflyEntity.this.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEAD_BUSH.defaultBlockState()),
                    center.x, center.y + .5f, center.z, 50, 0, 0, 0, 0.05D);

            FireflyEntity.this.eatCompostCooldown = 1200;
            FireflyEntity.this.setHasIllumerin(true);
        }

        private boolean canEat() {
            return !FireflyEntity.this.hasIllumerin() && FireflyEntity.this.eatCompostCooldown <= 0;
        }

        @Override
        public boolean canUse() {
            return canEat() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canEat() && super.canContinueToUse() && FireflyEntity.this.level.getBrightness(LightLayer.BLOCK, this.blockPos) <= 2;
        }

        @Override
        public void start() {
            this.startEatingTicks = 0;
            this.lookAtCompost();
            super.start();
        }
    }
}
