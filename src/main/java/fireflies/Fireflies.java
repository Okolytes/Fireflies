package fireflies;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.init.ClientSetup;
import fireflies.init.Registry;
import fireflies.misc.FireflyMasterPotion;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MOD_ID)
public class Fireflies {
    public static final String MOD_ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Fireflies() {
        Registry.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::init);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("deprecation") // TODO
    public void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GlobalEntityTypeAttributes.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
        });

        DeferredWorkQueue.runLater(() -> BrewingRecipeRegistry.addRecipe(new FireflyMasterPotion.FireflyMasterPotionRecipe()));
    }
}
