package fireflies.setup;

import fireflies.entity.firefly.FireflyEntity;
import fireflies.Fireflies;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirefliesSetup {

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GlobalEntityTypeAttributes.put(FirefliesRegistration.FIREFLY.get(), FireflyEntity.createAttributes().create());
        });
    }
}