package fireflies;

import fireflies.setup.FirefliesClientSetup;
import fireflies.setup.FirefliesSetup;
import fireflies.setup.FirefliesRegistration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MOD_ID)
public class Fireflies {

    public static final String MOD_ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Fireflies() {
        FirefliesRegistration.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(FirefliesSetup::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(FirefliesClientSetup::init);
    }
}
