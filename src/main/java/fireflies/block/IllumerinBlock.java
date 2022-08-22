package fireflies.block;

import fireflies.Fireflies;
import fireflies.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class IllumerinBlock extends HalfTransparentBlock {
    private static final int ILLUMERIN_RADIUS = 15;
    private static final int LANTERN_ILLUMERIN_RADIUS = 8;

    public IllumerinBlock() {
        super(Properties.of(Material.CLAY, MaterialColor.GRASS).noOcclusion().sound(SoundType.SLIME_BLOCK).lightLevel(a -> 1).isValidSpawn((a, b, c, d) -> false));
    }

    @SubscribeEvent
    public static void stopMobSpawning(LivingSpawnEvent.CheckSpawn event) {
        if (event.getLevel() == null || event.getLevel().isClientSide() || event.getEntity() == null)
            return;

        // Only disable spawner and natural spawns
        if (event.getSpawnReason() != MobSpawnType.NATURAL && event.getSpawnReason() != MobSpawnType.SPAWNER)
            return;

        // Only disable monster spawns and bats
        if (!event.getEntity().getType().getCategory().equals(MobCategory.MONSTER) && !(event.getEntity() instanceof Bat))
            return;

        // Cancel the spawn if an illumerin is within radius
        boolean cancelMobSpawn = false;
        final BlockPos mobPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final var radius = Math.max(ILLUMERIN_RADIUS, LANTERN_ILLUMERIN_RADIUS);
        for (double x = event.getX() - radius; x < event.getX() + radius; x++) {
            for (double y = event.getY() - radius; y < event.getY() + radius; y++) {
                for (double z = event.getZ() - radius; z < event.getZ() + radius; z++) {
                    final BlockPos blockPos = new BlockPos(x, y, z);
                    // squared distance
                    if (!blockPos.closerThan(mobPos, radius)) {
                        continue;
                    }

                    final BlockState state = event.getLevel().getBlockState(blockPos);
                    final Block block = state.getBlock();
                    boolean flag = block instanceof IllumerinLantern;
                    if (block instanceof IllumerinBlock || flag) {
                        if (flag && !blockPos.closerThan(mobPos, LANTERN_ILLUMERIN_RADIUS))
                        {
                            continue;
                        }

                        cancelMobSpawn = true;
                        break;
                    }
                }
            }
        }

        if (cancelMobSpawn) {
            // Remove any passengers that come with it
            for (var passenger : event.getEntity().getPassengers()){
                passenger.discard();
            }

            // Finally, cancel the mob spawn
            event.setResult(Event.Result.DENY);
        }
    }


    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (!Fireflies.creativeTabItemPlacement(Registry.ILLUMERIN_BLOCK_ITEM.get(), Items.SHROOMLIGHT, pCategory, pItems)) {
            super.fillItemCategory(pCategory, pItems);
        }
    }
}
