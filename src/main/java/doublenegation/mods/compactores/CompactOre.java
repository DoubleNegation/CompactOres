package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class CompactOre {

    private CompactOreBlock block;
    private Item blockItem;

    private Block baseBlock;
    private int minRolls;
    private int maxRolls;
    private ResourceLocation baseLootTable;

    public CompactOre(Block baseBlock, int minRolls, int maxRolls, ResourceLocation baseLootTable) {
        this.baseBlock = baseBlock;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.baseLootTable = baseLootTable;
    }

    public void init1_block() {
        block = new CompactOreBlock(baseBlock);
        CompactOres.LOGGER.debug(block);
    }

    public void init2_item() {
        blockItem = new BlockItem(block, new Item.Properties()).setRegistryName(block.getRegistryName());
    }

    public CompactOreBlock getBlock() {
        return block;
    }

    public Item getBlockItem() {
        return blockItem;
    }

    public Block getBaseBlock() {
        return baseBlock;
    }

    public ResourceLocation getBaseLootTable() {
        return baseLootTable;
    }

    public int getMinRolls() {
        return minRolls;
    }

    public int getMaxRolls() {
        return maxRolls;
    }

}
