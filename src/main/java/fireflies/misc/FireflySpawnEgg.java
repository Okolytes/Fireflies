package fireflies.misc;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.NonNullList;

public class FireflySpawnEgg extends SpawnEggItem {
    public FireflySpawnEgg(EntityType<?> typeIn, int primaryColorIn, int secondaryColorIn, Properties builder) {
        super(typeIn, primaryColorIn, secondaryColorIn, builder);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        // Relocate from the bottom of the creative tab to next to the fox spawn egg to fit with alphabetical order,
        // TODO Maybe this will break if the locale is different? eg en español Fox = (Z)orro, Firefly = (L)uciérnaga
        if (group.equals(ItemGroup.MISC) || group.equals(ItemGroup.SEARCH)) {
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getItem().equals(Items.FOX_SPAWN_EGG)) {
                    index = i;
                }
            }

            if (index != -1) {
                items.add(index + 1, new ItemStack(this));
            } else {
                super.fillItemGroup(group, items);
            }
        }
    }
}
