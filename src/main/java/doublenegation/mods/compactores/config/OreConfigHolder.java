package doublenegation.mods.compactores.config;

import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.Utils;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

public class OreConfigHolder {

    private final ResourceLocation oreName;
    private ConfigFile config;
    private final Function<ConfigFile, OreBuilder> oreBuilderProvider;

    public OreConfigHolder(ResourceLocation oreName, Function<ConfigFile, OreBuilder> oreBuilderProvider) {
        this.oreName = oreName;
        this.oreBuilderProvider = oreBuilderProvider;
    }

    public synchronized void setConfig(ConfigFile config) {
        if(this.config != null) {
            throw new IllegalStateException("Config already set");
        }
        if(config == null) {
            throw new IllegalArgumentException("config is null!");
        }
        if(!config.containsOre(oreName)) {
            throw new IllegalArgumentException("Config does not support ore (" + oreName + ")");
        }
        this.config = config;
    }

    public CompactOre buildOre() {
        if(config == null) {
            throw new IllegalStateException("Cconfig is not set");
        }
        // customization config has no required fields - can be null (global values will be inherited)
        return oreBuilderProvider.apply(config)
                // definition
                .generateTexture(config.getOreConfigValue(oreName, "generateTexture"))
                .maxOreLayerColorDiff(config.getOreConfigValue(oreName, "maxOreLayerColorDiff"))
                .oreTexture(Utils.parseResourceLocationExtra(config.getOreConfigValue(oreName, "oreTexture"), config.getFilenameNamespace()))
                .rockTexture(Utils.parseResourceLocationExtra(config.getOreConfigValue(oreName, "rockTexture"), config.getFilenameNamespace()))
                .lateGeneration(config.getOreConfigValue(oreName, "lateGeneration"))
                .useGetDrops(config.getOreConfigValue(oreName, "useGetDrops"))
                // customization
                .minRolls((Integer) Optional.ofNullable(config).map(c -> c.getOreConfigValue(oreName, "minRolls")).orElse(null))
                .maxRolls((Integer) Optional.ofNullable(config).map(c -> c.getOreConfigValue(oreName, "maxRolls")).orElse(null))
                .spawnProbability(Optional.ofNullable(config).map(c -> c.getOreConfigValue(oreName, "spawnProbability")).map(v -> (float)(double)v).orElse(null))
                // others
                .baseBlock(oreName)
                // build
                .build();
    }

}
