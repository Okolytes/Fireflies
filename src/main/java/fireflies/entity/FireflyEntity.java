package fireflies.entity;

import fireflies.Registry;
import fireflies.block.GlassJarBlock;
import fireflies.block.IllumerinLamp;
import fireflies.client.ClientStuff;
import fireflies.client.particle.FireflyAbdomenParticle;
import fireflies.misc.FireflyParticleData;
import net.minecraft.block.*;
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
import net.minecraft.particles.IParticleData;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    private static final DataParameter<Boolean> IS_REDSTONE_COATED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> HAS_ILLUMERIN = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ILLUMERIN_DEPOSITED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.VARINT);
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
     * The position of the current glass jar that the firefly is filling, if any.
     */
    @Nullable
    private BlockPos currentGlassJar;
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
     * The radius of which illumerin lamps can be activated.
     */
    private int illumerinRadius = 5;
    /**
     * A list of illumerin lamps this firefly is currently activating.
     */
    private final ArrayList<BlockPos> illumerinBlocks = new ArrayList<>(illumerinRadius * illumerinRadius * illumerinRadius);
    /**
     * Used for checking if the current firefly is redstone coated, this is updated every 20 ticks.
     */
    private boolean cachedIsRedstoneCoated;
    /**
     * Used for checking if the current firefly has illumerin on itself, this is updated every 20 ticks.
     */
    private boolean cachedHasIllumerin;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.animationHandler = new FireflyAbdomenAnimationHandler(this);
        this.moveController = new FireflyEntity.FlyingMovementHelper();
        this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathPriority(PathNodeType.COCOA, -1.0F);
        this.setPathPriority(PathNodeType.FENCE, -1.0F);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return LivingEntity.registerAttributes()
                .createMutableAttribute(Attributes.MAX_HEALTH, 6.0D)
                .createMutableAttribute(Attributes.FLYING_SPEED, 0.25F)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.15F)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D);
    }

    public FireflyEntity createChild(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return Registry.FIREFLY.get().create(serverWorld);
    }

    @Override
    protected void registerData() {
        // Register our DataParameters.
        super.registerData();
        this.dataManager.register(IS_REDSTONE_COATED, false);
        this.dataManager.register(HAS_ILLUMERIN, false);
        this.dataManager.register(ILLUMERIN_DEPOSITED, 0);
    }

    /**
     * Get the value of {@link FireflyEntity#IS_REDSTONE_COATED}
     *
     * @param getCached If true, this method will return the cached value of {@link FireflyEntity#IS_REDSTONE_COATED}, the cache is updated every 20 ticks.
     * @return The DataParameter's value or the cached value, depending on the getCached parameter.
     */
    public boolean isRedstoneCoated(boolean getCached) {
        if (getCached) {
            // Update the cached value
            // For the first 3 ticks it will return the actual current value, so things don't look screwy.
            if (this.ticksExisted % 20 == 0 || this.ticksExisted < 3) {
                this.cachedIsRedstoneCoated = this.dataManager.get(IS_REDSTONE_COATED);
            }
            return this.cachedIsRedstoneCoated;
        }

        boolean isRedstoneCoated = this.dataManager.get(IS_REDSTONE_COATED);
        this.cachedIsRedstoneCoated = isRedstoneCoated;
        return isRedstoneCoated;
    }

    /**
     * Update {@link FireflyEntity#IS_REDSTONE_COATED}, and its cached value.
     *
     * @param b The new value to set the DataParameter to.
     */
    private void setRedstoneCovered(boolean b) {
        this.cachedIsRedstoneCoated = b;
        this.dataManager.set(IS_REDSTONE_COATED, b);
    }

    /**
     * @see FireflyEntity#isRedstoneCoated
     */
    public boolean hasIllumerin(boolean getCached) {
        if (getCached) {
            if (this.ticksExisted % 20 == 0) {
                this.cachedHasIllumerin = this.dataManager.get(HAS_ILLUMERIN);
            }
            return this.cachedHasIllumerin;
        }

        boolean hasIllumerin = this.dataManager.get(HAS_ILLUMERIN);
        this.cachedHasIllumerin = hasIllumerin;
        return hasIllumerin;
    }

    /**
     * @see FireflyEntity#setRedstoneCovered
     */
    private void setHasIllumerin(boolean b) {
        this.cachedHasIllumerin = b;
        this.dataManager.set(HAS_ILLUMERIN, b);
    }

    public int getIllumerinDeposited() {
        return this.dataManager.get(ILLUMERIN_DEPOSITED);
    }

    public void setIllumerinDeposited(int i) {
        this.dataManager.set(ILLUMERIN_DEPOSITED, i);
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putBoolean("IsRedstoneCoated", this.isRedstoneCoated(true));
        nbt.putBoolean("HasIllumerin", this.hasIllumerin(true));
        nbt.putInt("IllumerinDeposited", this.getIllumerinDeposited());
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.setRedstoneCovered(nbt.getBoolean("IsRedstoneCoated"));
        this.setHasIllumerin(nbt.getBoolean("HasIllumerin"));
        this.setIllumerinDeposited(nbt.getInt("IllumerinDeposited"));
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals. (0 being highest priority, of course -_-)
        this.goalSelector.addGoal(0, new FireflyEntity.MateGoal(1f));
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.15f, Ingredient.fromItems(Items.HONEY_BOTTLE, Items.HONEY_BLOCK), false));
        this.goalSelector.addGoal(1, new FireflyEntity.EatCompostGoal(1f, 22));
        this.goalSelector.addGoal(2, new FireflyEntity.EntrancedByHoneyGoal());
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.15f));
        this.goalSelector.addGoal(4, new FireflyEntity.WanderGoal());
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
    public double[] getAbdomenParticlePos() {
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
        if (this.world.isRemote) {
            double[] pos = this.getAbdomenParticlePos();
            this.world.addOptionalParticle(this.isRedstoneCoated(true)
                            ? new FireflyParticleData.AbdomenRedstone(this.getEntityId())
                            : this.hasIllumerin(true) ? new FireflyParticleData.AbdomenIllumerin(this.getEntityId())
                            : new FireflyParticleData.Abdomen(this.getEntityId()),
                    true, pos[0], pos[1], pos[2], 0, 0, 0);
        }
    }

    private Vector3d rotateVector(Vector3d vector3d) {
        Vector3d vector3d1 = vector3d.rotatePitch((float) Math.PI / 180F);
        return vector3d1.rotateYaw(-this.prevRenderYawOffset * ((float) Math.PI / 180F));
    }

    private IParticleData getDustParticle() {
        return this.isRedstoneCoated(true) ? new FireflyParticleData.DustRedstone(this.getEntityId()) : new FireflyParticleData.Dust(this.getEntityId());
    }

    /**
     * (Client) Spawn falling particles every so often, at the abdomen's position. Falling angle depends on fireflies speed.
     * This is called every tick.
     */
    private void spawnFallingDustParticles() {
        if (this.ticksExisted % 10 == 0 && this.rand.nextFloat() > 0.25f && this.glowAlpha > 0.25f && !this.isInvisible()) {
            // Redstone fireflies & illumerin fireflies don't spawn particles as often.
            if ((this.isRedstoneCoated(true) || this.hasIllumerin(true)) && (this.rand.nextFloat() > 0.5f))
                return;

            // no clue what this does lol
            Vector3d vector3d = this.rotateVector(new Vector3d(0.0D, -1.0D, 0.0D)).add(this.getPosX(), this.getPosY(), this.getPosZ());
            Vector3d vector3d1 = this.rotateVector(new Vector3d(this.rand.nextFloat(), -1.0D, this.rand.nextFloat() * Math.abs(this.getMotion().getZ()) * 10 + 2));
            Vector3d vector3d2 = vector3d1.scale(-5f + this.rand.nextFloat() * 2.0F);

            // Small random offset around the abdomen, baby fireflies don't have it
            float randPos = this.isChild() ? 0f : MathHelper.nextFloat(this.rand, -0.2f, 0.2f);
            // also have no clue what these numbers mean
            this.world.addParticle(this.getDustParticle(),
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
     * Redstone fireflies activate illumerin lamps in a radius of {@link FireflyEntity#illumerinRadius}, called every half a second.
     */
    private void activateIllumerinLamps() {
        // Baby fireflies have a smaller radius
        if (this.isChild()) illumerinRadius = 3;
        for (double x = this.getPosX() - illumerinRadius; x < this.getPosX() + illumerinRadius; x++) {
            for (double y = this.getPosY() - illumerinRadius; y < this.getPosY() + illumerinRadius; y++) {
                for (double z = this.getPosZ() - illumerinRadius; z < this.getPosZ() + illumerinRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.withinDistance(this.getPosition(), illumerinRadius))
                        continue;

                    BlockState state = this.world.getBlockState(pos);
                    if (state.getBlock() instanceof IllumerinLamp && !state.get(IllumerinLamp.POWERED)) {
                        this.world.setBlockState(pos, state.with(IllumerinLamp.POWERED, Boolean.TRUE), 3);
                        this.illumerinBlocks.add(pos);
                    }
                }
            }
        }
    }

    /**
     * Removes any invalid blocks from {@link FireflyEntity#illumerinBlocks}, called every half a second.
     */
    private void removeInvalidIllumerinLamps() {
        this.illumerinBlocks.removeIf(pos -> {
            BlockState state = this.world.getBlockState(pos);
            // Remove if no longer an illumerin block
            if (!(state.getBlock() instanceof IllumerinLamp)) {
                return true;
            }

            // Remove if out of range
            if (pos.distanceSq(this.getPosition()) > illumerinRadius * illumerinRadius) {
                this.world.setBlockState(pos, state.with(IllumerinLamp.POWERED, Boolean.FALSE), 3);
                return true;
            }

            // Remove if we dead
            if (!this.isAlive()) {
                this.world.setBlockState(pos, state.with(IllumerinLamp.POWERED, Boolean.FALSE), 3);
                return true;
            }

            return false;
        });
    }

    private void doWaterRelatedLogic() {
        // Increase underWaterTicks by 1 if in water
        this.underWaterTicks = this.isInWaterOrBubbleColumn() ? this.underWaterTicks + 1 : 0;
        // Increase rainedOnTicks by 1 if in rain
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

    @Nullable
    private BlockPos getJarPos() {
        for (int i = 1; i < 4; i++) {
            BlockPos pos = this.getPosition().down(i);
            if (this.world.getBlockState(pos).getBlock() instanceof GlassJarBlock) {
                this.currentGlassJar = pos;
                return pos;
            }
        }
        return null;
    }

    private void doJarLogic() {
        if (!this.hasIllumerin(true))
            return;

        BlockPos pos = this.getJarPos();
        if (pos != null && this.ticksExisted % 60 == 0) {
            BlockState state = this.world.getBlockState(pos);
            // Shouldn't need to check for ATTACHED since we're going to be *above* the jar
            if (!(state.getBlock() instanceof GlassJarBlock) || !state.get(GlassJarBlock.OPEN) || state.get(GlassJarBlock.LEVEL) > 0) return;

            int level = state.get(GlassJarBlock.ILLUMERIN_LEVEL);
            if (level >= GlassJarBlock.MAX_ILLUMERIN_LEVEL) return;

            this.world.setBlockState(pos, state.with(GlassJarBlock.ILLUMERIN_LEVEL, level + 1), 3);

            this.setIllumerinDeposited(this.getIllumerinDeposited() + 1);
            if (this.getIllumerinDeposited() >= GlassJarBlock.MAX_ILLUMERIN_LEVEL) {
                this.setHasIllumerin(false);
                this.setIllumerinDeposited(0);
            }
        }
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            // Redstone fireflies & illumerin fireflies are always on, regardless if it's daytime - unlike regular ones.
            if (this.isDayTimeClient() && !this.isRedstoneCoated(true) && !this.hasIllumerin(true)) {
                // Turn off during the day
                this.animationHandler.setAbdomenAnimation(FireflyAbdomenAnimation.OFF);
                this.glowAlpha = 0;
            } else {
                this.animationHandler.updateAbdomenAnimation();
                this.animationHandler.updateGlowAnimation();
                this.spawnFallingDustParticles();

                // Fix abdomen particle not spawning with synced firefly on first glow cycle
                if (this.ticksExisted < 3 && this.glowAlpha > 0.1f && this.abdomenParticle == null) {
                    this.spawnAbdomenParticle();
                }

                // Sometimes particles can disappear to various things, so every 5s we'll try to destroy and add it again
                if (this.ticksExisted % 100 == 0 && this.isRedstoneCoated(true)) {
                    if (this.abdomenParticle != null) {
                        this.abdomenParticle.setExpired();
                    }
                    this.spawnAbdomenParticle();
                }
            }
        } else {
            if (this.isAIDisabled())
                return;

            if (this.isRedstoneCoated(true)) {
                if (this.ticksExisted % 10 == 0) {
                    this.removeInvalidIllumerinLamps();
                    this.activateIllumerinLamps();
                }
            }

            this.doJarLogic();
            this.doWaterRelatedLogic();
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack itemStack) {
        // Fireflies are bred with honey bottles
        return itemStack.getItem().equals(Items.HONEY_BOTTLE);
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
            ClientStuff.beginFireflyFlightSound(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (this.world.isRemote) {
            // Remove ourselves from all the synced lists, if any.
            FireflyGlowSyncHandler.calmSyncedFireflies.syncedFireflies.remove(this);
            FireflyGlowSyncHandler.starryNightSyncedFireflies.syncedFireflies.remove(this);
        } else {
            // Stop giving a signal to nearby illumerin blocks.
            this.removeInvalidIllumerinLamps();
        }
    }

    /**
     * Spawn a bunch of redstone particles around the firefly.
     *
     * @param amount How many particles should be spawned.
     * @param alpha  Alpha of the particle.
     */
    private void spawnRedstoneParticlePuff(boolean lowPowered, int amount) {
        for (int i = 0; i < amount; i++) {
            float randPos = this.isChild() ? 0.22f : 0.66f;
            ((ServerWorld) (this.world)).spawnParticle(new RedstoneParticleData(lowPowered ? 0.5f : 1f, 0, 0, 1f),
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
            this.setRedstoneCovered(true);
            // Reset any illumerin related data
            this.setIllumerinDeposited(0);
            this.setHasIllumerin(false);
            if (!this.world.isRemote) {
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }

                // Spawn powered redstone dust particles
                this.spawnRedstoneParticlePuff(false, 5 + this.rand.nextInt(5));
            } else {
                player.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f);

                // Destroy the current abdomen particle.
                if (this.abdomenParticle != null) this.abdomenParticle.setExpired();
                // Spawn a new abdomen particle.
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
                this.world.addParticle(this.getDustParticle(), this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
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

    private boolean isDarkEnoughToBreed() {
        return this.world.getLight(this.getPosition()) <= 4;
    }

    @Override
    public boolean canFallInLove() {
        return this.isDarkEnoughToBreed() && super.canFallInLove();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vector3d getLeashStartPosition() {
        return new Vector3d(0.0D, 0.5F * this.getEyeHeight(), this.getWidth() * 0.2F);
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
        this.spawnRedstoneParticlePuff(true, 5 + this.rand.nextInt(5));
        this.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f); // TODO temp sound
    }

    //region AI

    private class WanderGoal extends RandomWalkingGoal {
        private final World world;
        private final EntityPredicate fireflyPredicate = new EntityPredicate().setDistance(8f).allowFriendlyFire().allowInvulnerable().setIgnoresLineOfSight().setCustomPredicate(entity -> !entity.isChild());

        private WanderGoal() {
            super(FireflyEntity.this, 1.0f, 1, false);
            this.world = FireflyEntity.this.world;
        }

        @Override
        public boolean shouldExecute() {
            return !FireflyEntity.this.isEntrancedByHoney && FireflyEntity.this.ticksExisted % 20 == 0 && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !FireflyEntity.this.isEntrancedByHoney && super.shouldContinueExecuting();
        }

        @Nullable
        @Override
        protected Vector3d getPosition() {
            Vector3d position;

            // Find some place to go in the air.
            position = RandomPositionGenerator.findAirTarget(FireflyEntity.this, 3, 1,
                    FireflyEntity.this.getLook(0), (float) (Math.PI / 2f), 2, 1);

            // Try again...
            if (position == null) {
                position = RandomPositionGenerator.findRandomTarget(FireflyEntity.this, 3, 1);
            }

            // Ok, we'll just try to go to another firefly then.
            if (!FireflyEntity.this.isChild() && position == null) {
                // Search within 8 block radius.
                FireflyEntity closestFirefly = this.world.getClosestEntityWithinAABB(FireflyEntity.class, fireflyPredicate, FireflyEntity.this,
                        FireflyEntity.this.getPosX(), FireflyEntity.this.getPosY(), FireflyEntity.this.getPosZ(),
                        FireflyEntity.this.getBoundingBox().grow(8f, 2.5f, 8f));

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
                if (position.getY() - FireflyEntity.this.getPosY() > 2f) {
                    position = position.add(0, -(position.getY() - FireflyEntity.this.getPosY() - 1f), 0);
                }

                // Avoid leaves.. They float to the top of trees which is unwanted.
                if (this.world.getBlockState(blockPos.down()).getBlock() instanceof LeavesBlock) {
                    for (int i = 0; i < 3; i++) {
                        if (!(this.world.getBlockState(blockPos.down(i)).getBlock() instanceof LeavesBlock)) {
                            position = position.add(0, -i, 0);
                        }
                    }
                }
            }

            return position;
        }
    }

    private class FlyingMovementHelper extends FlyingMovementController {
        private final World world;
        private boolean isHighUp;
        private BlockPos prevBlockPos;

        private FlyingMovementHelper() {
            super(FireflyEntity.this, 20, true);
            this.world = FireflyEntity.this.world;
            this.prevBlockPos = FireflyEntity.this.getPosition();
        }

        private void moveForward(float multiplier, float y, float speed) {
            FireflyEntity.this.navigator.tryMoveToXYZ(
                    FireflyEntity.this.getPosX() + FireflyEntity.this.getLookVec().getX() * multiplier,
                    FireflyEntity.this.getPosY() + y,
                    FireflyEntity.this.getPosZ() + FireflyEntity.this.getLookVec().getZ() * multiplier, speed);
        }

        @Override
        public void tick() {
            super.tick();

            // Do random accelerations / dashes every so often.
            if (FireflyEntity.this.ticksExisted % 100 == 0 && FireflyEntity.this.rand.nextFloat() > 0.75f && !FireflyEntity.this.isEntrancedByHoney) {
                final float accelAmount = 0.125f;
                FireflyEntity.this.addMotion(
                        MathHelper.clamp(FireflyEntity.this.getLook(0).getX(), -accelAmount, accelAmount),
                        this.world.isAirBlock(FireflyEntity.this.getPosition().down()) ? 0f : 0.075f,
                        MathHelper.clamp(FireflyEntity.this.getLook(0).getZ(), -accelAmount, accelAmount));
            }

            // Don't vertically too quickly.
            if (FireflyEntity.this.getMotion().getY() < -0.04f) {
                FireflyEntity.this.addMotion(0, 0.025f, 0);
            } else if (FireflyEntity.this.getMotion().getY() > 0.04f) {
                FireflyEntity.this.addMotion(0, -0.025f, 0);
            }

            if (FireflyEntity.this.ticksExisted % 20 == 0 && !FireflyEntity.this.isEntrancedByHoney) {
                // Stay off the ground
                if (this.world.getBlockState(FireflyEntity.this.getPosition().down()).isSolid() || FireflyEntity.this.isOnGround()) {
                    this.moveForward(1.2f, 1.5f, 0.85f);
                }

                // And also don't stay too high in the air
                for (int i = 1; i < 4; i++) {
                    isHighUp = !this.world.getBlockState(FireflyEntity.this.getPosition().down(i)).isSolid();
                    if (!isHighUp) break;
                }
                if (isHighUp) {
                    this.moveForward(1.2f, -2f, 0.85f);
                }

                // Try to not stay idle for more than a second.
                if (this.prevBlockPos == FireflyEntity.this.getPosition()) {
                    this.moveForward(MathHelper.nextFloat(FireflyEntity.this.rand, -3f, 3f), FireflyEntity.this.rand.nextFloat(), 1f);
                }
                this.prevBlockPos = FireflyEntity.this.getPosition();
            }
        }
    }

    private class EntrancedByHoneyGoal extends MoveToBlockGoal {
        private final World world;

        private EntrancedByHoneyGoal() {
            super(FireflyEntity.this, 1.15, 8);
            this.world = FireflyEntity.this.world;
        }

        private Vector3d getDestinationBlockCentered() {
            return new Vector3d(this.destinationBlock.getX() + 0.5f, this.destinationBlock.getY() + 0.5f, this.destinationBlock.getZ() + 0.5f);
        }

        private boolean isHoneyBlockVisible(BlockPos destinationBlock) {
            BlockRayTraceResult rayTraceResult = this.world.rayTraceBlocks(new RayTraceContext(
                    FireflyEntity.this.getPositionVec().add(0, FireflyEntity.this.getEyeHeight(), 0),
                    new Vector3d(destinationBlock.getX() + 0.5f, destinationBlock.getY() + 0.5f, destinationBlock.getZ() + 0.5f),
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, FireflyEntity.this));

            return FireflyEntity.this.world.getBlockState(rayTraceResult.getPos()).getBlock() instanceof HoneyBlock;
        }

        @Override
        protected int getRunDelay(CreatureEntity creatureIn) {
            return 20 + FireflyEntity.this.rand.nextInt(20);
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            FireflyEntity.this.isEntrancedByHoney = true;
        }

        @Override
        public void resetTask() {
            super.resetTask();
            FireflyEntity.this.isEntrancedByHoney = false;
        }

        @Override
        public void tick() {
            super.tick();
            Vector3d destinationBlockCentered = this.getDestinationBlockCentered();
            // Stare at the honey block.
            FireflyEntity.this.getLookController().setLookPosition(destinationBlockCentered);

            // Keep close to the honey block.
            if (FireflyEntity.this.ticksExisted % 20 == 0 && FireflyEntity.this.getDistanceSq(destinationBlockCentered) > 3f) {
                this.attemptToMove();
            }
        }

        @Override
        protected boolean shouldMoveTo(IWorldReader worldReader, BlockPos blockPos) {
            if (this.world.getBlockState(blockPos).getBlock() instanceof HoneyBlock && this.isHoneyBlockVisible(blockPos)) {
                FireflyEntity.this.isEntrancedByHoney = true;
                return true;
            }
            return false;
        }

        @Override
        protected void attemptToMove() {
            Vector3d destinationBlockCentered = this.getDestinationBlockCentered();
            FireflyEntity.this.getNavigator().tryMoveToXYZ(
                    destinationBlockCentered.getX(), destinationBlockCentered.getY(), destinationBlockCentered.getZ(), this.movementSpeed);
        }
    }

    private class MateGoal extends BreedGoal {
        public MateGoal(double speedIn) {
            super(FireflyEntity.this, speedIn);
        }

        @Override
        public boolean shouldExecute() {
            return FireflyEntity.this.isDarkEnoughToBreed() && super.shouldExecute();
        }
    }

    private class EatCompostGoal extends MoveToBlockGoal {
        private final World world;
        private int startEatingTicks;

        public EatCompostGoal(double speedIn, int length) {
            super(FireflyEntity.this, speedIn, length);
            this.world = FireflyEntity.this.world;
        }

        @Override
        public boolean shouldMove() {
            return this.timeoutCounter % 100 == 0;
        }

        @Override
        protected boolean shouldMoveTo(IWorldReader worldIn, BlockPos pos) {
            BlockState state = worldIn.getBlockState(pos);
            return state.matchesBlock(Blocks.COMPOSTER) && state.get(ComposterBlock.LEVEL) > 0;
        }

        private void lookAtCompost() {
            FireflyEntity.this.getLookController().setLookPosition(this.destinationBlock.getX() + 0.5f, this.destinationBlock.getY() + 0.5f, this.destinationBlock.getZ() + 0.5f);
        }

        @Override
        public void tick() {
            if (this.destinationBlock.withinDistance(FireflyEntity.this.getPosition(), 3f)) {
                this.lookAtCompost();
                if (this.startEatingTicks >= 60) {
                    this.eatCompost();
                } else {
                    ++this.startEatingTicks;
                }
            }

            super.tick();
        }

        private void eatCompost() {
            if (!this.world.getGameRules().getBoolean(GameRules.MOB_GRIEFING))
                return;

            BlockState state = this.world.getBlockState(this.destinationBlock);
            if (state.matchesBlock(Blocks.COMPOSTER)) {
                FireflyEntity.this.playSound(SoundEvents.BLOCK_COMPOSTER_EMPTY, 1.0F, 1.0F);
                int i = state.get(ComposterBlock.LEVEL);
                this.world.setBlockState(this.destinationBlock, state.with(ComposterBlock.LEVEL, i - (i == 8 ? 2 : 1)), 3);
                FireflyEntity.this.setHasIllumerin(true);
            }
        }

        @Override
        public boolean shouldExecute() {
            return !FireflyEntity.this.isRedstoneCoated(true) && !FireflyEntity.this.hasIllumerin(true) && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !FireflyEntity.this.isRedstoneCoated(true) && !FireflyEntity.this.hasIllumerin(true) && super.shouldContinueExecuting();
        }

        @Override
        public void startExecuting() {
            this.startEatingTicks = 0;
            this.lookAtCompost();
            super.startExecuting();
        }
    }

    //endregion AI
}
