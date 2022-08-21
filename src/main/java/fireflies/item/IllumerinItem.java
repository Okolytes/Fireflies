package fireflies.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IllumerinItem extends Item {

    public IllumerinItem() {
        super(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity target, InteractionHand pUsedHand) {
        if (target.level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!target.isAlive()) {
            return InteractionResult.FAIL;
        }

        if (target instanceof Frog) {
            if (!pPlayer.getAbilities().instabuild) {
                pStack.shrink(1);
            }

            // taken from parrot feed cookie code
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (pPlayer.isCreative() || !target.isInvulnerable()) {
                target.playSound(SoundEvents.GENERIC_EXPLODE); // todo update
                target.hurt(DamageSource.playerAttack(pPlayer), Float.MAX_VALUE);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
