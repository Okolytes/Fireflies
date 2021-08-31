package fireflies.misc;

import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.util.NonNullList;

public class FireflySpawnEgg extends SpawnEggItem {
    private final boolean soulFirefly;
    public FireflySpawnEgg(EntityType<?> typeIn, int primaryColorIn, int secondaryColorIn, boolean soulFirefly) {
        super(typeIn, primaryColorIn, secondaryColorIn, new Item.Properties().group(ItemGroup.MISC));
        this.soulFirefly = soulFirefly;
    }

    /**
     * Relocate from the bottom of the creative tab to next to the fox spawn egg (or slime egg if its soul firefly) to fit with alphabetical order
     */
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (group == ItemGroup.MISC || group == ItemGroup.SEARCH) {
            int index = -1;
            // Get the spawn egg's index
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getItem() == (this.soulFirefly ? Items.SLIME_SPAWN_EGG : Items.FOX_SPAWN_EGG)) {
                    index = i;
                }
            }

            if (index != -1) {
                // Put it next to the spawn egg
                items.add(index + 1, new ItemStack(this));
            } else {
                super.fillItemGroup(group, items);
            }
        }
    }
}
