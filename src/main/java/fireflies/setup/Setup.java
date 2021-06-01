package fireflies.setup;

import fireflies.Fireflies;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Setup {

    public static void init(final FMLCommonSetupEvent event) {
        // Register all of our entity attributes
        event.enqueueWork(() -> {
            GlobalEntityTypeAttributes.put(Registry.FIREFLY.get(), FireflyEntity.createAttributes().create());
        });
    }
}
