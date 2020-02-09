package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.GameData;

import java.util.Map;

public class CompactOreBlockItem extends BlockItem {

    public CompactOreBlockItem(Block blockIn) {
        super(blockIn, new Item.Properties().group(CompactOres.getItemGroup()));
        addPropertyOverride(new ResourceLocation(CompactOres.MODID, "ore"), (stack, world, holder) -> {
            CompactOre ore = findOreForStack(stack);
            return CompactOres.compactOres().indexOf(ore);
        });
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if(isInGroup(group)) {
            for(CompactOre ore : CompactOres.compactOres()) {
                items.add(getStackOfOre(ore, 1));
            }
        }
    }

    public CompactOre findOreForStack(ItemStack stack) {
        // default to the first compact ore in the list
        CompactOre ore = CompactOres.compactOres().get(0);
        // if the item stack has a specific ore set, use that one
        if(stack.getTag() != null) {
            CompoundNBT tag = stack.getTag();
            if(tag.contains("BlockEntityTag", 10 /*Compound*/)) {
                CompoundNBT blockEntityTag = tag.getCompound("BlockEntityTag");
                if(blockEntityTag.contains("ore", 8 /*String*/)) {
                    String oreName = blockEntityTag.getString("ore");
                    ResourceLocation baseOre = Utils.parseResourceLocation(oreName);
                    CompactOre actualOre = CompactOres.getFor(baseOre);
                    if(actualOre != null) {
                        ore = actualOre;
                    }
                }
            }
        }
        return ore;
    }

    public ItemStack getStackOfOre(CompactOre ore, int size) {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT blockEntityTag = new CompoundNBT();
        blockEntityTag.putString("ore", ore.getBaseBlockRegistryName().toString());
        tag.put("BlockEntityTag", blockEntityTag);
        ItemStack is = new ItemStack(this, size);
        is.setTag(tag);
        return is;
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        CompactOre ore = findOreForStack(stack);
        ResourceLocation bln = CompactOres.COMPACT_ORE.getId();
        Map<Block, Item> blockItemMap = GameData.getBlockItemMap();
        ITextComponent baseName = blockItemMap.get(ore.getBaseBlock()).getDisplayName(stack);
        return new TranslationTextComponent("block." + bln.getNamespace() + "." + bln.getPath(), baseName);
    }

}
