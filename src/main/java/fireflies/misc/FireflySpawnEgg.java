package fireflies.misc;

import fireflies.Fireflies;
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
        if (!Fireflies.creativeTabItemPlacement(this, Items.FOX_SPAWN_EGG, pCategory, pItems)) {
            super.fillItemCategory(pCategory, pItems);
        }
    }
}
