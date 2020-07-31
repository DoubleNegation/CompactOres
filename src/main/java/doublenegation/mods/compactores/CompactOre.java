package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompactOre implements Comparable<CompactOre>, IStringSerializable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Set<String> usedResourceNames = new HashSet<>();

    private boolean isReal = true;

    private String resourceName;
    private ResourceLocation baseBlockLoc;
    private Block baseBlock;
    private int minRolls;
    private int maxRolls;
    private ResourceLocation baseOreTexture;
    private ResourceLocation baseUnderlyingTexture;
    private float spawnProbability;
    private int maxOreLayerColorDiff;
    private boolean lateGeneration;
    private boolean generateTexture;
    private boolean useGetDrops;

    public CompactOre(ResourceLocation baseBlockLoc, int minRolls, int maxRolls, ResourceLocation baseOreTexture,
                      ResourceLocation baseUnderlyingTexture, float spawnProbability, int maxOreLayerColorDiff,
                      boolean lateGeneration, boolean generateTexture, boolean useGetDrops) {
        this.baseBlockLoc = baseBlockLoc;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.baseOreTexture = baseOreTexture == null ? null : new ResourceLocation(baseOreTexture.getNamespace(), "textures/" + baseOreTexture.getPath() + ".png");
        this.baseUnderlyingTexture = baseUnderlyingTexture == null ? null : new ResourceLocation(baseUnderlyingTexture.getNamespace(), "textures/" + baseUnderlyingTexture.getPath() + ".png");
        this.spawnProbability = spawnProbability;
        this.maxOreLayerColorDiff = maxOreLayerColorDiff;
        this.lateGeneration = lateGeneration;
        this.generateTexture = generateTexture;
        this.useGetDrops = useGetDrops;
        String resourceName = baseBlockLoc.toString().replace(":", "__");
        while(usedResourceNames.contains(resourceName)) {
            resourceName += "_";
        }
        this.resourceName = resourceName;
        usedResourceNames.add(resourceName);
    }

    CompactOre() {
        // construct the "missing" ore
        this(new ResourceLocation("stone"), 0, 0, null, null, 0, -1, false, false, false);
        // actually use a different resource name
        usedResourceNames.remove(resourceName);
        resourceName = "missing";
        usedResourceNames.add(resourceName);
        isReal = false;
    }

    public ResourceLocation getBaseBlockRegistryName() {
        return baseBlockLoc;
    }

    /**<b>Do NOT call before all mods have registered all their blocks.</b>*/
    public Block getBaseBlock() {
        if(baseBlock == null) {
            baseBlock = ForgeRegistries.BLOCKS.getValue(baseBlockLoc);
            if(baseBlock == Blocks.AIR) {
                LOGGER.error("Block " + baseBlockLoc + " does not exist - failed to create compact ore");
                baseBlock = null;
            }
        }
        return baseBlock;
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

    public int getMaxOreLayerColorDiff() {
        return maxOreLayerColorDiff;
    }

    public boolean isLateGeneration() {
        return lateGeneration;
    }

    public boolean isGenerateTexture() {
        return generateTexture;
    }

    public boolean isUseGetDrops() {
        return useGetDrops;
    }

    public boolean isReal() {
        return isReal;
    }

    @Override
    public int compareTo(CompactOre compactOre) {
        ModList.get().getMods();
        if(this.baseBlockLoc.getNamespace().equals(compactOre.baseBlockLoc.getNamespace())) {
            return this.baseBlockLoc.getPath().compareTo(compactOre.baseBlockLoc.getPath());
        } else {
            List<ModInfo> modList = ModList.get().getMods();
            int thisIndex = -1, otherIndex = -1;
            for(int i = 0; i < modList.size(); i++) {
                String modId = modList.get(i).getModId();
                if(this.baseBlockLoc.getNamespace().equals(modId)) thisIndex = i;
                else if(compactOre.baseBlockLoc.getNamespace().equals(modId)) otherIndex = i;
                if(thisIndex != -1 && otherIndex != -1) break;
            }
            return thisIndex - otherIndex;
        }
    }

    @Override
    public String getString() {
        return resourceName;
    }

}
