package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompactOre implements Comparable<CompactOre>, IStringSerializable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CompactOres.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CompactOres.MODID);
    static {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private static Set<String> usedResourceNames = new HashSet<>();

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
    private boolean experimentalGenerator;
    private boolean generateTexture;
    private boolean useGetDrops;
    private RegistryObject<CompactOreBlock> compactOreBlock;
    private ResourceLocation compactOreBlockRegistryName;
    private RegistryObject<CompactOreBlockItem> compactOreBlockItem;

    public CompactOre(ResourceLocation baseBlockLoc, int minRolls, int maxRolls, ResourceLocation baseOreTexture,
                      ResourceLocation baseUnderlyingTexture, float spawnProbability, int maxOreLayerColorDiff,
                      boolean lateGeneration, boolean experimentalGenerator, boolean generateTexture,
                      boolean useGetDrops) {
        this.baseBlockLoc = baseBlockLoc;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.baseOreTexture = baseOreTexture == null ? null : new ResourceLocation(baseOreTexture.getNamespace(), "textures/" + baseOreTexture.getPath() + ".png");
        this.baseUnderlyingTexture = baseUnderlyingTexture == null ? null : new ResourceLocation(baseUnderlyingTexture.getNamespace(), "textures/" + baseUnderlyingTexture.getPath() + ".png");
        this.spawnProbability = spawnProbability;
        this.maxOreLayerColorDiff = maxOreLayerColorDiff;
        this.lateGeneration = lateGeneration;
        this.experimentalGenerator = experimentalGenerator;
        this.generateTexture = generateTexture;
        this.useGetDrops = useGetDrops;
        String resourceName = baseBlockLoc.toString().replace(":", "__");
        while(usedResourceNames.contains(resourceName)) {
            resourceName += "_";
        }
        this.resourceName = resourceName;
        usedResourceNames.add(resourceName);
        compactOreBlockRegistryName = new ResourceLocation(CompactOres.MODID, "compactore__" + this.resourceName);
        compactOreBlock = BLOCKS.register(compactOreBlockRegistryName.getPath(), () -> new CompactOreBlock(this));
        compactOreBlockItem = ITEMS.register(compactOreBlockRegistryName.getPath(), () -> new CompactOreBlockItem(this));
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

    public boolean isExperimentalGenerator() {
        return experimentalGenerator;
    }

    public boolean isGenerateTexture() {
        return generateTexture;
    }

    public boolean isUseGetDrops() {
        return useGetDrops;
    }

    public ResourceLocation name() {
        return compactOreBlockRegistryName;
    }

    public CompactOreBlock getCompactOreBlock() {
        return compactOreBlock.orElse(null);
    }

    public CompactOreBlockItem getCompactOreBlockItem() {
        return compactOreBlockItem.orElse(null);
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
