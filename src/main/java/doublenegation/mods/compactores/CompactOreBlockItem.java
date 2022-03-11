package doublenegation.mods.compactores;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.GameData;

public class CompactOreBlockItem extends BlockItem {

    private final CompactOre ore;

    public CompactOreBlockItem(CompactOre ore) {
        super(ore.getCompactOreBlock(), new Item.Properties().tab(CompactOres.getItemGroup()));
        this.ore = ore;
    }

    // make sure that there is an order to the ore list
    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        if(getCreativeTabs().contains(group)) {
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
        Item baseItem = GameData.getBlockItemMap().get(ore.getBaseBlock());
        Component baseName = baseItem == null ? new TextComponent("<unknown>") :
                baseItem.getName(new ItemStack(baseItem, 1));
        return new TranslatableComponent("block." + ore.name().getNamespace() + ".compact_ore", baseName);
    }
}
