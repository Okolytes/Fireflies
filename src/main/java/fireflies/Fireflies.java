package fireflies;

import fireflies.client.AbdomenAnimationLoader;
import fireflies.entity.FireflyEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MOD_ID)
public class Fireflies {
    public static final String MOD_ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Fireflies() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Registry.register(modEventBus);
        modEventBus.addListener(this::createEntityAttributes);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AbdomenAnimationLoader::addFireflyAnimationsReloadListener);
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().build());
    }

    // I hate Orbic
    public static boolean creativeTabItemPlacement(Item thisItem, Item item, CreativeModeTab category, NonNullList<ItemStack> pItems) {
        if (category == thisItem.getItemCategory() || category == CreativeModeTab.TAB_SEARCH) {
            int index = -1;
            // Get the item of choice's index
            for (int i = 0; i < pItems.size(); i++) {
                if (pItems.get(i).getItem() == item) {
                    index = i;
                }
            }

            if (index != -1) {
                // Put our item next to the item of choice
                pItems.add(index + 1, new ItemStack(thisItem));
                return true;
            }
        }
        return false;
    }
}
