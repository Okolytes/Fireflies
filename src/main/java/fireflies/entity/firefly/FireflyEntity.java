package fireflies.entity.firefly;

import fireflies.client.DoClientStuff;
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
import java.util.Objects;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    private static final DataParameter<Integer> ABDOMEN_ANIMATION = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> GLOW_ALPHA = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> GLOW_INCREASING = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    public float abdomenParticlePositionOffset;
    private int underWaterTicks;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.moveController = new FlyingMovementController(this, 20, true);
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
        this.dataManager.register(ABDOMEN_ANIMATION, FireflyAbdomenAnimation.OFF.ordinal());
        this.dataManager.register(GLOW_ALPHA, 0f);
        this.dataManager.register(GLOW_INCREASING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25D, Ingredient.fromItems(Items.HONEY_BOTTLE), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.25f));
        this.goalSelector.addGoal(5, new WanderGoal());
        this.goalSelector.addGoal(6, new SwimGoal(this));
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
    }

    public FireflyAbdomenAnimation getAnimation() {
        return FireflyAbdomenAnimation.values()[this.dataManager.get(ABDOMEN_ANIMATION)];
    }

    public void setAbdomenAnimation(FireflyAbdomenAnimation newAnimation) {
        if (!this.world.isRemote && newAnimation != this.getAnimation()) { // Probably don't wanna access list(s) every tick
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
        this.dataManager.set(ABDOMEN_ANIMATION, newAnimation.ordinal());
    }

    public float getGlowAlpha() {
        return this.dataManager.get(GLOW_ALPHA);
    }

    public void setGlowAlpha(float newAlpha) {
        this.dataManager.set(GLOW_ALPHA, newAlpha);
    }

    public boolean getGlowIncreasing() {
        return this.dataManager.get(GLOW_INCREASING);
    }

    public void setGlowIncreasing(boolean newBool) {
        this.dataManager.set(GLOW_INCREASING, newBool);
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    public void spawnAbdomenParticle() {
        if (!this.world.isRemote) {
            ((ServerWorld) (this.world)).spawnParticle(new FireflyAbdomenParticleData(this.getEntityId()), this.getPosX(), this.getPosY(), this.getPosZ(),
                    1, 0, 0, 0, 0);
        }
    }

    private void spawnFallingDustParticles() {
        // no i don't understand this i stole it from the squid code
        if (this.getGlowAlpha() > 0.15f && this.rand.nextFloat() > 0.9f) {
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

    private void updateGlowAnimation() {
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

    @Override
    public void livingTick() {
        super.livingTick();
        if (this.world.isRemote) {
            this.spawnFallingDustParticles();
        } else {
            if (this.world.isDaytime()) {
                this.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.setGlowAlpha(0);
                return;
            }

            this.updateAbdomenAnimation();
            this.updateGlowAnimation();
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
        if (!this.world.isRemote) {
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

    public class WanderGoal extends Goal {
        private final FireflyEntity fireflyEntity;

        @Nullable
        private BlockPos favouritePos;
        private int favouritePosTimer;
        private int newFavouritePosTimer;

        public WanderGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
            fireflyEntity = FireflyEntity.this;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.favouritePos == null) {
                this.favouritePos = fireflyEntity.getPosition();
            }

            if (this.fireflyEntity.getPosition() == this.favouritePos) {
                this.favouritePosTimer++;
            } else {
                this.newFavouritePosTimer++;
            }

            if (this.newFavouritePosTimer > this.favouritePosTimer) {
                this.favouritePos = this.fireflyEntity.getPosition();
                this.favouritePosTimer = 0;
                this.newFavouritePosTimer = 0;
            }
        }

        public boolean shouldExecute() {
            return this.fireflyEntity.navigator.noPath() && this.fireflyEntity.rand.nextFloat() > 0.9f;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.fireflyEntity.navigator.hasPath();
        }

        @Override
        public void startExecuting() {
            if (this.fireflyEntity.rand.nextFloat() > 0.5f) {
                Vector3d vector3d = this.getRandomLocation();
                if (vector3d != null) {
                    this.fireflyEntity.navigator.setPath(this.fireflyEntity.navigator.getPathToPos(new BlockPos(vector3d), 1), 0.75f);
                }
            } else {
                if (this.favouritePos != null) {
                    this.fireflyEntity.navigator.setPath(this.fireflyEntity.navigator.getPathToPos(this.favouritePos, 1), 0.75f);
                }
            }
        }

        @Nullable
        private Vector3d getRandomLocation() {
            Vector3d vector3d = this.fireflyEntity.getLook(0.0F);
            Vector3d vector3d2 = RandomPositionGenerator.findAirTarget(this.fireflyEntity, 4, 3, vector3d, ((float) Math.PI / 2F), 2, 1);
            return vector3d2 != null ? vector3d2 : RandomPositionGenerator.findGroundTarget(this.fireflyEntity, 4, 2, -2, vector3d, ((float) Math.PI / 2F));
        }
    }
}
