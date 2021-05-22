package fireflies.entity.firefly;

import fireflies.client.DoClientStuff;
import fireflies.setup.FirefliesRegistration;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import java.util.Objects;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    private static final DataParameter<Boolean> IS_NIGHTTIME = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    public FireflyAbdomenAnimation abdomenAnimation;
    public float glowAlpha;
    public boolean glowIncreasing;
    public float abdomenParticlePositionOffset;
    private int underWaterTicks;
    private int lastAccelerationTicks;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.moveController = new FlyingMovementController(this, 10, true);
        this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathPriority(PathNodeType.COCOA, -1.0F);
        this.setPathPriority(PathNodeType.FENCE, -1.0F);
    }

    public FireflyEntity createChild(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return FirefliesRegistration.FIREFLY.get().create(serverWorld);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return LivingEntity.registerAttributes()
                .createMutableAttribute(Attributes.MAX_HEALTH, 6.0D)
                .createMutableAttribute(Attributes.FLYING_SPEED, 0.25F)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.15F)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(IS_NIGHTTIME, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreedGoal(this, 1f));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25D, Ingredient.fromItems(Items.HONEY_BOTTLE), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.75f));
        this.goalSelector.addGoal(5, new WanderGoal(this));
        this.goalSelector.addGoal(7, new SwimGoal(this));
        this.goalSelector.addGoal(10, new MoveToHoneyGoal(this, 1, 24));
    }

    @Override
    protected PathNavigator createNavigator(World world) {
        FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, world) {
            @Override
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };
        flyingpathnavigator.setCanOpenDoors(false);
        flyingpathnavigator.setCanSwim(false);
        flyingpathnavigator.setCanEnterDoors(true);
        return flyingpathnavigator;
    }

    @Override
    public float getBlockPathWeight(BlockPos blockPos, IWorldReader world) {
        return world.getBlockState(blockPos).isAir() ? 16.0F : 0.0F;
    }

    public void setAbdomenAnimation(FireflyAbdomenAnimation newAnimation) {
        if (this.world.isRemote && newAnimation != this.abdomenAnimation) { // Probably don't wanna access list(s) every tick
            switch (newAnimation) {
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
            }
        }
        this.abdomenAnimation = newAnimation;
    }

    public void spawnAbdomenParticle() {
        if (this.world.isRemote) {
            this.world.addParticle(new FireflyAbdomenParticleData(this.getEntityId()), this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
        }
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    private void spawnFallingDustParticles() {
        if (this.ticksExisted % 8 == 0 && this.glowAlpha > 0.25f) {
            // no i don't understand this i stole it from the squid code
            Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.getPosX(), this.getPosY(), this.getPosZ());
            Vector3d vector3d1 = this.rotateVector(new Vector3d(this.rand.nextFloat(), -1.0D, this.rand.nextFloat() * Math.abs(this.getMotion().getZ()) * 10 + 2));
            Vector3d vector3d2 = vector3d1.scale(-5f + this.rand.nextFloat() * 2.0F);

            float randPos = this.isChild() ? 0f : MathHelper.nextFloat(this.rand, -0.2f, 0.2f);
            this.world.addParticle(FirefliesRegistration.FIREFLY_DUST_PARTICLE.get(),
                    vector3d.x + randPos, vector3d.y + 1.35f + randPos, vector3d.z + randPos,
                    vector3d2.x, vector3d2.y * -16, vector3d2.z);
        }
    }

    private void glowAnimation(float increase, float decrease, float startIncreasingChance, float startDecreasingChance) {
        this.glowAlpha += this.glowIncreasing ? increase : -decrease;
        if (this.glowAlpha <= 0) {
            this.glowAlpha = 0; // If it goes under or over 0 or 1 it'll wrap back around to being on/off, we don't want that
            if (this.rand.nextFloat() <= startIncreasingChance) {
                this.glowIncreasing = true;
                this.spawnAbdomenParticle();
            }
        } else if (this.glowAlpha >= 1) {
            this.glowAlpha = 1;
            if (this.rand.nextFloat() <= startDecreasingChance) {
                this.glowIncreasing = false;
            }
        }
    }

    private void updateGlowAnimation() {
        switch (this.abdomenAnimation) {
            case OFF:
                this.glowAlpha = 0;
                break;
            case DEFAULT:
                this.glowAnimation(0.1f, 0.05f, 0.05f, 0.075f);
                break;
            case CALM:
                boolean isMiddling = this.glowAlpha < 0.75f && this.glowAlpha > 0.25f;
                float increaseAmount = isMiddling ? 0.02f : 0.075f;
                float decreaseAmount = isMiddling ? 0.01f : 0.05f;
                this.glowAnimation(increaseAmount, decreaseAmount, 0.075f, 0.05f);
                break;
            case STARRY_NIGHT:
                this.glowAnimation(0.3f, 0.25f, 0.075f, 0.95f);
                break;
            case FRANTIC:
                this.glowAnimation(0.35f, 0.4f, 0.2f, 0.35f);
                break;
        }
    }

    private void updateAbdomenAnimation() {
        Biome currentBiome = this.world.getBiome(this.getPosition());
        switch (currentBiome.getCategory()) {
            case SWAMP:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM);
                break;
            case FOREST:
                if (Objects.equals(currentBiome.getRegistryName(), Biomes.DARK_FOREST.getLocation())
                        || Objects.equals(currentBiome.getRegistryName(), Biomes.DARK_FOREST_HILLS.getLocation())) {
                    this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM_SYNCHRONIZED);
                } else {
                    this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT_SYNCHRONIZED);
                }
                break;
            case JUNGLE:
            case TAIGA:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT_SYNCHRONIZED);
                break;
            case PLAINS:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.STARRY_NIGHT);
                break;
            case NETHER:
            case THEEND:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.FRANTIC);
                break;
            default:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.DEFAULT);
                break;
        }
    }

    private boolean isNightTime() {
        return this.dataManager.get(FireflyEntity.IS_NIGHTTIME);
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            if (this.isNightTime()) {
                this.updateAbdomenAnimation();
                this.updateGlowAnimation();
                this.spawnFallingDustParticles();
            } else {
                this.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.glowAlpha = 0;
            }
        } else if (this.ticksExisted % 60 == 0) {
            this.dataManager.set(FireflyEntity.IS_NIGHTTIME, this.world.isNightTime());
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack itemStack) {
        return itemStack.getItem() == Items.HONEY_BOTTLE;
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
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.world.isRemote) {
            DoClientStuff doClientStuff = new DoClientStuff();
            doClientStuff.playFireflyLoopSound(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (this.world.isRemote) {
            FireflyGlowSync.calmSyncedFireflies.syncedFireflies.remove(this);
            FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.remove(this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
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
    protected float getStandingEyeHeight(Pose pose, EntitySize entitySize) {
        return entitySize.height * 0.5F;
    }

    @Override
    public boolean onLivingFall(float distance, float damageMultiplier) {
        return false;
    }

    @Override
    protected void updateFallState(double y, boolean onGround, BlockState blockState, BlockPos blockPos) {
        // Do nothing
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!this.world.isRemote && this.world.isNightTime()) {
            ((ServerWorld) (this.world)).spawnParticle(FirefliesRegistration.FIREFLY_DUST_PARTICLE.get(), this.getPosX(), this.getPosY(), this.getPosZ(),
                    (int) MathHelper.clamp(amount, 3, 10), 0, 0, 0, 1);
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
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

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (this.isInWaterOrBubbleColumn()) {
            this.underWaterTicks++;
        } else {
            this.underWaterTicks = 0;
        }

        if (this.underWaterTicks > 20) {
            this.attackEntityFrom(DamageSource.DROWN, 1.0F);
        }

        if (MathHelper.sqrt(Entity.horizontalMag(this.getMotion())) < 0.015f
                && this.lastAccelerationTicks > 60 && !this.world.isRemote) {
            final float accelAmount = 0.035f;
            this.setMotion(
                    MathHelper.clamp(this.getLook(0).getX(), -accelAmount, accelAmount),
                    this.world.isAirBlock(this.getPosition().down()) ? 0f : 0.05f,
                    MathHelper.clamp(this.getLook(0).getZ(), -accelAmount, accelAmount));
            this.navigator.clearPath();
            this.lastAccelerationTicks = 0;
        }

        if (this.isOnGround()) {
            this.navigator.tryMoveToXYZ(this.getPosXRandom(3f), this.getPosY() + 2, this.getPosZRandom(3f), 0.85);
        } else if (this.ticksExisted % 20 == 0) {
            if (!this.world.isAirBlock(this.getPosition().down())) {
                this.setMotion(0, 0.005f, 0);
            } else if (this.world.isAirBlock(this.getPosition().down(3))) {
                this.setMotion(0, -0.01f, 0);
            }
        }

        this.lastAccelerationTicks++;
    }

    private static class WanderGoal extends Goal {
        private final FireflyEntity fireflyEntity;
        private BlockPos currentBlock;
        private int currentBlockTimer;

        private WanderGoal(FireflyEntity fireflyEntity) {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
            this.fireflyEntity = fireflyEntity;
            this.currentBlock = fireflyEntity.getPosition();
        }

        @Override
        public void tick() {
            super.tick();

            if (this.currentBlock == this.fireflyEntity.getPosition()) {
                this.currentBlockTimer++;
            } else {
                this.currentBlock = this.fireflyEntity.getPosition();
                this.currentBlockTimer = 0;
            }

            if (this.currentBlockTimer > 60) { // Try to start moving if we're idle for 3 seconds
                this.moveToRandomLocation(this.fireflyEntity.getLook(0.0F)
                        .rotateYaw(MathHelper.nextFloat(this.fireflyEntity.rand, -80, -180)));
                this.currentBlockTimer = 0;
            }
        }

        public boolean shouldExecute() {
            return this.fireflyEntity.navigator.noPath();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.fireflyEntity.navigator.hasPath();
        }

        @Override
        public void startExecuting() {
            this.moveToRandomLocation(this.fireflyEntity.getLook(0.0F));
        }

        private void moveToRandomLocation(Vector3d angle) {
            Vector3d vector3d = this.getRandomLocation(angle);
            if (vector3d != null) {
                this.fireflyEntity.navigator.setPath(this.fireflyEntity.navigator.getPathToPos(new BlockPos(vector3d), 1), 0.75f);
            }
        }

        @Nullable
        private Vector3d getRandomLocation(Vector3d angle) {
            Vector3d vector3d2 = null;
            for (int i = 0; i < 3; i++) {
                vector3d2 = RandomPositionGenerator.findAirTarget(this.fireflyEntity, 3, 2, angle, ((float) Math.PI / 2F), 2, 2);
                if (vector3d2 != null) {
                    break;
                }
            }

            return vector3d2;
        }
    }

    private static class MoveToHoneyGoal extends MoveToBlockGoal {
        public MoveToHoneyGoal(CreatureEntity creature, double speedIn, int length) {
            super(creature, speedIn, length);
        }

        protected boolean shouldMoveTo(IWorldReader worldIn, BlockPos pos) {
            BlockState blockstate = worldIn.getBlockState(pos);
            return (blockstate.matchesBlock(Blocks.HONEY_BLOCK) || blockstate.matchesBlock(Blocks.HONEYCOMB_BLOCK));
        }
    }
}
