package fireflies.entity;

import fireflies.Fireflies;
import fireflies.Registry;
import fireflies.block.IllumerinLamp;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.*;

public class RedstoneFireflyManager {
    /**
     * A set of illumerin lamp positions this redstone firefly is synced to.
     */
    public final HashSet<BlockPos> syncedLamps;
    /**
     * A set of illumerin lamps this redstone firefly is powering.
     */
    public final HashSet<BlockPos> poweredLamps;
    private final FireflyEntity firefly;
    private final World world;
    private final Random random;
    public int searchTime;
    private int flashTime;
    private boolean cachedIsRedstoneCoated;

    public RedstoneFireflyManager(FireflyEntity fireflyEntity) {
        this.firefly = fireflyEntity;
        this.world = fireflyEntity.world;
        this.random = fireflyEntity.getRNG();
        this.syncedLamps = new HashSet<>(this.getMaxSyncedLamps());
        this.poweredLamps = new HashSet<>(this.getMaxPoweredLamps());
    }

    private void powerLamps() {
        // Sort our synced lamps by distance
        final ArrayList<LampPosition> lamps = new ArrayList<>(this.getMaxSyncedLamps());
        this.syncedLamps.forEach(blockPos -> lamps.add(new LampPosition(blockPos)));
        lamps.sort(Comparator.comparingDouble(value -> value.distSq));

        for (int i = 0; i < lamps.size(); i++) {
            final BlockPos pos = lamps.get(i).pos;
            final BlockState state = this.world.getBlockState(pos);
            final boolean powered = state.get(IllumerinLamp.POWERED);
            // Unpower the lamps out of range or once we've reached the last possible one we can
            if (i >= this.getMaxPoweredLamps() || lamps.get(i).distSq > (this.getMaxPoweredLampDist() * this.getMaxPoweredLampDist())) {
                if (this.poweredLamps.contains(pos) && powered) {
                    this.poweredLamps.remove(pos);
                    this.world.setBlockState(pos, state.with(IllumerinLamp.POWERED, false), 3);
                }
            } else if (!this.poweredLamps.contains(pos) && !powered) {
                this.poweredLamps.add(pos);
                this.world.setBlockState(pos, state.with(IllumerinLamp.POWERED, true), 3);
            }
        }
    }

    public void desyncLamps() {
        this.removeInvalidLamps();
        for (BlockPos pos : this.syncedLamps) {
            final BlockState state = this.world.getBlockState(pos);
            this.world.setBlockState(pos, state.with(IllumerinLamp.SYNCED, false).with(IllumerinLamp.POWERED, false), 3);
        }
    }

    private void removeInvalidLamps() {
        // Desync any lamps that have been moved / destroyed, or just when we are too far away - (don't want to keep chunks loaded)
        for (final Iterator<BlockPos> iterator = this.syncedLamps.iterator(); iterator.hasNext(); ) {
            final BlockPos pos = iterator.next();
            final BlockState state = this.world.getBlockState(pos);
            final boolean isStillLamp = state.getBlock() instanceof IllumerinLamp;
            if (isStillLamp) {
                if (!pos.withinDistance(this.firefly.getPositionVec(), this.getMaxSyncDistance())) {
                    this.world.setBlockState(pos, state.with(IllumerinLamp.SYNCED, false).with(IllumerinLamp.POWERED, false), 3);
                    this.poweredLamps.remove(pos);
                    iterator.remove();
                }
            } else {
                this.poweredLamps.remove(pos);
                iterator.remove();
            }
        }
    }

    public void tick() {
        if (!this.firefly.redstoneManager.isRedstoneCoated(true))
            return;

        this.removeInvalidLamps();

        if (this.searchTime > 0) {
            this.searchTime--;
            if (this.searchTime <= 0) {
                this.searchForLamps();
            }
        }

        if (this.flashTime > 0) {
            this.flashTime--;
            if (this.flashTime % 10 == 0) {
                for (BlockPos pos : this.syncedLamps) {
                    final BlockState state = this.world.getBlockState(pos);
                    this.world.setBlockState(pos, state.cycleValue(IllumerinLamp.POWERED), 2 | 16 | 32);
                }
            }
        }

        if (this.firefly.ticksExisted % 20 == 0 && this.syncedLamps.size() > 0 && this.flashTime <= 0) {
            this.powerLamps();
        }
    }

    public void startSearchForLamps() {
        // Between 3 and 5 seconds
        this.searchTime = MathHelper.nextInt(this.random, 60, 100);
    }

