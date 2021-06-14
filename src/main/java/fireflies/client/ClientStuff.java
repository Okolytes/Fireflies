package fireflies.client;

import fireflies.Fireflies;
import fireflies.client.sound.FireflyFlightLoopSound;
import fireflies.entity.firefly.FireflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Random;

// Java classloading forces us to put calls to the Minecraft class in here
@Mod.EventBusSubscriber(modid = Fireflies.MOD_ID, value = Dist.CLIENT)
public class ClientStuff {
    public static void playFireflyLoopSound(FireflyEntity fireflyEntity) {
        Minecraft.getInstance().getSoundHandler().playOnNextTick(new FireflyFlightLoopSound(fireflyEntity));
    }

    public static boolean isGamePaused() {
        return Minecraft.getInstance().isGamePaused();
    }

    @SubscribeEvent
    public static void hideFireflyMasterToolTip(ItemTooltipEvent event) {
        CompoundNBT tag = event.getItemStack().getTag();
        if (tag != null && tag.getString("Potion").contains("firefly_master")) {
            event.getToolTip().removeIf(textComponent -> textComponent.getString().contains("firefly_master_effect"));
        }
    }

    @SubscribeEvent
    public static void changeSplashText(GuiOpenEvent event) {
        if (event.getGui() instanceof MainMenuScreen) {
            if (Math.random() > 0.99f) { // 1 in 100 chance - this is run every time the main menu screen is opened, not when the game opens - not intentional but oh well!
                String[] splashes = {
                        "You would not believe your eyes...",
                        "If ten million fireflies...",
                        "I'd get a thousand hugs, from ten thousand lightning bugs",
                };
                String newSplashText = splashes[new Random().nextInt(splashes.length)];

                try {
                    ObfuscationReflectionHelper.setPrivateValue(MainMenuScreen.class, (MainMenuScreen) event.getGui(), newSplashText, "field_73975_c");
                } catch (ObfuscationReflectionHelper.UnableToAccessFieldException | ObfuscationReflectionHelper.UnableToFindFieldException e) {
                    Fireflies.LOGGER.error("Failed miserably at setting the splash text.", e);
                }
            }
        }
    }
}
