package fireflies.entity.firefly;

import fireflies.block.RedstoneIllumerinBlock;
import fireflies.client.ClientStuff;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.init.Registry;
import fireflies.misc.FireflyAbdomenParticleData;
import fireflies.misc.FireflyAbdomenRedstoneParticleData;
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
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    /**
     * (Client) Helper class for handling the firefly abdomen animations.
     */
    private final FireflyAbdomenAnimationHandler animationHandler;
    /**
     * (Client) The current animation this firefly has.
     */
    @Nullable
    public FireflyAbdomenAnimation abdomenAnimation;
    /**
     * (Client) The current abdomen particle this firefly has. (if any)
     */
    @Nullable
    public FireflyAbdomenParticle abdomenParticle;
    /**
     * (Client) the Y offset of where the abdomen particle should spawn / be at any given moment.
     */
    public float abdomenParticlePositionOffset;
    /**
     * (Client) The current alpha of the abdomen overlay on the firefly.
     */
    public float glowAlpha;
    /**
     * (Client) Is the glow alpha increasing or decreasing?
     */
    public boolean isGlowIncreasing;
    /**
     * (Client) Is the abdomen animation being set every tick
     */
    public boolean isAnimationOverridden;
    /**
     * How many ticks this firefly has been underwater for, used to start taking damage & converting from redstone firefly after 20 ticks.
     */
    private int underWaterTicks;
    /**
     * How many ticks this firefly has been rained on for, used for converting from redstone firefly after 20 ticks.
     */
    private int rainedOnTicks;
    /**
     * Is this fireflies current goal to pathfind towards honey?
     */
    private boolean isEntrancedByHoney;
    /**
     * Used for checking if the current firefly is redstone coated, this is updated every 20 ticks.
     */
    private boolean prevIsRedstoneCoated;
    /**
     * The radius (spherical) of which redstone illumerin blocks can be activated.
     */
    private int illumerinRadius = 5;
    /**
     * A list of redstone illumerin blocks this firefly is currently activating.
     */
    private final ArrayList<BlockPos> illumerinBlocks = new ArrayList<>(illumerinRadius * illumerinRadius * illumerinRadius);
    /**
     * DataParameter for if this firefly is redstone activated or not.
     */
    private static final DataParameter<Boolean> IS_REDSTONE_COATED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.animationHandler = new FireflyAbdomenAnimationHandler(this);
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
        // Register our DataParameters.
        super.registerData();
        this.dataManager.register(IS_REDSTONE_COATED, false);
    }

    /**
     * Get the value of {@link FireflyEntity#IS_REDSTONE_COATED}
     *
     * @param delayed If true, this method will return the cached value of {@link FireflyEntity#IS_REDSTONE_COATED}, the cache is updated every 20 ticks.
     * @return The DataParameter's value or the cached value, depending on the delayed parameter.
     */
    public boolean isRedstoneCoated(boolean delayed) {
        // I don't know what the performance implications are of getting a DataParameter value every frame & tick,
        // so I'm doing this just to be on the safe side ¯\_(ツ)_/¯
        if (delayed) {
            // Update the cached value
            // For the first 3 ticks it will return the actual current value, so things don't look screwy.
            if (this.ticksExisted % 20 == 0 || this.ticksExisted < 3) {
                this.prevIsRedstoneCoated = this.dataManager.get(IS_REDSTONE_COATED);
            }
            return this.prevIsRedstoneCoated;
        }

        return this.dataManager.get(IS_REDSTONE_COATED);
    }

    /**
     * Update {@link FireflyEntity#IS_REDSTONE_COATED}, and its cached value.
     *
     * @param b The new value to set the DataParameter to.
     */
    private void setRedstoneCovered(boolean b) {
        this.prevIsRedstoneCoated = b;
        this.dataManager.set(IS_REDSTONE_COATED, b);
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        // Write to our nbt data
        super.writeAdditional(nbt);
        nbt.putBoolean("IsRedstoneCoated", this.isRedstoneCoated(true));
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        // Read from our nbt data
        super.readAdditional(nbt);
        this.setRedstoneCovered(nbt.getBoolean("IsRedstoneCoated"));
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals.
        this.goalSelector.addGoal(0, new BreedGoal(this, 1f));
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.15f, Ingredient.fromItems(Items.HONEY_BOTTLE, Items.HONEY_BLOCK), false));
        this.goalSelector.addGoal(2, new FireflyEntity.EntrancedByHoneyGoal(this, 1.15f, 8));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.15f));
        this.goalSelector.addGoal(4, new FireflyEntity.WanderGoal(this, 1f, 1, false));
        this.goalSelector.addGoal(5, new SwimGoal(this));
    }

    @Override
    protected PathNavigator createNavigator(World world) {
        // Create the fireflies navigator
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

    /**
     * (Client) Unused for now - play a sound when the firefly starts glowing.
     */
    public void playGlowSound() {
        //this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), FirefliesRegistration.FIREFLY_GLOW.get(), SoundCategory.NEUTRAL, 0.33f, 1, false);
    }


    /**
     * (Client) Keeps up with the bobbing animation of the firefly.
     *
     * @return the precise position of where the abdomen particle should spawn / move to.
     */
    public double[] abdomenParticlePos() {
        return new double[] {
                this.getPosX() - -this.getWidth() * 0.35f * MathHelper.sin(this.renderYawOffset * ((float) Math.PI / 180F)),
                this.getPosYEye() + this.abdomenParticlePositionOffset + 0.3f,
                this.getPosZ() + -this.getWidth() * 0.35f * MathHelper.cos(this.renderYawOffset * ((float) Math.PI / 180F))
        };
    }


    /**
     * (Client) Spawns the abdomen particle at the precise location of the abdomen, with range being far as the firefly itself.
     */
    public void spawnAbdomenParticle() {
        if (this.world.isRemote && !this.isInvisible()) {
            double[] pos = this.abdomenParticlePos();
            this.world.addOptionalParticle(this.isRedstoneCoated(true)
                            ? new FireflyAbdomenRedstoneParticleData(this.getEntityId()) : new FireflyAbdomenParticleData(this.getEntityId()),
                    true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    /**
     * (Client) Spawn falling particles every so often, at the abdomen's position. Falling angle depends on fireflies speed.
     * This is called every tick.
     */
    private void spawnFallingDustParticles() {
        if (this.ticksExisted % 10 == 0 && this.rand.nextFloat() > 0.25f && this.glowAlpha > 0.25f && !this.isInvisible()) {
            // Redstone fireflies have less of a chance to spawn particles.
            if (this.isRedstoneCoated(true) && this.rand.nextFloat() > 0.5f)
                return;

            // no clue what this does lol
            Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.getPosX(), this.getPosY(), this.getPosZ());
            Vector3d vector3d1 = this.rotateVector(new Vector3d(this.rand.nextFloat(), -1.0D, this.rand.nextFloat() * Math.abs(this.getMotion().getZ()) * 10 + 2));
            Vector3d vector3d2 = vector3d1.scale(-5f + this.rand.nextFloat() * 2.0F);

            // Small random offset around the abdomen, baby fireflies don't have it
            float randPos = this.isChild() ? 0f : MathHelper.nextFloat(this.rand, -0.2f, 0.2f);
            // also have no clue what these numbers mean
            this.world.addParticle(
                    this.isRedstoneCoated(true) ? Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get() : Registry.FIREFLY_DUST_PARTICLE.get(),
                    vector3d.x + randPos, vector3d.y + (this.isChild() ? 1.1f : 1.35f) + randPos, vector3d.z + randPos,
                    vector3d2.x, vector3d2.y * -16, vector3d2.z);
        }
    }

    /**
     * (Client) Taken from {@link World#calculateInitialSkylight} because {@link World#isDaytime} does not work when called from the client.
     *
     * @return Is it currently daytime in this dimension?
     */
    private boolean isDayTimeClient() {
        double d0 = 1.0D - (double) (this.world.getRainStrength(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (this.world.getThunderStrength(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D *
                MathHelper.clamp(MathHelper.cos(this.world.func_242415_f(1.0F) * ((float) Math.PI * 2F)), -0.25D, 0.25D);
        int skylightSubtracted = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
        return !this.world.getDimensionType().doesFixedTimeExist() && skylightSubtracted < 4;
    }

    /**
     * Activates redstone illumerin blocks in a radius of {@link FireflyEntity#illumerinRadius}, called every half a second.
     */
    private void activateIllumerinBlocks() {
        // Baby fireflies have a smaller radius
        if (this.isChild()) illumerinRadius = 3;
        for (double x = this.getPosX() - illumerinRadius; x < this.getPosX() + illumerinRadius; x++) {
            for (double y = this.getPosY() - illumerinRadius; y < this.getPosY() + illumerinRadius; y++) {
                for (double z = this.getPosZ() - illumerinRadius; z < this.getPosZ() + illumerinRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.withinDistance(this.getPosition(), illumerinRadius))
                        continue;

                    BlockState state = this.world.getBlockState(pos);
                    if (state.getBlock() instanceof RedstoneIllumerinBlock && !state.get(RedstoneIllumerinBlock.POWERED)) {
                        this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.POWERED, Boolean.TRUE), 3);
                        this.illumerinBlocks.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Removes any invalid blocks from {@link FireflyEntity#illumerinBlocks}, called every half a second.
     */
    private void removeInvalidIllumerinBlocks() {
        this.illumerinBlocks.removeIf(pos -> {
            BlockState state = this.world.getBlockState(pos);
            // Remove if no longer an illumerin block
            if (!(state.getBlock() instanceof RedstoneIllumerinBlock)) {
                return true;
            }

            // Remove if out of range
            if (pos.distanceSq(this.getPosition()) > illumerinRadius * illumerinRadius) {
                this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.POWERED, Boolean.FALSE), 3);
                return true;
            }

            // Remove if we dead
            if (!this.isAlive()) {
                this.world.setBlockState(pos, state.with(RedstoneIllumerinBlock.POWERED, Boolean.FALSE), 3);
                return true;
            }

            return false;
        });
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            // Redstone fireflies are always on, regardless if it's daytime - unlike regular ones.
            if (this.isDayTimeClient() && !this.isRedstoneCoated(true)) {
                // Turn off during the day
                animationHandler.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.glowAlpha = 0;
            } else {
                animationHandler.updateAbdomenAnimation();
                animationHandler.updateGlowAnimation();
                this.spawnFallingDustParticles();

                // Fix abdomen particle not spawning with synced firefly on first glow cycle
                if (this.ticksExisted < 3 && this.glowAlpha > 0.1f) {
                    this.spawnAbdomenParticle();
                }
            }
        } else {
            if (this.isAIDisabled())
                return;

            if (this.isRedstoneCoated(true)) {
                if (this.ticksExisted % 10 == 0) {
                    this.removeInvalidIllumerinBlocks();
                    this.activateIllumerinBlocks();
                }
            }

            // Increase underWaterTicks by 1 if in water
            this.underWaterTicks = this.isInWaterOrBubbleColumn() ? this.underWaterTicks + 1 : 0;
            // Increase rainedOnTicks by 1 if in water
            this.rainedOnTicks = this.world.isRainingAt(this.getPosition()) ? this.rainedOnTicks + 1 : 0;

            if (this.underWaterTicks > 20) {
                // Once we're at 20, start taking damage
                this.attackEntityFrom(DamageSource.DROWN, 1.0F);
                // Additionally, try to convert back to a regular firefly
                if (this.isRedstoneCoated(true)) {
                    this.removeRedstoneCoated();
                }
            }

            // Convert back to regular firefly once we have been rained on for 20 ticks
            if (this.rainedOnTicks > 20 && this.isRedstoneCoated(true)) {
                this.removeRedstoneCoated();
                this.rainedOnTicks = 0;
            }
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack itemStack) {
        // Fireflies are bred with honey bottles
        return itemStack.getItem() == Items.HONEY_BOTTLE;
    }

    @Override
    protected void consumeItemFromStack(PlayerEntity player, ItemStack honeyBottle) {
        // Fix the honey bottle being consumed in it's entirety
        if (!player.abilities.isCreativeMode) {
            // Consume the honey bottle
            honeyBottle.shrink(1);
            // Give back a glass bottle
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
            ClientStuff.playFireflyLoopSound(this);
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
            // Stop giving a signal to nearby illumerin blocks.
            this.removeInvalidIllumerinBlocks();
        }
    }

    /**
     * Spawn a bunch of redstone particles around the firefly.
     *
     * @param amount How many particles should be spawned.
     * @param alpha  Alpha of the particle.
     */
    private void spawnRedstoneParticlePuff(boolean lowPowered, int amount, float alpha) {
        for (int i = 0; i < amount; i++) {
            float randPos = this.isChild() ? 0.22f : 0.66f;
            ((ServerWorld) (this.world)).spawnParticle(new RedstoneParticleData(lowPowered ? 0.5f : 1f, 0, 0, alpha),
                    this.getPosX() + MathHelper.nextFloat(this.rand, -randPos, randPos),
                    this.getPosY() + MathHelper.nextFloat(this.rand, 0f, randPos * 1.33f),
                    this.getPosZ() + MathHelper.nextFloat(this.rand, -randPos, randPos),
                    0, 0, 0, 0, 0);
        }
    }

    @Override
    public ActionResultType getEntityInteractionResult(PlayerEntity player, Hand hand) {
        /* Convert to redstone firefly */
        ItemStack itemstack = player.getHeldItem(hand);
        if (itemstack.getItem() == Items.REDSTONE && !this.isRedstoneCoated(false)) {
            // Set redstone coated
            this.setRedstoneCovered(true);
            if (!this.world.isRemote) {
                // Remove the redstone dust from players inventory if not in creative mode
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }
                // Spawn powered redstone dust particles
                this.spawnRedstoneParticlePuff(false, 5 + this.rand.nextInt(5), 1f);
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
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Puff out some particles on hit, the amount depending on damage.
        if (this.world.isRemote) {
            for (int i = 0; i < (int) MathHelper.clamp(amount, 2, 5); i++) {
                this.world.addParticle(
                        this.isRedstoneCoated(false) ? Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get() : Registry.FIREFLY_DUST_PARTICLE.get(),
                        this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
            }
        }
        return super.attackEntityFrom(source, amount);
    }

    //region Basic Overrides

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

    //endregion Basic Overrides

    /**
     * Adds/removes motion to the current amount.
     */
    private void addMotion(double x, double y, double z) {
        this.setMotion(this.getMotion().add(x, y, z));
    }

    /**
     * Convert from redstone firefly to regular firefly.
     */
    private void removeRedstoneCoated() {
        this.setRedstoneCovered(false);
        // Spawn powered off redstone particles
        this.spawnRedstoneParticlePuff(true, 5 + this.rand.nextInt(5), 0.5f);
        this.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f); // temp sound
    }

    //region AI

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
            return !this.fireflyEntity.isEntrancedByHoney && this.fireflyEntity.ticksExisted % 20 == 0 && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.fireflyEntity.isEntrancedByHoney && super.shouldContinueExecuting();
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
            if (this.fireflyEntity.ticksExisted % 100 == 0 && this.fireflyEntity.rand.nextFloat() > 0.75f && !this.fireflyEntity.isEntrancedByHoney) {
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

            if (this.fireflyEntity.ticksExisted % 20 == 0 && !this.fireflyEntity.isEntrancedByHoney) {
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
            this.fireflyEntity.isEntrancedByHoney = true;
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.fireflyEntity.isEntrancedByHoney = false;
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
                this.fireflyEntity.isEntrancedByHoney = true;
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
    //endregion AI
}
