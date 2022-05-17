package doublenegation.mods.compactores.config;

import doublenegation.mods.compactores.CompactOre;
import net.minecraft.resources.ResourceLocation;

public class OreBuilder {

    // GLOBAL DEFINITION DEFAULTS
    private static boolean G_GENERATETEXTURE = true;
    private static int G_MAXORELAYERCOLORDIFF = 50;
    private static boolean G_RETROGEN = true;

    // GLOBAL CUSTOMIZATION DEFAULTS
    private static int G_MINROLLS = 3;
    private static int G_MAXROLLS = 5;
    private static float G_SPAWNPROBABILITY = .1f;

    // LOCAL DEFINITION DEFAULTS
    private Boolean L_GENERATETEXTURE;
    private Integer L_MAXORELAYERCOLORDIFF;
    private ResourceLocation L_ORETEXTURE;
    private ResourceLocation L_ROCKTEXTURE;
    private Boolean L_RETROGEN;
    private boolean L_USEGETDROPS; /* default value = false, defined in Builder */

    // LOCAL CUSTOMIZATION DEFAULTS
    private Integer L_MINROLLS;
    private Integer L_MAXROLLS;
    private Float L_SPAWNPROBABILITY;

    // private definition values
    private Boolean generateTexture;
    private Integer maxOreLayerColorDiff;
    private ResourceLocation oreTexture;
    private ResourceLocation rockTexture;
    private Boolean retrogen;
    private Boolean useGetDrops;

    // private customization values
    private Integer minRolls;
    private Integer maxRolls;
    private Float spawnProbability;

    // further private values
    private ResourceLocation baseBlock;

    public static void setGlobalGenerateTexture(boolean value) {
        G_GENERATETEXTURE = value;
    }

    public static void setGlobalMaxOreLayerColorDiff(int value) {
        G_MAXORELAYERCOLORDIFF = value;
    }

    public static void setGlobalRetrogen(boolean value) {
        G_RETROGEN = value;
    }

    public static void setGlobalMinRolls(int value) {
        G_MINROLLS = value;
    }

    public static void setGlobalMaxRolls(int value) {
        G_MAXROLLS = value;
    }

    public static void setGlobalSpawnProbability(float value) {
        G_SPAWNPROBABILITY = value;
    }

    private OreBuilder(Boolean localGenerateTexture,
                       Integer localMaxOreLayerColorDiff,
                       ResourceLocation localOreTexture,
                       ResourceLocation localRockTexture,
                       Boolean retrogen,
                       Boolean localUseGetDrops,
                       Integer localMinRolls,
                       Integer localMaxRolls,
                       Float localSpawnProbability) {
        L_GENERATETEXTURE = localGenerateTexture;
        L_MAXORELAYERCOLORDIFF = localMaxOreLayerColorDiff;
        L_ORETEXTURE = localOreTexture;
        L_ROCKTEXTURE = localRockTexture;
        L_RETROGEN = retrogen;
        L_USEGETDROPS = localUseGetDrops;
        L_MINROLLS = localMinRolls;
        L_MAXROLLS = localMaxRolls;
        L_SPAWNPROBABILITY = localSpawnProbability;
    }

    public OreBuilder generateTexture(Boolean generateTexture) {
        this.generateTexture = generateTexture;
        return this;
    }

    public OreBuilder maxOreLayerColorDiff(Integer maxOreLayerColorDiff) {
        this.maxOreLayerColorDiff = maxOreLayerColorDiff;
        return this;
    }

    public OreBuilder oreTexture(ResourceLocation oreTexture) {
        this.oreTexture = oreTexture;
        return this;
    }

    public OreBuilder rockTexture(ResourceLocation rockTexture) {
        this.rockTexture = rockTexture;
        return this;
    }

    public OreBuilder retrogen(Boolean retrogen) {
        this.retrogen = retrogen;
        return this;
    }

    public OreBuilder useGetDrops(Boolean useGetDrops) {
        this.useGetDrops = useGetDrops;
        return this;
    }

    public OreBuilder minRolls(Integer minRolls) {
        this.minRolls = minRolls;
        return this;
    }