    private void searchForLamps() {
        this.desyncLamps();
        this.syncedLamps.clear();
        this.poweredLamps.clear();
        this.flashTime = 40; // 2 flashes

        final long time = System.currentTimeMillis();
        final int range = this.getMaxLampSearchRange();
        final ArrayList<LampPosition> lamps = new ArrayList<>(range * range * range);

        // Find positions of illumerin lamps
        BlockPos.getAllInBox(this.firefly.getBoundingBox().grow(range)).forEach(pos -> {
            if (pos.withinDistance(this.firefly.getPositionVec(), range)) { // getAllInBox quite literally does return, everything in a box.
                final BlockState state = this.world.getBlockState(pos);
                if (state.getBlock() instanceof IllumerinLamp && !state.get(IllumerinLamp.SYNCED)) {
                    lamps.add(new LampPosition(pos.toImmutable()));
                }
            }
        });

        if (lamps.size() > 0) {
            // Sort by closest distance and only get the first n elements, then add them to the syncedLamps set.
            lamps.sort(Comparator.comparingDouble(lampPosition -> lampPosition.distSq));
            lamps.stream().limit(this.getMaxSyncedLamps()).forEach(lampPosition -> {
                final BlockPos pos = lampPosition.pos;
                this.world.setBlockState(pos, this.world.getBlockState(pos).with(IllumerinLamp.SYNCED, true), 3);
                this.syncedLamps.add(pos);
            });
        }

        Fireflies.LOGGER.debug("{} Illumerin Lamp search took {}ms of server time", this.firefly.toString(), System.currentTimeMillis() - time);
    }

    /**
     * Convert from redstone firefly to regular firefly.
     */
    public void removeRedstoneCoated() {
        this.firefly.redstoneManager.setRedstoneCoated(false);
        this.spawnRedstoneParticlePuff(0.25f);
        this.desyncLamps();
        this.syncedLamps.clear();
        this.poweredLamps.clear();
        this.firefly.particleManager.resetAbdomenParticle();
        this.firefly.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f); // TODO temp sound
    }

    public void convertRedstoneCoated(PlayerEntity player, ItemStack heldItem) {
        this.firefly.redstoneManager.setRedstoneCoated(true);
        // Reset any illumerin related data
        this.firefly.setIllumerinDeposited(0);
        this.firefly.setHasIllumerin(false);
        // Reset the water and rain ticks, so it doesn't wash off instantly if applied in rain or water
        this.firefly.rainedOnTicks = 0;
        this.firefly.underWaterTicks = 0;
        if (!this.world.isRemote) {
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            this.spawnRedstoneParticlePuff(1f);
        } else {
            player.playSound(Registry.FIREFLY_APPLY_REDSTONE.get(), 1f, 1f);
            this.firefly.particleManager.resetAbdomenParticle();
        }
    }

    /**
     * Spawn a bunch of redstone particles around the firefly on the server.
     */
    private void spawnRedstoneParticlePuff(float power) {
        final int amount = 5 + this.random.nextInt(5);
        for (int i = 0; i < amount; i++) {
            final float randPos = this.firefly.isChild() ? 0.22f : 0.66f;
            ((ServerWorld) (this.world)).spawnParticle(new RedstoneParticleData(power, 0, 0, 1f),
                    this.firefly.getPosX() + MathHelper.nextFloat(this.random, -randPos, randPos),
                    this.firefly.getPosY() + MathHelper.nextFloat(this.random, 0f, randPos * 1.33f),
                    this.firefly.getPosZ() + MathHelper.nextFloat(this.random, -randPos, randPos),
                    0, 0, 0, 0, 0);
        }
    }

    /**
     * @return The radius of which a redstone firefly will search for illumerin lamps
     */
    private int getMaxLampSearchRange() {
        return this.firefly.isChild() ? 8 : 16;
    }

    /**
     * @return The maximum amount of lamps this a redstone firefly can sync to
     */
    private int getMaxSyncedLamps() {
        return this.firefly.isChild() ? 32 : 64;
    }

    /**
     * @return The maximum amount of lamps a redstone firefly can power
     */
    private int getMaxPoweredLamps() {
        return this.firefly.isChild() ? 8 : 16;
    }

    /**
     * @return The distance of which illumerin lamps can be powered.
     */
    private int getMaxPoweredLampDist() {
        return this.firefly.isChild() ? 4 : 8;
    }

    /**
     * @return How far a redstone firefly can be from a synced lamp before it desyncs.
     */
    private int getMaxSyncDistance() {
        return this.firefly.isChild() ? 24 : 48;
    }

    public boolean isRedstoneCoated(boolean getCached) {
        if (getCached) {
            // This method is called every frame in our abdomen layer renderer, so this statement *will* pass
            if (this.firefly.ticksExisted % 20 == 0) {
                this.cachedIsRedstoneCoated = this.firefly.getDataManager().get(FireflyEntity.IS_REDSTONE_COATED);
            }
            return this.cachedIsRedstoneCoated;
        }

        final boolean isRedstoneCoated = this.firefly.getDataManager().get(FireflyEntity.IS_REDSTONE_COATED);
        this.cachedIsRedstoneCoated = isRedstoneCoated;
        return isRedstoneCoated;
    }

    public void setRedstoneCoated(boolean b) {
        this.cachedIsRedstoneCoated = b;
        this.firefly.getDataManager().set(FireflyEntity.IS_REDSTONE_COATED, b);
    }

    private class LampPosition {
        public BlockPos pos;
        public double distSq;

        public LampPosition(BlockPos pos) {
            this.pos = pos;
            this.distSq = pos.distanceSq(RedstoneFireflyManager.this.firefly.getPositionVec(), true);
        }

        @Override
        public String toString() {
            return String.format("LampPosition{%s, %s}", this.pos, this.distSq);
        }
    }

}
