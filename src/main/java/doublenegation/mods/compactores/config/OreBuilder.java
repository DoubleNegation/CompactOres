package doublenegation.mods.compactores.config;

import doublenegation.mods.compactores.CompactOre;
import net.minecraft.util.ResourceLocation;

public class OreBuilder {

    // GLOBAL DEFINITION DEFAULTS
    private static boolean G_GENERATETEXTURE = true;
    private static int G_MAXORELAYERCOLORDIFF = 50;
    private static boolean G_EXPERIMENTALGENERATOR = false;

    // GLOBAL CUSTOMIZATION DEFAULTS
    private static int G_MINROLLS = 3;
    private static int G_MAXROLLS = 5;
    private static float G_SPAWNPROBABILITY = .1f;

    // LOCAL DEFINITION DEFAULTS
    private Boolean L_GENERATETEXTURE;
    private Integer L_MAXORELAYERCOLORDIFF;
    private ResourceLocation L_ORETEXTURE;
    private ResourceLocation L_ROCKTEXTURE;
    private boolean L_LATEGENRATION; /* default value = false, defined in Factory */
    private Boolean L_EXPERIMENTALGENERATOR;
    private boolean L_USEGETDROPS; /* default value = false, defined in Factory */

    // LOCAL CUSTOMIZATION DEFAULTS
    private Integer L_MINROLLS;
    private Integer L_MAXROLLS;
    private Float L_SPAWNPROBABILITY;

    // private definition values
    private Boolean generateTexture;
    private Integer maxOreLayerColorDiff;
    private ResourceLocation oreTexture;
    private ResourceLocation rockTexture;
    private Boolean lateGeneration;
    private Boolean experimentalGenerator;
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

    public static void setGlobalExperimentalGenerator(boolean value) {
        G_EXPERIMENTALGENERATOR = value;
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
                       Boolean localLateGeneration,
                       Boolean experimentalGenerator,
                       Boolean localUseGetDrops,
                       Integer localMinRolls,
                       Integer localMaxRolls,
                       Float localSpawnProbability) {
        L_GENERATETEXTURE = localGenerateTexture;
        L_MAXORELAYERCOLORDIFF = localMaxOreLayerColorDiff;
        L_ORETEXTURE = localOreTexture;
        L_ROCKTEXTURE = localRockTexture;
        L_LATEGENRATION = localLateGeneration;
        L_EXPERIMENTALGENERATOR = experimentalGenerator;
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

    public OreBuilder lateGeneration(Boolean lateGeneration) {
        this.lateGeneration = lateGeneration;
        return this;
    }
    
    public OreBuilder experimentalGenerator(Boolean experimentalGenerator) {
        this.experimentalGenerator = experimentalGenerator;
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
        boolean actualLateGeneration = lateGeneration != null ? lateGeneration : L_LATEGENRATION;
        boolean actualExperimentalGenerator = experimentalGenerator != null ? experimentalGenerator : L_EXPERIMENTALGENERATOR != null ? L_EXPERIMENTALGENERATOR : G_EXPERIMENTALGENERATOR;
        boolean actualUseGetDrops = useGetDrops != null ? useGetDrops : L_USEGETDROPS;
        // customization
        int actualMinRolls = minRolls != null ? minRolls : L_MINROLLS != null ? L_MINROLLS : G_MINROLLS;
        int actualMaxRolls = maxRolls != null ? maxRolls : L_MAXROLLS != null ? L_MAXROLLS : G_MAXROLLS;
        float actualSpawnProbability = spawnProbability != null ? spawnProbability : L_SPAWNPROBABILITY != null ? L_SPAWNPROBABILITY : G_SPAWNPROBABILITY;
        return new CompactOre(baseBlock, actualMinRolls, actualMaxRolls, actualOreTexture, actualRockTexture,
                actualSpawnProbability, actualMaxOreLayerColorDiff, actualLateGeneration, actualExperimentalGenerator,
                actualGenerateTexture, actualUseGetDrops);
    }

    public static class Factory {

        // definition
        private Boolean generateTexture;
        private Integer maxOreLayerColorDiff;
        private ResourceLocation oreTexture;
        private ResourceLocation rockTexture;
        private boolean lateGeneration = false;
        private Boolean experimentalGenerator;
        private boolean useGetDrops = false;

        // customization
        private Integer minRolls;
        private Integer maxRolls;
        private Float spawnProbability;

        private Factory() {}

        public static Factory createFactory() {
            return new Factory();
        }

        public Factory generateTexture(Boolean generateTexture) {
            this.generateTexture = generateTexture;
            return this;
        }

        public Factory maxOreLayerColorDiff(Integer maxOreLayerColorDiff) {
            this.maxOreLayerColorDiff = maxOreLayerColorDiff;
            return this;
        }

        public Factory oreTexture(ResourceLocation oreTexture) {
            this.oreTexture = oreTexture;
            return this;
        }

        public Factory rockTexture(ResourceLocation rockTexture) {
            this.rockTexture = rockTexture;
            return this;
        }

        public Factory lateGeneration(Boolean lateGeneration) {
            if(lateGeneration != null) {
                this.lateGeneration = lateGeneration;
            }
            return this;
        }
        
        public Factory experimentalGenerator(Boolean experimentalGenerator) {
            this.experimentalGenerator = experimentalGenerator;
            return this;
        }

        public Factory useGetDrops(Boolean useGetDrops) {
            if(useGetDrops != null) {
                this.useGetDrops = useGetDrops;
            }
            return this;
        }

        public Factory minRolls(Integer minRolls) {
            this.minRolls = minRolls;
            return this;
        }

        public Factory maxRolls(Integer maxRolls) {
            this.maxRolls = maxRolls;
            return this;
        }

        public Factory spawnProbability(Float spawnProbability) {
            this.spawnProbability = spawnProbability;
            return this;
        }

        public OreBuilder create() {
            return new OreBuilder(generateTexture,
                    maxOreLayerColorDiff,
                    oreTexture,
                    rockTexture,
                    lateGeneration,
                    experimentalGenerator,
                    useGetDrops,
                    minRolls,
                    maxRolls,
                    spawnProbability);
        }

    }

}
