package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class CompactOreBlockItem extends BlockItem {

    private Block block;

    public CompactOreBlockItem(Block blockIn) {
        super(blockIn, new Item.Properties().group(CompactOres.getItemGroup()));
        block = blockIn;
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return block.getNameTextComponent();
    }

    @Override
    public ITextComponent getName() {
        return block.getNameTextComponent();
    }

}
