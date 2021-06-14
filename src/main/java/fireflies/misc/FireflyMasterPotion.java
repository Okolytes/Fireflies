package fireflies.misc;

import fireflies.init.Registry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class FireflyMasterPotion extends Potion {
    public FireflyMasterPotion(EffectInstance... effectsIn) {
        super("firefly_master", effectsIn);
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