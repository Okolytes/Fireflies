package fireflies.item;

import fireflies.Fireflies;
import fireflies.Registry;
import fireflies.entity.FireflyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class FireflyBottleItem extends Item {

    public FireflyBottleItem() {
        super(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1));
    }

    // On right click firefly with glass bottle
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity target = event.getTarget();
        if (!(target instanceof FireflyEntity firefly) || !target.isAlive()) {
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();

        if (itemstack.isEmpty() || item != Items.GLASS_BOTTLE) {
            return;
        }

        ItemStack fireflyBottle = new ItemStack(Registry.FIREFLY_BOTTLE.get());
        var tag = fireflyBottle.getOrCreateTag();
        firefly.addAdditionalSaveData(tag);

        if (target.hasCustomName()) {
            fireflyBottle.setHoverName(target.getCustomName());
        }

        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }
        event.getLevel().playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(item));
        if (itemstack.isEmpty()) {
            player.setItemInHand(hand, fireflyBottle);
        } else if (!player.getInventory().add(fireflyBottle)) {
            player.drop(fireflyBottle, false);
        }
        player.swing(hand, true);
        target.discard();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack bottle = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockState = level.getBlockState(blockPos);

        if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
            blockPos = blockPos.relative(direction);
        }

        var tag = bottle.getTag();
        if (tag == null) {
            Fireflies.LOGGER.error("Attempted to spawn a firefly from a bottle with empty nbt data");
            return InteractionResult.PASS;
        }

        var entity = Registry.FIREFLY.get().spawn((ServerLevel) level, bottle, player, blockPos, MobSpawnType.BUCKET, true, false);
        if (!(entity instanceof FireflyEntity firefly)) {
            Fireflies.LOGGER.error("Failed to spawn firefly from a bottle");
            return InteractionResult.PASS;
        }

        firefly.readAdditionalSaveData(tag);

        level.playSound(null, context.getClickedPos(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

        var glassBottle = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(context.getHand(), glassBottle);
        } else if (!player.getInventory().add(glassBottle)) {
            player.drop(glassBottle, false);
        }
        player.swing(context.getHand(), true);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        //todo fireflies specie tooltip
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (!Fireflies.creativeTabItemPlacement(this, Items.EXPERIENCE_BOTTLE, pCategory, pItems)) {
            super.fillItemCategory(pCategory, pItems);
        }
    }
}