    public OreBuilder maxRolls(Integer maxRolls) {
        this.maxRolls = maxRolls;
        return this;
    }

    public OreBuilder spawnProbability(Float spawnProbability) {
        this.spawnProbability = spawnProbability;
        return this;
    }

    public OreBuilder baseBlock(ResourceLocation baseBlock) {
        this.baseBlock = baseBlock;
        return this;
    }

    public CompactOre build() {
        if(baseBlock == null) {
            throw new IllegalArgumentException("Can not build ore: baseBlock is null.");
        }
        // definition
        boolean actualGenerateTexture = generateTexture != null ? generateTexture : L_GENERATETEXTURE != null ? L_GENERATETEXTURE : G_GENERATETEXTURE;
        int actualMaxOreLayerColorDiff = maxOreLayerColorDiff != null ? maxOreLayerColorDiff : L_MAXORELAYERCOLORDIFF != null ? L_MAXORELAYERCOLORDIFF : G_MAXORELAYERCOLORDIFF;
        ResourceLocation actualOreTexture = oreTexture != null ? oreTexture : L_ORETEXTURE;
        ResourceLocation actualRockTexture = rockTexture != null ? rockTexture : L_ROCKTEXTURE;
        boolean actualRetrogen = retrogen != null ? retrogen : L_RETROGEN != null ? L_RETROGEN : G_RETROGEN;
        boolean actualUseGetDrops = useGetDrops != null ? useGetDrops : L_USEGETDROPS;
        // customization
        int actualMinRolls = minRolls != null ? minRolls : L_MINROLLS != null ? L_MINROLLS : G_MINROLLS;
        int actualMaxRolls = maxRolls != null ? maxRolls : L_MAXROLLS != null ? L_MAXROLLS : G_MAXROLLS;
        float actualSpawnProbability = spawnProbability != null ? spawnProbability : L_SPAWNPROBABILITY != null ? L_SPAWNPROBABILITY : G_SPAWNPROBABILITY;
        return new CompactOre(baseBlock, actualMinRolls, actualMaxRolls, actualOreTexture, actualRockTexture,
                actualSpawnProbability, actualMaxOreLayerColorDiff, actualRetrogen, actualGenerateTexture, actualUseGetDrops);
    }

    public static class Builder {

        // definition
        private Boolean generateTexture;
        private Integer maxOreLayerColorDiff;
        private ResourceLocation oreTexture;
        private ResourceLocation rockTexture;
        private Boolean retrogen;
        private boolean useGetDrops = false;

        // customization
        private Integer minRolls;
        private Integer maxRolls;
        private Float spawnProbability;

        private Builder() {}

        public static Builder create() {
            return new Builder();
        }

        public Builder generateTexture(Boolean generateTexture) {
            this.generateTexture = generateTexture;
            return this;
        }

        public Builder maxOreLayerColorDiff(Integer maxOreLayerColorDiff) {
            this.maxOreLayerColorDiff = maxOreLayerColorDiff;
            return this;
        }

        public Builder oreTexture(ResourceLocation oreTexture) {
            this.oreTexture = oreTexture;
            return this;
        }

        public Builder rockTexture(ResourceLocation rockTexture) {
            this.rockTexture = rockTexture;
            return this;
        }

        public Builder retrogen(Boolean retrogen) {
            this.retrogen = retrogen;
            return this;
        }

        public Builder useGetDrops(Boolean useGetDrops) {
            if(useGetDrops != null) {
                this.useGetDrops = useGetDrops;
            }
            return this;
        }

        public Builder minRolls(Integer minRolls) {
            this.minRolls = minRolls;
            return this;
        }

        public Builder maxRolls(Integer maxRolls) {
            this.maxRolls = maxRolls;
            return this;
        }

        public Builder spawnProbability(Float spawnProbability) {
            this.spawnProbability = spawnProbability;
            return this;
        }

        public OreBuilder build() {
            return new OreBuilder(generateTexture,
                    maxOreLayerColorDiff,
                    oreTexture,
                    rockTexture,
                    retrogen,
                    useGetDrops,
                    minRolls,
                    maxRolls,
                    spawnProbability);
        }

    }

}
