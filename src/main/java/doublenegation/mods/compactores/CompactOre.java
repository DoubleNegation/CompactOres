package doublenegation.mods.compactores;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompactOre implements Comparable<CompactOre>, StringRepresentable {

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
    private boolean retrogen;
    private boolean generateTexture;
    private boolean useGetDrops;
    private float breakTimeMultiplier;
    private RegistryObject<CompactOreBlock> compactOreBlock;
    private ResourceLocation compactOreBlockRegistryName;
    private RegistryObject<CompactOreBlockItem> compactOreBlockItem;

    public CompactOre(ResourceLocation baseBlockLoc, int minRolls, int maxRolls, ResourceLocation baseOreTexture,
                      ResourceLocation baseUnderlyingTexture, float spawnProbability, int maxOreLayerColorDiff,
                      boolean retrogen, boolean generateTexture, boolean useGetDrops, float breakTimeMultiplier) {
        this.baseBlockLoc = baseBlockLoc;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.baseOreTexture = baseOreTexture == null ? null : new ResourceLocation(baseOreTexture.getNamespace(), "textures/" + baseOreTexture.getPath() + ".png");
        this.baseUnderlyingTexture = baseUnderlyingTexture == null ? null : new ResourceLocation(baseUnderlyingTexture.getNamespace(), "textures/" + baseUnderlyingTexture.getPath() + ".png");
        this.spawnProbability = spawnProbability;
        this.maxOreLayerColorDiff = maxOreLayerColorDiff;
        this.retrogen = retrogen;
        this.generateTexture = generateTexture;
        this.useGetDrops = useGetDrops;
        this.breakTimeMultiplier = breakTimeMultiplier;
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

    public boolean isRetrogen() {
        return retrogen;
    }

    public boolean isGenerateTexture() {
        return generateTexture;
    }

    public boolean isUseGetDrops() {
        return useGetDrops;
    }

    public float getBreakTimeMultiplier() {
        return breakTimeMultiplier;
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
        if(this.baseBlockLoc.getNamespace().equals(compactOre.baseBlockLoc.getNamespace())) {
            return this.baseBlockLoc.getPath().compareTo(compactOre.baseBlockLoc.getPath());
        } else {
            List<IModInfo> modList = ModList.get().getMods();
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
    public String getSerializedName() {
        return resourceName;
    }

}
