package fireflies.entity.firefly;

import fireflies.block.RedstoneIllumerinBlock;
import fireflies.client.DoClientStuff;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.setup.Registry;
import net.minecraft.block.BlockState;
import net.minecraft.block.HoneyBlock;
import net.minecraft.block.LeavesBlock;
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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ITag;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    public FireflyAbdomenAnimation abdomenAnimation;
    public FireflyAbdomenParticle abdomenParticle;
    public float glowAlpha;
    public boolean glowIncreasing;
    public float abdomenParticlePositionOffset;
    private int underWaterTicks;
    private int isInRain;
    private boolean entrancedByHoney;
    private boolean isRedstoneActivatedDelayed;
    private static final int ILLUMERIN_RADIUS = 5;
    private final ArrayList<BlockPos> illumerinBlocks = new ArrayList<>(ILLUMERIN_RADIUS * ILLUMERIN_RADIUS * ILLUMERIN_RADIUS);
    private static final DataParameter<Boolean> REDSTONE_ACTIVATED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.moveController = new FireflyEntity.FlyingMovementHelper(this, 20, true);
        this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathPriority(PathNodeType.COCOA, -1.0F);
        this.setPathPriority(PathNodeType.FENCE, -1.0F);
    }

    public FireflyEntity createChild(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return Registry.FIREFLY.get().create(serverWorld);
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
        this.dataManager.register(REDSTONE_ACTIVATED, false);
    }

    public boolean isRedstoneActivated(boolean delayed) {
        // I don't know what the performance implications are of getting a DataParameter value every frame & tick,
        // so I'm doing this just to be on the safe side ¯\_(ツ)_/¯
        if (delayed) {
            if (this.ticksExisted % 20 == 0 || this.ticksExisted < 3) {
                this.isRedstoneActivatedDelayed = this.dataManager.get(REDSTONE_ACTIVATED);
            }
            return this.isRedstoneActivatedDelayed;
        } else {
            return this.dataManager.get(REDSTONE_ACTIVATED);
        }
    }

    public void setRedstoneActivated(boolean b) {
        this.isRedstoneActivatedDelayed = b;
        this.dataManager.set(REDSTONE_ACTIVATED, b);
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putBoolean("RedstoneActivated", this.isRedstoneActivated(true));
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        this.setRedstoneActivated(compound.getBoolean("RedstoneActivated"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreedGoal(this, 1f));
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.15f, Ingredient.fromItems(Items.HONEY_BOTTLE, Items.HONEY_BLOCK), false));
        this.goalSelector.addGoal(2, new FireflyEntity.EntrancedByHoneyGoal(this, 1.15f, 8));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.15f));
        this.goalSelector.addGoal(4, new FireflyEntity.WanderGoal(this, 1f, 1, false));
        this.goalSelector.addGoal(5, new SwimGoal(this));
    }

    @Override
    protected PathNavigator createNavigator(World world) {
        FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, world) {
            @Override
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.isAirBlock(pos.down());
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
        if (this.isRedstoneActivated(true)) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.ON);
            return;
        }

        if (this.getPosition().getY() < 0 || this.getPosition().getY() >= 256) {
            this.setAbdomenAnimation(FireflyAbdomenAnimation.FRANTIC);
            return;
        }

        // Set the animation based on the current biome.
        Biome currentBiome = this.world.getBiome(this.getPosition());
        switch (currentBiome.getCategory()) {
            case SWAMP:
                this.setAbdomenAnimation(FireflyAbdomenAnimation.CALM);
                break;
            case FOREST:
                Optional<RegistryKey<Biome>> biomeRegistryKey = this.world.func_242406_i(this.getPosition());
                if (Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST))
                        || Objects.equals(biomeRegistryKey, Optional.of(Biomes.DARK_FOREST_HILLS))) {
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

    public void playGlowSound() {
        //this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), FirefliesRegistration.FIREFLY_GLOW.get(), SoundCategory.NEUTRAL, 0.33f, 1, false);
    }

    private void glowAnimation(float increaseAmount, float decreaseAmount, float startIncreasingChance, float startDecreasingChance) {
        // Add / deplete the glow alpha, baby fireflies go a little faster.
        this.glowAlpha += this.glowIncreasing
                ? (this.isChild() ? increaseAmount * 1.25f : increaseAmount) : (this.isChild() ? -decreaseAmount * 1.25f : -decreaseAmount);
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
                this.playGlowSound();
            }
        }
    }

    private void updateGlowAnimation() {
        // Update the glow animation every tick.
        switch (this.abdomenAnimation) {
            case OFF:
                this.glowAlpha = 0;
                this.glowIncreasing = false;
                break;
            case ON:
                this.glowAlpha = 1;
                this.glowIncreasing = false;
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

    /**
     * Keeps up with the bobbing animation of the firefly.
     *
     * @return the precise position of where the abdomen particle should spawn / move to.
     */
    public double[] abdomenParticlePos() {
        return new double[]{
                this.getPosX() - -this.getWidth() * 0.35f * MathHelper.sin(this.renderYawOffset * ((float) Math.PI / 180F)),
                this.getPosYEye() + this.abdomenParticlePositionOffset + 0.3f,
                this.getPosZ() + -this.getWidth() * 0.35f * MathHelper.cos(this.renderYawOffset * ((float) Math.PI / 180F))
        };
    }

    public void spawnAbdomenParticle() {
        if (this.world.isRemote && !this.isInvisible()) {
            // Spawn the abdomen particle at the proper position, with range being far as the firefly itself.
            double[] pos = this.abdomenParticlePos();
            this.world.addOptionalParticle(this.isRedstoneActivated(true)
                            ? new FireflyAbdomenRedstoneParticleData(this.getEntityId()) : new FireflyAbdomenParticleData(this.getEntityId()),
                    true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    private void spawnFallingDustParticles() {
        if (this.ticksExisted % 8 == 0 && this.rand.nextFloat() > 0.25f && this.glowAlpha > 0.25f && !this.isInvisible()) {
            // Spawn falling particles every so often, at the abdomen's position. Falling angle depends on fireflies speed.
            if (this.isRedstoneActivated(true) && this.rand.nextFloat() > 0.5f)
                return;

            Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.getPosX(), this.getPosY(), this.getPosZ());
            Vector3d vector3d1 = this.rotateVector(new Vector3d(this.rand.nextFloat(), -1.0D, this.rand.nextFloat() * Math.abs(this.getMotion().getZ()) * 10 + 2));
            Vector3d vector3d2 = vector3d1.scale(-5f + this.rand.nextFloat() * 2.0F);

            float randPos = this.isChild() ? 0f : MathHelper.nextFloat(this.rand, -0.2f, 0.2f);
            this.world.addParticle(this.isRedstoneActivated(true)
                            ? Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get() : Registry.FIREFLY_DUST_PARTICLE.get(),
                    vector3d.x + randPos, vector3d.y + 1.35f + randPos, vector3d.z + randPos,
                    vector3d2.x, vector3d2.y * -16, vector3d2.z);
        }
    }

    private boolean isDayTimeClient() {
        // Taken from World#calculateInitialSkylight(), since I couldn't figure out how to check the skylight from the chunk data :I
        double d0 = 1.0D - (double) (this.world.getRainStrength(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (this.world.getThunderStrength(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D *
                MathHelper.clamp(MathHelper.cos(this.world.func_242415_f(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !this.world.getDimensionType().doesFixedTimeExist() && skylightSubtracted < 4;
    }

    private void activateIllumerinBlocks() {
        // Activate redstone illumerin blocks in a radius
        for (double x = this.getPosX() - ILLUMERIN_RADIUS; x < this.getPosX() + ILLUMERIN_RADIUS; x++) {
            for (double y = this.getPosY() - ILLUMERIN_RADIUS; y < this.getPosY() + ILLUMERIN_RADIUS; y++) {
                for (double z = this.getPosZ() - ILLUMERIN_RADIUS; z < this.getPosZ() + ILLUMERIN_RADIUS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.withinDistance(this.getPosition(), ILLUMERIN_RADIUS))
                        continue;

                    BlockState state = this.world.getBlockState(pos);
                    if (state.getBlock() instanceof RedstoneIllumerinBlock && !state.get(RedstoneIllumerinBlock.LOCKED)) {
                        this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.LOCKED, Boolean.TRUE).with(RedstoneIllumerinBlock.POWERED, Boolean.TRUE), 3);
                        this.illumerinBlocks.add(pos);
                    }
                }
            }
        }
    }

    private void removeInvalidIllumerinBlocks() {
        this.illumerinBlocks.removeIf(pos -> {
            BlockState state = this.world.getBlockState(pos);
            // Remove if no longer a illumerin block
            if (!(state.getBlock() instanceof RedstoneIllumerinBlock)) {
                return true;
            }

            // Remove if out of range
            if (pos.distanceSq(this.getPosition()) > ILLUMERIN_RADIUS * ILLUMERIN_RADIUS) {
                this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.LOCKED, Boolean.FALSE), 3);
                this.world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 1);
                return true;
            }

            // Remove if we dead
            if (!this.isAlive()) {
                this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.LOCKED, Boolean.FALSE), 3);
                this.world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 1);
            }

            return false;
        });
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            if (this.isDayTimeClient() && !this.isRedstoneActivated(true)) {
                // Turn off during the day
                this.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.glowAlpha = 0;
            } else {
                this.updateAbdomenAnimation();
                this.updateGlowAnimation();
                this.spawnFallingDustParticles();

                // Fix abdomen particle not spawning with synced firefly on first glow cycle
                if (this.ticksExisted < 3 && this.glowAlpha > 0.1f) {
                    this.spawnAbdomenParticle();
                }
            }
        } else if (this.isRedstoneActivated(true)) {
            if (this.ticksExisted % 10 == 0) {
                this.removeInvalidIllumerinBlocks();
                this.activateIllumerinBlocks();
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
            // Start playing the flight loop sound
            DoClientStuff doClientStuff = new DoClientStuff();
            doClientStuff.playFireflyLoopSound(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (this.world.isRemote) {
            // Remove ourselves from all the synced lists, if any.
            FireflyGlowSync.calmSyncedFireflies.syncedFireflies.remove(this);
            FireflyGlowSync.starryNightSyncedFireflies.syncedFireflies.remove(this);
        } else {
            this.removeInvalidIllumerinBlocks();
        }
    }

    private void spawnRedstoneParticlePuff(int amount, float a) {
        for (int i = 0; i < amount; i++) {
            ((ServerWorld) (this.world)).spawnParticle(new RedstoneParticleData(1f, 0, 0, a),
                    this.getPosX() + MathHelper.nextFloat(this.rand, -0.6f, 0.6f),
                    this.getPosY() + MathHelper.nextFloat(this.rand, 0f, 0.8f),
                    this.getPosZ() + MathHelper.nextFloat(this.rand, -0.6f, 0.6f),
                    0, 0, 0, 0, 0);
        }
    }

    @Override
    public ActionResultType getEntityInteractionResult(PlayerEntity player, Hand hand) {
        /* Convert to redstone firefly */
        ItemStack itemstack = player.getHeldItem(hand);
        if (itemstack.getItem() == Items.REDSTONE && !this.isRedstoneActivated(false)) {
            // Set redstone activated DataParameter.
            this.setRedstoneActivated(true);
            if (!this.world.isRemote) {
                // Remove from inventory if not in creative mode
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }
                // Spawn activated redstone dust particles
                this.spawnRedstoneParticlePuff(5 + this.rand.nextInt(5), 1f);
            } else {
                // Play the conversion sound to the client
                player.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f);
                // Destroy the current abdomen particle.
                if (this.abdomenParticle != null) this.abdomenParticle.setExpired();
                // Spawn the new redstone abdomen particle.
                this.spawnAbdomenParticle();
            }
            return ActionResultType.SUCCESS;
        }
        return super.getEntityInteractionResult(player, hand);
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
        // puff out some particles on hit, the amount depending on damage.
        if (this.world.isRemote) {
            for (int i = 0; i < (int) MathHelper.clamp(amount, 2, 5); i++) {
                this.world.addParticle(
                        this.isRedstoneActivated(false) ? Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get() : Registry.FIREFLY_DUST_PARTICLE.get(),
                        this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
            }
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
        this.addMotion(0f, 0.01f, 0f);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vector3d getLeashStartPosition() {
        return new Vector3d(0.0D, (0.5F * this.getEyeHeight()), (this.getWidth() * 0.2F));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isInRangeToRenderDist(double distance) {
        // About 2x the render distance of players.
        double d0 = 128 * getRenderDistanceWeight();
        return distance < d0 * d0;
    }

    /**
     * Adds/removes motion to the current amount.
     */
    private void addMotion(double x, double y, double z) {
        this.setMotion(this.getMotion().add(x, y, z));
    }

    private void removeRedstoneActivated() {
        // Convert from redstone firefly to regular firefly.
        this.setRedstoneActivated(false);
        // Spawn "off" particles
        this.spawnRedstoneParticlePuff(3 + this.rand.nextInt(3), 0.75f);
    }

    @Override
    protected void updateAITasks() {
        // Note that this will not run if NoAI is set to true. (may seem obvious but I fell to it a couple times)
        super.updateAITasks();

        this.underWaterTicks = this.isInWaterOrBubbleColumn() ? this.underWaterTicks + 1 : 0;
        this.isInRain = this.world.isRainingAt(this.getPosition()) ? this.isInRain + 1 : 0;

        if (this.underWaterTicks > 20) {
            this.attackEntityFrom(DamageSource.DROWN, 1.0F);
            if (this.isRedstoneActivated(true)) {
                this.removeRedstoneActivated();
            }
        }

        if (this.isInRain > 20 && this.isRedstoneActivated(true)) {
            this.removeRedstoneActivated();
        }
    }

    private static class WanderGoal extends RandomWalkingGoal {
        private final FireflyEntity fireflyEntity;
        private final World world;
        private static final EntityPredicate FIREFLY_PREDICATE = (new EntityPredicate()).setDistance(8f).allowFriendlyFire().allowInvulnerable().setIgnoresLineOfSight();

        private WanderGoal(FireflyEntity fireflyEntity, double speed, int chance, boolean stopWhenIdle) {
            super(fireflyEntity, speed, chance, stopWhenIdle);
            this.fireflyEntity = fireflyEntity;
            this.world = fireflyEntity.world;
        }

        @Override
        public boolean shouldExecute() {
            return !this.fireflyEntity.entrancedByHoney && this.fireflyEntity.ticksExisted % 20 == 0 && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.fireflyEntity.entrancedByHoney && super.shouldContinueExecuting();
        }

        @Nullable
        @Override
        protected Vector3d getPosition() {
            Vector3d position;

            // Find some place to go in the air.
            position = RandomPositionGenerator.findAirTarget(this.fireflyEntity, 3, 1,
                    this.fireflyEntity.getLook(0), (float) (Math.PI / 2f), 2, 1);

            // Try again...
            if (position == null) {
                position = RandomPositionGenerator.findRandomTarget(this.fireflyEntity, 3, 1);
            }

            // Ok, we'll just try to go to another firefly then.
            if (position == null) {
                // Search within 8 block radius.
                FireflyEntity closestFirefly = this.world.getClosestEntityWithinAABB(FireflyEntity.class, FIREFLY_PREDICATE, this.fireflyEntity,
                        this.fireflyEntity.getPosX(), this.fireflyEntity.getPosY(), this.fireflyEntity.getPosZ(),
                        this.fireflyEntity.getBoundingBox().grow(8f, 2.5f, 8f));

                if (closestFirefly != null) {
                    position = closestFirefly.getPositionVec();
                }
            }

            if (position != null) {
                BlockPos blockPos = new BlockPos(position);

                // Don't land on the ground if its avoidable..
                if (this.world.getBlockState(blockPos.down()).isSolid()
                        && this.world.isAirBlock(blockPos.up())) {
                    position = position.add(0, 1.5f, 0);
                }

                // Don't go too high.
                if (position.getY() - this.fireflyEntity.getPosY() > 2f) {
                    position = position.add(0, -(position.getY() - this.fireflyEntity.getPosY() - 1f), 0);
                }

                // Avoid leaves.. They float to the top of trees which is unwanted.
                if (this.world.getBlockState(blockPos.down()).getBlock() instanceof LeavesBlock) {
                    for (int i = 0; i < 3; i++) {
                        if (!(this.world.getBlockState(blockPos.down(i)).getBlock() instanceof LeavesBlock))
                            position = position.add(0, -i, 0);
                    }
                }
            }

            return position;
        }
    }

    private static class FlyingMovementHelper extends FlyingMovementController {
        private final FireflyEntity fireflyEntity;
        private final World world;
        private boolean isHighUp;
        private BlockPos prevBlockPos;

        private FlyingMovementHelper(FireflyEntity fireflyEntity, int maximumRotateAngle, boolean affectedByGravity) {
            super(fireflyEntity, maximumRotateAngle, affectedByGravity);
            this.fireflyEntity = fireflyEntity;
            this.world = fireflyEntity.world;
            this.prevBlockPos = fireflyEntity.getPosition();
        }

        private void moveForward(float multiplier, float y, float speed) {
            this.fireflyEntity.navigator.tryMoveToXYZ(
                    this.fireflyEntity.getPosX() + this.fireflyEntity.getLookVec().getX() * multiplier,
                    this.fireflyEntity.getPosY() + y,
                    this.fireflyEntity.getPosZ() + this.fireflyEntity.getLookVec().getZ() * multiplier, speed);
        }

        @Override
        public void tick() {
            super.tick();

            // Do random accelerations every so often.
            if (this.fireflyEntity.ticksExisted % 100 == 0 && this.fireflyEntity.rand.nextFloat() > 0.75f && !this.fireflyEntity.entrancedByHoney) {
                final float accelAmount = 0.125f;
                this.fireflyEntity.addMotion(
                        MathHelper.clamp(this.fireflyEntity.getLook(0).getX(), -accelAmount, accelAmount),
                        this.world.isAirBlock(this.fireflyEntity.getPosition().down()) ? 0f : 0.075f,
                        MathHelper.clamp(this.fireflyEntity.getLook(0).getZ(), -accelAmount, accelAmount));
            }

            // Don't vertically too quickly.
            if (this.fireflyEntity.getMotion().getY() < -0.04f) {
                this.fireflyEntity.addMotion(0, 0.025f, 0);
            } else if (this.fireflyEntity.getMotion().getY() > 0.04f) {
                this.fireflyEntity.addMotion(0, -0.025f, 0);
            }

            if (this.fireflyEntity.ticksExisted % 20 == 0 && !this.fireflyEntity.entrancedByHoney) {
                // Stay off the ground
                if (this.world.getBlockState(this.fireflyEntity.getPosition().down()).isSolid() || this.fireflyEntity.isOnGround()) {
                    this.moveForward(1.2f, 1.5f, 0.85f);
                }

                // And also don't stay too high in the air
                for (int i = 1; i < 4; i++) {
                    isHighUp = !this.world.getBlockState(this.fireflyEntity.getPosition().down(i)).isSolid();
                    if (!isHighUp) break;
                }
                if (isHighUp) {
                    this.moveForward(1.2f, -2f, 0.85f);
                }

                // Try to not stay idle for more than a second.
                if (this.prevBlockPos == this.fireflyEntity.getPosition()) {
                    this.moveForward(MathHelper.nextFloat(this.fireflyEntity.rand, -3f, 3f), this.fireflyEntity.rand.nextFloat(), 1f);
                }
                this.prevBlockPos = this.fireflyEntity.getPosition();
            }
        }
    }

    private static class EntrancedByHoneyGoal extends MoveToBlockGoal {
        private final FireflyEntity fireflyEntity;
        private final World world;

        private EntrancedByHoneyGoal(FireflyEntity fireflyEntity, double speed, int length) {
            super(fireflyEntity, speed, length);
            this.fireflyEntity = fireflyEntity;
            this.world = fireflyEntity.world;
        }

        private Vector3d getDestinationBlockCentered() {
            return new Vector3d(this.destinationBlock.getX() + 0.5f, this.destinationBlock.getY() + 0.5f, this.destinationBlock.getZ() + 0.5f);
        }

        private boolean isHoneyBlockVisible(BlockPos destinationBlock) {
            BlockRayTraceResult rayTraceResult = this.world.rayTraceBlocks(new RayTraceContext(
                    this.fireflyEntity.getPositionVec().add(0, this.fireflyEntity.getEyeHeight(), 0),
                    new Vector3d(destinationBlock.getX() + 0.5f, destinationBlock.getY() + 0.5f, destinationBlock.getZ() + 0.5f),
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this.fireflyEntity));

            return this.fireflyEntity.world.getBlockState(rayTraceResult.getPos()).getBlock() instanceof HoneyBlock;
        }

        @Override
        protected int getRunDelay(CreatureEntity creatureIn) {
            return 20 + this.fireflyEntity.rand.nextInt(20);
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            this.fireflyEntity.entrancedByHoney = true;
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.fireflyEntity.entrancedByHoney = false;
        }

        @Override
        public void tick() {
            super.tick();
            Vector3d destinationBlockCentered = this.getDestinationBlockCentered();
            // Stare at the honey block.
            this.fireflyEntity.getLookController()
                    .setLookPosition(destinationBlockCentered.getX(), destinationBlockCentered.getY(), destinationBlockCentered.getZ(),
                            this.fireflyEntity.getHorizontalFaceSpeed(), this.fireflyEntity.getVerticalFaceSpeed());

            // Keep close to the honey block.
            if (this.fireflyEntity.ticksExisted % 20 == 0 && this.fireflyEntity
                    .getDistanceSq(destinationBlockCentered.getX(), destinationBlockCentered.getY(), destinationBlockCentered.getZ()) > 3f) {
                this.attemptToMove();
            }
        }

        @Override
        protected boolean shouldMoveTo(IWorldReader worldReader, BlockPos blockPos) {
            if (this.world.getBlockState(blockPos).getBlock() instanceof HoneyBlock && this.isHoneyBlockVisible(blockPos)) {
                this.fireflyEntity.entrancedByHoney = true;
                return true;
            }
            return false;
        }

        @Override
        protected void attemptToMove() {
            Vector3d destinationBlockCentered = this.getDestinationBlockCentered();
            this.fireflyEntity.getNavigator().tryMoveToXYZ(
                    destinationBlockCentered.getX(), destinationBlockCentered.getY(), destinationBlockCentered.getZ(), this.movementSpeed);
        }
    }
}
