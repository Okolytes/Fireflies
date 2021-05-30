package fireflies;

import fireflies.setup.ClientSetup;
import fireflies.setup.Registry;
import fireflies.setup.Setup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Fireflies.MOD_ID)
public class Fireflies {

    public static final String MOD_ID = "fireflies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Fireflies() {
        Registry.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(Setup::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::init);
    }
}
