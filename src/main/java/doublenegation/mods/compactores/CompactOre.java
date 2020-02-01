package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class CompactOre {

    private CompactOreBlock block;
    private Item blockItem;

    private ResourceLocation baseBlockLoc;
    private int minRolls;
    private int maxRolls;
    private ResourceLocation baseOreTexture;
    private ResourceLocation baseUnderlyingTexture;
    private float spawnProbability;
    private boolean useGetDrops;
    private int maxOreLayerColorDiff;
    private boolean lateGeneration;

    private ResourceLocation registryName;

    public CompactOre(ResourceLocation baseBlockLoc, int minRolls, int maxRolls, ResourceLocation baseOreTexture,
                      ResourceLocation baseUnderlyingTexture, float spawnProbability, boolean useGetDrops,
                      int maxOreLayerColorDiff, boolean lateGeneration) {
        this.baseBlockLoc = baseBlockLoc;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.baseOreTexture = new ResourceLocation(baseOreTexture.getNamespace(), "textures/" + baseOreTexture.getPath() + ".png");
        this.baseUnderlyingTexture = new ResourceLocation(baseUnderlyingTexture.getNamespace(), "textures/" + baseUnderlyingTexture.getPath() + ".png");
        this.spawnProbability = spawnProbability;
        this.useGetDrops = useGetDrops;
        this.maxOreLayerColorDiff = maxOreLayerColorDiff;
        this.lateGeneration = lateGeneration;
        this.registryName = new ResourceLocation("compactores", "compact_" +
                baseBlockLoc.getNamespace() + "_" + baseBlockLoc.getPath());
    }

    public void init1_block() {
        block = new CompactOreBlock(registryName, baseBlockLoc, useGetDrops, minRolls, maxRolls);
    }

    public void init2_item() {
        blockItem = new CompactOreBlockItem(block);
    }

    public CompactOreBlock getBlock() {
        return block;
    }

    public Item getBlockItem() {
        return blockItem;
    }

    /**<b>Do NOT call before all mods have registered all their blocks.</b>*/
    public Block getBaseBlock() {
        return block.baseBlock();
    }

    public int getMinRolls() {
        return minRolls;
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public ResourceLocation getBaseOreTexture() {
        return baseOreTexture;
    }

    public ResourceLocation getBaseUnderlyingTexture() {
        return baseUnderlyingTexture;
    }

    public float getSpawnProbability() {
        return spawnProbability;
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public int getMaxOreLayerColorDiff() {
        return maxOreLayerColorDiff;
    }

    public boolean isLateGeneration() {
        return lateGeneration;
    }

}
