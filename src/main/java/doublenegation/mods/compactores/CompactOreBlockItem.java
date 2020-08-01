package doublenegation.mods.compactores;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.GameData;

public class CompactOreBlockItem extends BlockItem {

    private CompactOre ore;

    public CompactOreBlockItem(CompactOre ore) {
        super(ore.getCompactOreBlock(), new Item.Properties().group(CompactOres.getItemGroup()));
        this.ore = ore;
    }

    // make sure that there is an order to the ore list
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        for(int i = 0; i < items.size(); i++) {
            if(items.get(i).getItem() instanceof CompactOreBlockItem) {
                if(ore.compareTo(((CompactOreBlockItem)items.get(i).getItem()).ore) < 0) {
                    items.add(i, new ItemStack(this, 1));
                    return;
                }
            }
        }
        items.add(new ItemStack(this, 1));
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return getName();
    }

    @Override
    public ITextComponent getName() {
        Item baseItem = GameData.getBlockItemMap().get(ore.getBaseBlock());
        ITextComponent baseName = baseItem == null ? new StringTextComponent("<unknown>") :
                baseItem.getDisplayName(new ItemStack(baseItem, 1));
        return new TranslationTextComponent("block." + ore.name().getNamespace() + ".compact_ore", baseName);
    }
}
