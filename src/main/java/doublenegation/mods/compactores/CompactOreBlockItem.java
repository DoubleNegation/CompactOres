package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.GameData;

import java.util.Map;

public class CompactOreBlockItem extends BlockItem {

    private Block block;

    public CompactOreBlockItem(Block blockIn) {
        super(blockIn, new Item.Properties().group(CompactOres.getItemGroup()));
        block = blockIn;
        setRegistryName(blockIn.getRegistryName());
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return DistExecutor.runForDist(() -> () -> {
            // CLIENT
            return block.getNameTextComponent();
        }, () -> () -> {
            // SERVER
            // Block#getNameTextComponent() is not a thing on the dedicated server, so don't use it here.
            // Language files apparently also aren't, so a TranslationTextComponent won't do much good either.
            if(block instanceof CompactOreBlock) {
                Map<Block, Item> blockItemMap = GameData.getBlockItemMap();
                Block baseBlock = ((CompactOreBlock) block).baseBlock();
                if(blockItemMap.containsKey(baseBlock)) {
                    ITextComponent baseName = blockItemMap.get(baseBlock).getDisplayName(stack);
                    StringTextComponent name = new StringTextComponent("Compact ");
                    name.appendSibling(baseName);
                    return name;
                }
            }
            return new StringTextComponent("Compact Ore");
        });
    }

    @Override
    public ITextComponent getName() {
        return block.getNameTextComponent();
    }

}
