package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;

public class CompactOre {

    private CompactOreBlock block;
    private Item blockItem;
    private LootTable lootTable;

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

    private LootTable makeLootTable() {
        return LootTable.builder().addLootPool(LootPool.builder().name("pool0").rolls(RandomValueRange.of(minRolls, maxRolls))
                .addEntry(TableLootEntry.builder(baseLootTable))).build();
    }

    public void init1_block() {
        block = new CompactOreBlock(baseBlock);
        CompactOres.LOGGER.debug(block);
    }

    public void init2_item() {
        blockItem = new BlockItem(block, new Item.Properties()).setRegistryName(block.getRegistryName());
    }

    public void init3_final() {
        lootTable = makeLootTable();
    }

    public CompactOreBlock getBlock() {
        return block;
    }

    public Item getBlockItem() {
        return blockItem;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public ResourceLocation getLootTableLocation() {
        return block.getRegistryName();
    }

}
