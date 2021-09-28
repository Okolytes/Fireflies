package fireflies.entity;

import net.minecraft.block.*;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class FireflyAI {

    /* Horrible code ahead! */

    public static class WanderGoal extends RandomWalkingGoal {
        private final FireflyEntity firefly;
        private final World world;
        private final EntityPredicate fireflyPredicate = new EntityPredicate().setDistance(8f).allowFriendlyFire().allowInvulnerable().setIgnoresLineOfSight().setCustomPredicate(entity -> !entity.isChild());

        public WanderGoal(FireflyEntity firefly) {
            super(firefly, 1.0f, 1, false);
            this.firefly = firefly;
            this.world = this.firefly.world;
        }

        @Override
        public boolean shouldExecute() {
            return !this.firefly.isEntrancedByHoney && this.firefly.ticksExisted % 20 == 0 && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.firefly.isEntrancedByHoney && super.shouldContinueExecuting();
        }

        @Nullable
        @Override
        protected Vector3d getPosition() {
            Vector3d position;

            // Find some place to go in the air.
            position = RandomPositionGenerator.findAirTarget(this.firefly, 3, 1,
                    this.firefly.getLook(0), (float) (Math.PI / 2f), 2, 1);

            // Try again...
            if (position == null) {
                position = RandomPositionGenerator.findRandomTarget(this.firefly, 3, 1);
            }

            // Ok, we'll just try to go to another firefly then.
            if (position == null && !this.firefly.isChild()) {
                // Search within 8 block radius.
                final FireflyEntity closestFirefly = this.world.getClosestEntityWithinAABB(FireflyEntity.class, this.fireflyPredicate, this.firefly,
                        this.firefly.getPosX(), this.firefly.getPosY(), this.firefly.getPosZ(),
                        this.firefly.getBoundingBox().grow(8f, 2.5f, 8f));

                if (closestFirefly != null) {
                    position = closestFirefly.getPositionVec();
                }
            }

            if (position != null) {
                final BlockPos blockPos = new BlockPos(position);
                final BlockState down = this.world.getBlockState(blockPos.down());

                // Don't land on the ground if it's avoidable.
                if (down.isSolid() && this.world.isAirBlock(blockPos.up())) {
                    position = position.add(0, 1.5f, 0);
                }

                // Don't go too high.
                if (position.getY() - this.firefly.getPosY() > 2f) {
                    position = position.add(0, -(position.getY() - this.firefly.getPosY() - 1f), 0);
                }

                // Avoid leaves... They tend to float to the top of trees
                if (down.getBlock() instanceof LeavesBlock) {
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

    public static class FlyingMovementHelper extends FlyingMovementController {
        private final FireflyEntity firefly;
        private final World world;
        private boolean isHighUp;
        private BlockPos prevBlockPos;

        public FlyingMovementHelper(FireflyEntity firefly) {
            super(firefly, 20, true);
            this.firefly = firefly;
            this.world = this.firefly.world;
            this.prevBlockPos = this.firefly.getPosition();
        }

        private void moveForward(float multiplier, float y, float speed) {
            this.firefly.getNavigator().tryMoveToXYZ(
                    this.firefly.getPosX() + this.firefly.getLookVec().getX() * multiplier,
                    this.firefly.getPosY() + y,
                    this.firefly.getPosZ() + this.firefly.getLookVec().getZ() * multiplier, speed);
        }

        @Override
        public void tick() {
            super.tick();

            // Do random accelerations / dashes every so often.
            if (this.firefly.ticksExisted % 100 == 0 && this.firefly.getRNG().nextFloat() > 0.75f && !this.firefly.isEntrancedByHoney) {
                final float accelAmount = 0.125f;
                this.firefly.addMotion(
                        MathHelper.clamp(this.firefly.getLook(0).getX(), -accelAmount, accelAmount),
                        this.world.isAirBlock(this.firefly.getPosition().down()) ? 0f : 0.075f,
                        MathHelper.clamp(this.firefly.getLook(0).getZ(), -accelAmount, accelAmount));
            }

            // Don't vertically too quickly.
            if (this.firefly.getMotion().getY() < -0.04f) {
                this.firefly.addMotion(0, 0.025f, 0);
            } else if (this.firefly.getMotion().getY() > 0.04f) {
                this.firefly.addMotion(0, -0.025f, 0);
            }

            if (this.firefly.ticksExisted % 20 == 0 && !this.firefly.isEntrancedByHoney) {
                // Stay off the ground
                if (this.world.getBlockState(this.firefly.getPosition().down()).isSolid() || this.firefly.isOnGround()) {
                    this.moveForward(1.2f, 1.5f, 0.85f);
                }

                // And also don't stay too high in the air
                for (int i = 1; i < 4; i++) {
                    this.isHighUp = !this.world.getBlockState(this.firefly.getPosition().down(i)).isSolid();
                    if (!this.isHighUp)
                        break;
                }
                if (this.isHighUp) {
                    this.moveForward(1.2f, -2f, 0.85f);
                }

                // Try to not stay idle for more than a second.
                if (this.prevBlockPos == this.firefly.getPosition()) {
                    this.moveForward(MathHelper.nextFloat(this.firefly.getRNG(), -3f, 3f), this.firefly.getRNG().nextFloat(), 1f);
                }
                this.prevBlockPos = this.firefly.getPosition();
            }
        }
    }

    public static class EntrancedByHoneyGoal extends MoveToBlockGoal {
        private final FireflyEntity firefly;
        private final World world;

        public EntrancedByHoneyGoal(FireflyEntity firefly) {
            super(firefly, 1.15, 8);
            this.firefly = firefly;
            this.world = this.firefly.world;
        }

        private boolean isHoneyBlockVisible(BlockPos destinationBlock) {
            final BlockRayTraceResult rayTraceResult = this.world.rayTraceBlocks(new RayTraceContext(
                    this.firefly.getPositionVec().add(0, this.firefly.getEyeHeight(), 0), Vector3d.copyCentered(destinationBlock),
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this.firefly));

            return this.firefly.world.getBlockState(rayTraceResult.getPos()).getBlock() instanceof HoneyBlock;
        }

        @Override
        protected int getRunDelay(CreatureEntity creatureIn) {
            return 20 + this.firefly.getRNG().nextInt(20);
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            this.firefly.isEntrancedByHoney = true;
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.firefly.isEntrancedByHoney = false;
        }

        @Override
        public void tick() {
            super.tick();
            final Vector3d destinationBlockCentered = Vector3d.copyCentered(this.destinationBlock);
            // Stare at the honey block.
            this.firefly.getLookController().setLookPosition(destinationBlockCentered);

            // Keep close to the honey block.
            if (this.firefly.ticksExisted % 20 == 0 && this.firefly.getDistanceSq(destinationBlockCentered) > 1.5f) {
                this.attemptToMove();
            }
        }

        @Override
        protected boolean shouldMoveTo(IWorldReader worldReader, BlockPos blockPos) {
            if (this.world.getBlockState(blockPos).getBlock() instanceof HoneyBlock && this.isHoneyBlockVisible(blockPos)) {
                this.firefly.isEntrancedByHoney = true;
                return true;
            }
            return false;
        }

        @Override
        protected void attemptToMove() {
            final Vector3d destinationBlockCentered = Vector3d.copyCentered(this.destinationBlock);
            this.firefly.getNavigator().tryMoveToXYZ(destinationBlockCentered.getX(), destinationBlockCentered.getY(), destinationBlockCentered.getZ(), this.movementSpeed);
        }
    }

    public static class MateGoal extends BreedGoal {
        private final FireflyEntity firefly;

        public MateGoal(FireflyEntity firefly, double speedIn) {
            super(firefly, speedIn);
            this.firefly = firefly;
        }

        @Override
        public boolean shouldExecute() {
            return this.firefly.isDarkEnoughToBreed() && super.shouldExecute();
        }
    }

    public static class EatCompostGoal extends MoveToBlockGoal {
        private final FireflyEntity firefly;
        private final World world;
        private int startEatingTicks;

        public EatCompostGoal(FireflyEntity firefly, double speedIn, int length) {
            super(firefly, speedIn, length);
            this.firefly = firefly;
            this.world = this.firefly.world;
        }

        @Override
        public boolean shouldMove() {
            return this.timeoutCounter % 100 == 0;
        }

        @Override
        protected boolean shouldMoveTo(IWorldReader worldIn, BlockPos pos) {
            final BlockState state = worldIn.getBlockState(pos);
            return state.matchesBlock(Blocks.COMPOSTER) && state.get(ComposterBlock.LEVEL) > 0;
        }

        private void lookAtCompost() {
            this.firefly.getLookController().setLookPosition(Vector3d.copyCentered(this.destinationBlock));
        }

        @Override
        public void tick() {
            if (this.destinationBlock.withinDistance(this.firefly.getPosition(), 3f)) {
                this.lookAtCompost();
                this.startEatingTicks++;
                if (this.startEatingTicks >= 40) {
                    this.eatCompost();
                }
            }

            super.tick();
        }

        private void eatCompost() {
            if (!this.world.getGameRules().getBoolean(GameRules.MOB_GRIEFING))
                return;

            final BlockState state = this.world.getBlockState(this.destinationBlock);
            if (state.matchesBlock(Blocks.COMPOSTER)) {
                this.lookAtCompost();
                this.firefly.playSound(SoundEvents.BLOCK_COMPOSTER_EMPTY, 1.0F, 1.0F);
                final int i = state.get(ComposterBlock.LEVEL);
                this.world.setBlockState(this.destinationBlock, state.with(ComposterBlock.LEVEL, i - (i == 8 ? 2 : 1)), 3);
                this.firefly.setHasIllumerin(true);
            }
        }

        @Override
        public boolean shouldExecute() {
            return !this.firefly.redstoneManager.isRedstoneCoated(true) && !this.firefly.hasIllumerin(true) && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.firefly.redstoneManager.isRedstoneCoated(true) && !this.firefly.hasIllumerin(true) && super.shouldContinueExecuting();
        }

        @Override
        public void startExecuting() {
            this.startEatingTicks = 0;
            this.lookAtCompost();
            super.startExecuting();
        }
    }

}
