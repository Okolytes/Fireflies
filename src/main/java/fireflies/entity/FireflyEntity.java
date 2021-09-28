package fireflies.entity;

import fireflies.Registry;
import fireflies.block.GlassJarBlock;
import fireflies.client.ClientStuff;
import fireflies.client.entity.FireflyAbdomenAnimationManager;
import fireflies.client.entity.FireflyParticleManager;
import fireflies.client.sound.FireflyFlightSound;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ITag;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    public static final DataParameter<Boolean> IS_REDSTONE_COATED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> HAS_ILLUMERIN = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ILLUMERIN_DEPOSITED = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.VARINT);

    public final FireflyAbdomenAnimationManager animationManager = new FireflyAbdomenAnimationManager(this);
    public final FireflyParticleManager particleManager = new FireflyParticleManager(this);
    public final RedstoneFireflyManager redstoneManager = new RedstoneFireflyManager(this);
    /**
     * Is this firefly's current goal to pathfind towards honey?
     */
    public boolean isEntrancedByHoney;
    /**
     * How many ticks this firefly has been underwater for
     */
    public int underWaterTicks;
    /**
     * How many ticks this firefly has been rained on for
     */
    public int rainedOnTicks;
    /**
     * The position of the current glass jar that the firefly is filling, if any.
     */
    @Nullable
    private BlockPos currentGlassJar;
    /**
     * Used for checking if the current firefly has illumerin on itself, this is updated every 20 ticks.
     */
    private boolean cachedHasIllumerin;

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        this.moveController = new FireflyAI.FlyingMovementHelper(this);
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
        super.registerData();
        this.dataManager.register(IS_REDSTONE_COATED, false);
        this.dataManager.register(HAS_ILLUMERIN, false);
        this.dataManager.register(ILLUMERIN_DEPOSITED, 0);
    }

    public boolean hasIllumerin(boolean getCached) {
        if (getCached) {
            // This method is called every frame in our abdomen layer renderer, so this statement *will* pass
            if (this.ticksExisted % 20 == 0) {
                this.cachedHasIllumerin = this.dataManager.get(HAS_ILLUMERIN);
            }
            return this.cachedHasIllumerin;
        }

        final boolean hasIllumerin = this.dataManager.get(HAS_ILLUMERIN);
        this.cachedHasIllumerin = hasIllumerin;
        return hasIllumerin;
    }

    public void setHasIllumerin(boolean b) {
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
        final boolean isRedstoneCoated = this.redstoneManager.isRedstoneCoated(false);
        nbt.putBoolean("IsRedstoneCoated", isRedstoneCoated);
        nbt.putBoolean("HasIllumerin", this.hasIllumerin(true));
        nbt.putInt("IllumerinDeposited", this.getIllumerinDeposited());
        if (isRedstoneCoated) {
            final ListNBT listNBT = new ListNBT();
            this.redstoneManager.syncedLamps.forEach(pos -> listNBT.add(NBTUtil.writeBlockPos(pos)));
            nbt.put("IllumerinLamps", listNBT);
        }
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.redstoneManager.setRedstoneCoated(nbt.getBoolean("IsRedstoneCoated"));
        this.setHasIllumerin(nbt.getBoolean("HasIllumerin"));
        this.setIllumerinDeposited(nbt.getInt("IllumerinDeposited"));
        if (this.redstoneManager.isRedstoneCoated(false)) {
            final ListNBT listNBT = nbt.getList("IllumerinLamps", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < listNBT.size(); i++) {
                this.redstoneManager.syncedLamps.add(NBTUtil.readBlockPos(listNBT.getCompound(i)));
            }
        }
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals. (0 being the highest priority, of course -_-)
        this.goalSelector.addGoal(0, new FireflyAI.MateGoal(this, 1f));
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.15f, Ingredient.fromItems(Items.HONEY_BOTTLE, Items.HONEY_BLOCK), false));
        this.goalSelector.addGoal(2, new FireflyAI.EatCompostGoal(this, 1f, 22));
        this.goalSelector.addGoal(3, new FireflyAI.EntrancedByHoneyGoal(this));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.15f));
        this.goalSelector.addGoal(5, new FireflyAI.WanderGoal(this));
        this.goalSelector.addGoal(6, new SwimGoal(this));
    }

    @Override
    protected PathNavigator createNavigator(World world) {
        final FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, world) {
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
        if (!this.isSilent()) {
            //this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), FirefliesRegistration.FIREFLY_GLOW.get(), SoundCategory.NEUTRAL, 0.33f, 1, false);
        }
    }

    private void doWaterAndRainLogic() {
        this.underWaterTicks = this.isInWaterOrBubbleColumn() ? this.underWaterTicks + 1 : 0;
        this.rainedOnTicks = this.world.isRainingAt(this.getPosition()) ? this.rainedOnTicks + 1 : 0;

        if (this.underWaterTicks > 20) {
            this.attackEntityFrom(DamageSource.DROWN, 1.0F);
        } else if (this.underWaterTicks > 10) {
            if (this.redstoneManager.isRedstoneCoated(true)) {
                this.redstoneManager.removeRedstoneCoated();
            }
        }

        if (this.rainedOnTicks > 10 && this.redstoneManager.isRedstoneCoated(true)) {
            this.redstoneManager.removeRedstoneCoated();
            this.rainedOnTicks = 0;
        }
    }

    @Nullable
    private BlockPos getJarPos() {
        for (int i = 1; i < 4; i++) {
            final BlockPos pos = this.getPosition().down(i);
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

        final BlockPos pos = this.getJarPos();
        if (pos != null && this.ticksExisted % 60 == 0) {
            final BlockState state = this.world.getBlockState(pos);
            // Shouldn't need to check for ATTACHED since we're going to be *above* the jar
            if (!(state.getBlock() instanceof GlassJarBlock) || !state.get(GlassJarBlock.OPEN) || state.get(GlassJarBlock.LEVEL) > 0)
                return;

            final int level = state.get(GlassJarBlock.ILLUMERIN_LEVEL);
            if (level >= GlassJarBlock.MAX_ILLUMERIN_LEVEL)
                return;

            this.world.setBlockState(pos, state.with(GlassJarBlock.ILLUMERIN_LEVEL, level + 1), 3);

            this.setIllumerinDeposited(this.getIllumerinDeposited() + 1);
            if (this.getIllumerinDeposited() >= GlassJarBlock.MAX_ILLUMERIN_LEVEL) {
                this.setHasIllumerin(false);
                this.setIllumerinDeposited(0);
            }
        }
    }

    /**
     * @return Should this firefly have its abdomen particle, spawning dust particles, glow animation etc
     */
    public boolean shouldDoEffects() {
        // Redstone Fireflies & Illumerin Fireflies are not affected by if it's daytime or not
        return this.redstoneManager.isRedstoneCoated(true) || this.hasIllumerin(true) || !ClientStuff.isDayTimeClient(this.world);
    }

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            if (this.shouldDoEffects()) {
                this.animationManager.updateAbdomenAnimation();
                // If our AbdomenAnimator is using a synced instance we will let it do the animating
                if (!FireflyAbdomenAnimationManager.SYNCHRONIZED_ANIMATORS.containsValue(this.animationManager.animator)) {
                    this.animationManager.animator.animate();
                }
                this.particleManager.spawnFallingDustParticles();
            } else {
                this.animationManager.setAnimation(null);
            }
        } else {
            if (!this.isAIDisabled()) {
                this.redstoneManager.tick();
                this.doJarLogic();
                this.doWaterAndRainLogic();
            }
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.world.isRemote) {
            if (!this.isSilent()) {
                FireflyFlightSound.beginFireflyFlightSound(this);
            }
            this.particleManager.spawnAbdomenParticle();
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (this.world.isRemote) {
            this.animationManager.stopAnimating();
        } else {
            if (this.redstoneManager.isRedstoneCoated(false)) {
                this.redstoneManager.desyncLamps();
            }
        }
    }

    @Override
    public ActionResultType getEntityInteractionResult(PlayerEntity player, Hand hand) {
        final ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.getItem() == Items.REDSTONE && !this.redstoneManager.isRedstoneCoated(false)) {
            this.redstoneManager.convertRedstoneCoated(player, heldItem);
            return ActionResultType.SUCCESS;
        } else if (!this.world.isRemote && hand == Hand.MAIN_HAND && this.redstoneManager.isRedstoneCoated(false) && this.redstoneManager.searchTime <= 0
                && !(heldItem.getItem() == Items.HONEY_BOTTLE && !this.isInLove())) {
            this.redstoneManager.startSearchForLamps();
            return ActionResultType.SUCCESS;
        }

        return super.getEntityInteractionResult(player, hand);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Puff out some particles on hit, the amount depending on damage.
        if (this.world.isRemote) {
            for (int i = 0; i < (int) MathHelper.clamp(amount, 2, 5); i++) {
                this.world.addParticle(this.particleManager.getDustParticle(), this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
            }
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean isBreedingItem(ItemStack itemStack) {
        return itemStack.getItem().equals(Items.HONEY_BOTTLE);
    }

    @Override
    protected void consumeItemFromStack(PlayerEntity player, ItemStack honeyBottle) {
        /* Fix the honey bottle being consumed in its entirety */
        if (!player.abilities.isCreativeMode) {
            // Consume the honey bottle
            honeyBottle.shrink(1);
            // Give back a glass bottle
            final ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.inventory.addItemStackToInventory(glassBottle)) {
                player.dropItem(glassBottle, false);
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (this.world.isRemote) {
            this.particleManager.destroyAbdomenParticle();
        }
        super.onDeath(cause);
    }

    @Nullable
    @Override
    public Entity changeDimension(ServerWorld server) {
        if (this.redstoneManager.isRedstoneCoated(false)) {
            this.redstoneManager.desyncLamps();
            this.redstoneManager.syncedLamps.clear();
            this.redstoneManager.poweredLamps.clear();
        }
        return super.changeDimension(server);
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

    /**
     * Adds motion to our current motion (removes if negative value).
     */
    public void addMotion(double x, double y, double z) {
        this.setMotion(this.getMotion().add(x, y, z));
    }

    @Override
    protected void handleFluidJump(ITag<Fluid> fluidTag) {
        this.addMotion(0f, 0.01f, 0f);
    }

    public boolean isDarkEnoughToBreed() {
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
        final double d0 = 128 * getRenderDistanceWeight();
        return distance < d0 * d0;
    }
}
