package fireflies.entity.firefly;

import fireflies.client.DoClientStuff;
import fireflies.setup.FirefliesRegistration;
import net.minecraft.block.BlockState;
import net.minecraft.block.HoneyBlock;
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
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ITag;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
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
import java.util.Optional;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    public FireflyAbdomenAnimation abdomenAnimation;
    public float glowAlpha;
    public boolean glowIncreasing;
    public float abdomenParticlePositionOffset;
    private int underWaterTicks;
    private int lastAccelerationTicks;
    private boolean entrancedByHoney;
    @Nullable
    private BlockPos honeyBlock = null;

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
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreedGoal(this, 1f));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25D, Ingredient.fromItems(Items.HONEY_BOTTLE, Items.HONEY_BLOCK), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.75f));
        this.goalSelector.addGoal(5, new WanderGoal(this));
        this.goalSelector.addGoal(7, new SwimGoal(this));
    }

    @Override
    protected PathNavigator createNavigator(World world) {
        FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, world) {
            @Override
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.isAirBlock(pos);
            }
        };
        flyingpathnavigator.setCanOpenDoors(false);
        flyingpathnavigator.setCanSwim(false);
        flyingpathnavigator.setCanEnterDoors(true);
        return flyingpathnavigator;
    }

    @Override
    public float getBlockPathWeight(BlockPos blockPos, IWorldReader world) {
        return world.isAirBlock(blockPos) ? 10.0F : 0.0F;
    }

    public void setAbdomenAnimation(FireflyAbdomenAnimation newAnimation) {
        if (newAnimation != this.abdomenAnimation) { // Probably don't wanna try do list(s) stuff every tick
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
            this.abdomenAnimation = newAnimation;
        }
    }

    private void updateAbdomenAnimation() {
        if (this.getPosition().getY() < 0 || this.getPosition().getY() >= 256) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.FRANTIC);
            return;
        }

        Biome currentBiome = this.world.getBiome(this.getPosition());
        switch (currentBiome.getCategory()) {
            case SWAMP:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM);
                break;
            case FOREST:
                Optional<RegistryKey<Biome>> biomeRegistryKey = this.world.func_242406_i(this.getPosition());
                if (Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST)) || Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST_HILLS))) {
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

    public double[] abdomenParticlePos() {
        return new double[]{
                this.getPosX() - -this.getWidth() * 0.35f * MathHelper.sin(this.renderYawOffset * ((float) Math.PI / 180F)),
                this.getPosYEye() + this.abdomenParticlePositionOffset + 0.3f,
                this.getPosZ() + -this.getWidth() * 0.35f * MathHelper.cos(this.renderYawOffset * ((float) Math.PI / 180F))
        };
    }

    public void spawnAbdomenParticle() {
        if (this.world.isRemote && !this.isInvisible()) {
            double[] pos = this.abdomenParticlePos();
            this.world.addOptionalParticle(new FireflyAbdomenParticleData(this.getEntityId()), true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    private void spawnFallingDustParticles() {
        if (this.ticksExisted % 8 == 0 && this.glowAlpha > 0.25f && !this.isInvisible()) {
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

    // Taken from World#calculateInitialSkylight()
    private boolean isDayTime() {
        double d0 = 1.0D - (double) (this.world.getRainStrength(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (this.world.getThunderStrength(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D * MathHelper.clamp(MathHelper.cos(this.world.func_242415_f(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !this.world.getDimensionType().doesFixedTimeExist() && skylightSubtracted < 4;
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            if (this.isDayTime()) {
                this.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.glowAlpha = 0;
            } else {
                this.updateAbdomenAnimation();
                this.updateGlowAnimation();
                this.spawnFallingDustParticles();
            }
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
                    (int) MathHelper.clamp(amount, 3, 10), 0, 0, 0, 0);
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
        this.addMotion(0.0D, 0.01D, 0.0D);
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

    private void addMotion(double x, double y, double z) {
        this.setMotion(this.getMotion().add(x, y, z));
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

        if (!this.entrancedByHoney) {
            this.lastAccelerationTicks++;
            if (MathHelper.sqrt(Entity.horizontalMag(this.getMotion())) < 0.015f && this.lastAccelerationTicks > 60 && !this.world.isRemote) {
                final float accelAmount = 0.025f;
                this.addMotion(
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
                    this.addMotion(0, 0.005f, 0);
                } else if (this.world.isAirBlock(this.getPosition().down(3))) {
                    this.addMotion(0, -0.01f, 0);
                }
            }

            if (this.ticksExisted % 60 == 0) {
                this.tryMoveToHoney();
            }
        } else if (this.honeyBlock != null) {
            if (!this.getPosition().withinDistance(this.getPositionVec(), 3f)) {
                this.navigator.setPath(this.navigator.getPathToPos(this.honeyBlock, 1), 0.95f);
            }
        }
    }

    private void tryMoveToHoney() {
        final int radius = 6;
        double closestHoneyBlockDistSqr = Double.MAX_VALUE;
        BlockPos closestHoneyBlock = null;
        for (BlockPos blockPos :
                BlockPos.getAllInBoxMutable(this.getPosition().add(radius, radius, radius), this.getPosition().add(-radius, -radius, -radius))) {
            if (this.world.getBlockState(blockPos).getBlock() instanceof HoneyBlock) {
                double distSqr = blockPos.distanceSq(this.getPosition());
                if (distSqr < closestHoneyBlockDistSqr) {
                    closestHoneyBlockDistSqr = distSqr;
                    closestHoneyBlock = blockPos;
                }
            }
        }

        if (closestHoneyBlock != null) {
            this.entrancedByHoney = true;
            this.honeyBlock = closestHoneyBlock;
            this.navigator.setPath(this.navigator.getPathToPos(closestHoneyBlock, 1), 0.9f);
        }
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

            if (this.currentBlockTimer > 60 && !this.fireflyEntity.entrancedByHoney) { // Try to start moving if we're idle for 3 seconds
                this.tryMoveToRandomLocation(this.fireflyEntity.getLook(0.0F)
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
            this.tryMoveToRandomLocation(this.fireflyEntity.getLook(0.0F));
        }

        private void tryMoveToRandomLocation(Vector3d angle) {
            if (this.fireflyEntity.entrancedByHoney && this.fireflyEntity.honeyBlock != null) {
                if (!(this.fireflyEntity.world.getBlockState(this.fireflyEntity.honeyBlock).getBlock() instanceof HoneyBlock)) {
                    this.fireflyEntity.entrancedByHoney = false;
                    this.fireflyEntity.honeyBlock = null;
                    return;
                }

                this.fireflyEntity.navigator.setPath(this.fireflyEntity.navigator.getPathToPos(this.fireflyEntity.honeyBlock, 1), 0.9f);
                return;
            }

            Vector3d vector3d = RandomPositionGenerator.findAirTarget(this.fireflyEntity, 3, 1, angle, ((float) Math.PI / 2F), 3, 1);
            if (vector3d != null) {
                BlockPos blockPos = new BlockPos(vector3d);
                if (blockPos.getY() - this.currentBlock.getY() > 2)
                    return;

                if (this.fireflyEntity.world.getBlockState(blockPos).isSolid()) {
                    blockPos = blockPos.up();
                }

                for (int i = 1; i < 10; i++) {
                    if (i > 2 && !this.fireflyEntity.world.getBlockState(blockPos.down(i)).isSolid()) {
                        blockPos = blockPos.down(i);
                        break;
                    }
                }

                this.fireflyEntity.navigator.setPath(this.fireflyEntity.navigator.getPathToPos(blockPos, 1), 0.75f);
                // Debugging purposes lol
                if (!this.fireflyEntity.world.isRemote) {
                    ((ServerWorld) this.fireflyEntity.world).spawnParticle(ParticleTypes.FLAME, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            8, 0, 0, 0, 0);
                }
            }
        }
    }
}
