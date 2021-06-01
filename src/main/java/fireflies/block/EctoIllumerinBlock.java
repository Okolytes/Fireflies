package fireflies.block;

import net.minecraft.util.Direction;

public class EctoIllumerinBlock extends IllumerinBlock {
    public EctoIllumerinBlock() {
        this.setDefaultState(this.stateContainer.getBaseState().with(AXIS, Direction.Axis.Y));
    }
}
