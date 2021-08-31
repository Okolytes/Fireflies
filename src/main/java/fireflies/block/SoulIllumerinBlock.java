package fireflies.block;

import net.minecraft.util.Direction;

public class SoulIllumerinBlock extends IllumerinBlock {
    public SoulIllumerinBlock() {
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }
}
