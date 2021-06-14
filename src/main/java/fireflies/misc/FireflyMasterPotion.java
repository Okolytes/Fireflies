package fireflies.misc;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Potion;

public class FireflyMasterPotion extends Potion {
    public FireflyMasterPotion(EffectInstance... effectsIn) {
        super("firefly_master", effectsIn);
    }

    public static class HiddenFireflyMasterEffect extends Effect {
        public HiddenFireflyMasterEffect() {
            super(EffectType.NEUTRAL, 0xfffad420);
        }

        @Override
        public boolean shouldRender(EffectInstance effect) {
            return false;
        }

        @Override
        public boolean shouldRenderInvText(EffectInstance effect) {
            return false;
        }

        @Override
        public boolean shouldRenderHUD(EffectInstance effect) {
            return false;
        }
    }
}