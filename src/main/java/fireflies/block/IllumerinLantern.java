package fireflies.block;

import fireflies.Fireflies;
import fireflies.Registry;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class IllumerinLantern extends LanternBlock {
    public IllumerinLantern() {
        super(BlockBehaviour.Properties.of(Material.METAL).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.LANTERN).noOcclusion());
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (!Fireflies.creativeTabItemPlacement(Registry.ILLUMERIN_LANTERN_ITEM.get(), Items.SOUL_LANTERN, pCategory, pItems)) {
            super.fillItemCategory(pCategory, pItems);
        }
    }
}

