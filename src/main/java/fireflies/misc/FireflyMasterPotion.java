package fireflies.misc;

import fireflies.Fireflies;
import fireflies.init.Registry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID)
public class FireflyMasterPotion extends Potion {
    public FireflyMasterPotion(EffectInstance... effectsIn) {
        super("firefly_master", effectsIn);
    }

    @SubscribeEvent
    public static void hideFireflyMasterToolTip(ItemTooltipEvent event) {
        CompoundNBT tag = event.getItemStack().getTag();
        if (tag != null && tag.getString("Potion").contains("firefly_master")) {
            event.getToolTip().removeIf(textComponent -> textComponent.getString().contains("firefly_master_effect"));
        }
    }

    public static class FireflyMasterPotionRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return PotionUtils.getPotionFromItem(input) == Potions.AWKWARD;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.getItem() == Registry.ILLUMERIN.get();
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!this.isInput(input) || !this.isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }

            ItemStack itemStack = new ItemStack(input.getItem());
            itemStack.setTag(new CompoundNBT());
            PotionUtils.addPotionToItemStack(itemStack, Registry.FIREFLY_MASTER.get());
            return itemStack;
        }
    }

    public static class HiddenFireflyMasterEffect extends Effect {
        public HiddenFireflyMasterEffect() {
            super(EffectType.NEUTRAL, 0xfffad420);
        }

        @Override
        public boolean shouldRender(EffectInstance effect) {
            return false;
        }

        @Override
        public boolean shouldRenderInvText(EffectInstance effect) {
            return false;
        }

        @Override
        public boolean shouldRenderHUD(EffectInstance effect) {
            return false;
        }
    }
}