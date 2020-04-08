package doublenegation.mods.compactores.config;

import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.Utils;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;
import java.util.function.BiFunction;

public class OreConfigHolder {

    private ResourceLocation oreName;
    private ConfigFile definitionConfig;
    private ConfigFile customizationConfig;
    private BiFunction<ConfigFile, ConfigFile, OreBuilder> oreBuilderProvider;

    public OreConfigHolder(ResourceLocation oreName, BiFunction<ConfigFile, ConfigFile, OreBuilder> oreBuilderProvider) {
        this.oreName = oreName;
        this.oreBuilderProvider = oreBuilderProvider;
    }

    public synchronized void setDefinitionConfig(ConfigFile definitionConfig) {
        if(this.definitionConfig != null) {
            throw new IllegalStateException("Definition config already set");
        }
        if(definitionConfig == null) {
            throw new IllegalArgumentException("config is null!");
        }
        if(definitionConfig.getType() != ConfigFile.Type.DEFINITION) {
            throw new IllegalArgumentException("Config file is not a definition config");
        }
        if(!definitionConfig.containsOre(oreName)) {
            throw new IllegalArgumentException("Config does not support ore (" + oreName + ")");
        }
        this.definitionConfig = definitionConfig;
    }

    public synchronized void setCustomizationConfig(ConfigFile customizationConfig) {
        if(this.customizationConfig != null) {
            throw new IllegalStateException("Customization config already set");
        }
        if(customizationConfig == null) {
            throw new IllegalArgumentException("config is null!");
        }
        if(customizationConfig.getType() != ConfigFile.Type.CUSTOMIZATION) {
            throw new IllegalArgumentException("Config file is not a customization config");
        }
        if(!customizationConfig.containsOre(oreName)) {
            throw new IllegalArgumentException("Config does not support ore (" + oreName + ")");
        }
        this.customizationConfig = customizationConfig;
    }

    public CompactOre buildOre() {
        if(definitionConfig == null) {
            throw new IllegalStateException("Definition config is not set");
        }
        // customization config has no required fields - can be null (global values will be inherited)
        return oreBuilderProvider.apply(definitionConfig, customizationConfig)
                // definition
                .generateTexture(definitionConfig.getOreConfigValue(oreName, "generateTexture"))
                .maxOreLayerColorDiff(definitionConfig.getOreConfigValue(oreName, "maxOreLayerColorDiff"))
                .oreTexture(Utils.parseResourceLocationExtra(definitionConfig.getOreConfigValue(oreName, "oreTexture"), definitionConfig.getFilenameNamespace()))
                .rockTexture(Utils.parseResourceLocationExtra(definitionConfig.getOreConfigValue(oreName, "rockTexture"), definitionConfig.getFilenameNamespace()))
                .lateGeneration(definitionConfig.getOreConfigValue(oreName, "lateGeneration"))
                .useGetDrops(definitionConfig.getOreConfigValue(oreName, "useGetDrops"))
                // customization
                .minRolls((Integer) Optional.ofNullable(customizationConfig).map(c -> c.getOreConfigValue(oreName, "minRolls")).orElse(null))
                .maxRolls((Integer) Optional.ofNullable(customizationConfig).map(c -> c.getOreConfigValue(oreName, "maxRolls")).orElse(null))
                .spawnProbability(Optional.ofNullable(customizationConfig).map(c -> c.getOreConfigValue(oreName, "spawnProbability")).map(v -> (float)(double)v).orElse(null))
                // others
                .baseBlock(oreName)
                // build
                .build();
    }

}
