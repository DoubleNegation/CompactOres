package doublenegation.mods.compactores;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CompactOreBlockItem extends BlockItem {

    private final CompactOre ore;

    public CompactOreBlockItem(CompactOre ore) {
        super(ore.getCompactOreBlock(), new Item.Properties().tab(CompactOres.getItemGroup()));
        this.ore = ore;
    }

    // make sure that there is an order to the ore list
    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        if(getCreativeTabs().contains(group) || group == CreativeModeTab.TAB_SEARCH) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getItem() instanceof CompactOreBlockItem) {
                    if (ore.compareTo(((CompactOreBlockItem) items.get(i).getItem()).ore) < 0) {
                        items.add(i, new ItemStack(this, 1));
                        return;
                    }
                }
            }
            items.add(new ItemStack(this, 1));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return getBlock().getName();
    }
}
