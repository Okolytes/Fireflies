package fireflies.entity;

import fireflies.Registry;
import fireflies.client.AbdomenAnimationManager;
import fireflies.client.ClientStuff;
import fireflies.client.particle.FireflyParticleManager;
import fireflies.client.sound.FireflyFlightSound;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.PanicGoal;
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
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FireflyEntity extends AnimalEntity implements IFlyingAnimal {
    private static final DataParameter<Boolean> HAS_ILLUMERIN = EntityDataManager.createKey(FireflyEntity.class, DataSerializers.BOOLEAN);
    // client only
    public final AbdomenAnimationManager abdomenAnimationManager;
    // client only
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

    public FireflyEntity(EntityType<? extends FireflyEntity> entityType, World world) {
        super(entityType, world);
        if (world.isRemote) {
            abdomenAnimationManager = new AbdomenAnimationManager(this);
            particleManager = new FireflyParticleManager(this);
        } else {
            abdomenAnimationManager = null;
            particleManager = null;
        }
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
        this.dataManager.register(HAS_ILLUMERIN, false);
    }

    public boolean hasIllumerin() {
        // This method is called every frame in our abdomen layer renderer, so this statement *will* pass
        if (this.ticksExisted % 10 == 0) {
            this.cachedHasIllumerin = this.dataManager.get(HAS_ILLUMERIN);
        }
        return this.cachedHasIllumerin;
    }

    public void setHasIllumerin(boolean b) {
        if (b) {
            this.setRandomIllumerinDropTime();
        }
        this.cachedHasIllumerin = b;
        this.dataManager.set(HAS_ILLUMERIN, b);
    }


    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putBoolean("HasIllumerin", this.hasIllumerin());
        nbt.putInt("IllumerinDropTime", this.timeUntilIllumerinDrop);
        nbt.putInt("EatCompostCooldown", this.timeUntilCanEatCompostAgain);
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.setHasIllumerin(nbt.getBoolean("HasIllumerin"));
        this.timeUntilIllumerinDrop = nbt.getInt("IllumerinDropTime");
        this.timeUntilCanEatCompostAgain = nbt.getInt("EatCompostCooldown");
    }

    @Override
    protected void registerGoals() {
        // Register all of our fireflies AI goals. (0 being the highest priority, of course -_-)
        this.goalSelector.addGoal(0, new PanicGoal(this, 2.5f));
        this.goalSelector.addGoal(1, new FireflyAI.MateGoal(this, 1f));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.15f, Ingredient.fromItems(Items.HONEY_BOTTLE), false));
        this.goalSelector.addGoal(3, new FireflyAI.EatCompostGoal(this, 1f, 22));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.15f));
        this.goalSelector.addGoal(6, new FireflyAI.WanderGoal(this));
        this.goalSelector.addGoal(7, new SwimGoal(this));
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

    @Override
    public void livingTick() {
        super.livingTick();

        if (this.world.isRemote) {
            if (!ClientStuff.isDayTime(this.world)) {
                this.particleManager.trySpawnDustParticles();
            } else {
                this.abdomenAnimationManager.setAnimation(null);
            }
        } else {
            if (!this.isAIDisabled()) {
                this.underWaterTicks = this.isInWaterOrBubbleColumn() ? this.underWaterTicks + 1 : 0;
                this.rainedOnTicks = this.world.isRainingAt(this.getPosition()) ? this.rainedOnTicks + 1 : 0;

                if (this.underWaterTicks > 20) {
                    this.attackEntityFrom(DamageSource.DROWN, 1.0F);
                }

                if (this.timeUntilCanEatCompostAgain > 0) {
                    this.timeUntilCanEatCompostAgain--;
                }

                if (this.hasIllumerin()) {
                    if (this.timeUntilIllumerinDrop-- <= 0) {
                        this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, MathHelper.nextFloat(this.rand, 0.5f, 1.5f));
                        this.entityDropItem(Registry.ILLUMERIN.get());
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
        if (this.world.isRemote) {
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
        if (this.world.isRemote) {
            this.abdomenAnimationManager.setAnimation(null);
        }
        super.onRemovedFromWorld();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.world.isRemote && amount > 0) { // todo for some reason it runs twice, second time the source is 'generic' and amount is 0
            final int particleCount = (int) MathHelper.clamp(amount, 2, 8);
            for (int i = 0; i < particleCount; i++) {
                this.world.addParticle(this.particleManager.getDustParticle(), this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
            }
            this.abdomenAnimationManager.setAnimation("hurt");
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

    private void setRandomIllumerinDropTime() {
        this.timeUntilIllumerinDrop = MathHelper.nextInt(this.rand, 3600, 7200); // 3600, 7200
    }
}
