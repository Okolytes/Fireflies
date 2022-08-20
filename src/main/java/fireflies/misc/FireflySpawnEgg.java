package fireflies.misc;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

public class FireflySpawnEgg extends ForgeSpawnEggItem {
    public FireflySpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Properties props) {
        super(type, backgroundColor, highlightColor, props);
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        // Relocate from the bottom of the creative tab to next to the fox spawn egg (or slime egg if its soul firefly) to fit with alphabetical order
        if (pCategory == CreativeModeTab.TAB_MISC || pCategory == CreativeModeTab.TAB_SEARCH) {
            int index = -1;
            // Get the spawn egg's index
            for (int i = 0; i < pItems.size(); i++) {
                if (pItems.get(i).getItem() == Items.FOX_SPAWN_EGG) {
                    index = i;
                }
            }

            if (index != -1) {
                // Put it next to the spawn egg
                pItems.add(index + 1, new ItemStack(this));
            } else {
                super.fillItemCategory(pCategory, pItems);
            }
        }
    }
}
