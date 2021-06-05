package fireflies.misc;

import fireflies.init.Registry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class FireflyMasterPotion extends Potion
{
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
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("CustomPotionColor", 0xfff700);
            itemStack.setTag(nbt);
            PotionUtils.addPotionToItemStack(itemStack, Registry.FIREFLY_MASTER.get());
            return itemStack;
        }
    }

}