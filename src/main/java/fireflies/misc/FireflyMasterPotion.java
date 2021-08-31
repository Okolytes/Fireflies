package fireflies.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class FireflyMasterPotion extends Potion {
    public FireflyMasterPotion(EffectInstance... effectsIn) {
        super("firefly_master", effectsIn);
    }

    public static void hideFireflyMasterToolTip(ItemTooltipEvent event) {
        final CompoundNBT tag = event.getItemStack().getTag();
        if (tag != null && tag.getString("Potion").contains("firefly_master")) {
            event.getToolTip().removeIf(textComponent -> textComponent.getString().contains("firefly_master_effect"));
        }
    }

    // We use a hidden effect otherwise we can't get a custom colour.
    // https://forums.minecraftforge.net/topic/101238-1165-creating-a-potion-with-a-custom-colour/
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

    public static class FireflyMasterBrewingRecipe implements IBrewingRecipe {
        private final ItemStack input;
        private final ItemStack ingredient;
        private final ItemStack output;

        public FireflyMasterBrewingRecipe(Potion input, ItemStack ingredient, Potion output) {
            this.input = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), input);
            this.ingredient = ingredient;
            this.output = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), output);
        }

        @Override
        public boolean isInput(ItemStack input) {
            return input.isItemEqual(this.input) && PotionUtils.getPotionFromItem(input).equals(PotionUtils.getPotionFromItem(this.input));
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.isItemEqual(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return this.isInput(input) && this.isIngredient(ingredient) ? this.output.copy() : ItemStack.EMPTY;
        }
    }
}