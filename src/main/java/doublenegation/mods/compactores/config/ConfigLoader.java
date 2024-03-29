package doublenegation.mods.compactores.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ConfigLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    public static List<CompactOre> loadOres() {

        LOGGER.info("Loading compactores configuration...");

        ConfigFileManager cfm = new ConfigFileManager();

        try {

            cfm.load();
            ConfigFile globalConfig = null;
            Map<ResourceLocation, ConfigFile> configs = new HashMap<>();

            for (FileConfig cfg : cfm.getConfigs()) {
                ConfigFile f = new ConfigFile(cfg);
                if (f.hasGlobalConfig()) {
                    if (globalConfig == null) {
                        globalConfig = f;
                    } else {
                        LOGGER.fatal("Multiple global configs found:");
                        LOGGER.fatal("\t" + globalConfig.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\t" + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Multiple global configs found - aborting!");
                    }
                }
                for (ResourceLocation ore : f.getOres()) {
                    if (!configs.containsKey(ore)) {
                        configs.put(ore, f);
                    } else {
                        ConfigFile g = configs.get(ore);
                        LOGGER.fatal("Ore " + ore + " is defined multiple times:");
                        LOGGER.fatal("\tin " + g.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\tin " + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Ore " + ore + " is defined multiple times - aborting!");
                    }
                }
            }

            if (globalConfig != null) {
                Optional.ofNullable(globalConfig.getGlobalConfigValue("generateTexture"))
                        .filter(v -> v instanceof Boolean).ifPresent(v -> OreBuilder.setGlobalGenerateTexture((boolean) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("maxOreLayerColorDiff"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMaxOreLayerColorDiff((int) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("retrogen"))
                        .filter(v -> v instanceof Boolean).ifPresent(v -> OreBuilder.setGlobalRetrogen((boolean) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("minRolls"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMinRolls((int) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("maxRolls"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMaxRolls((int) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("spawnProbability"))
                        .filter(v -> v instanceof Number).ifPresent(v -> OreBuilder.setGlobalSpawnProbability((float) (double) v));
                Optional.ofNullable(globalConfig.getGlobalConfigValue("breakTimeMultiplier"))
                        .filter(v -> v instanceof Number).ifPresent(v -> OreBuilder.setGlobalBreakTimeMultiplier((float) (double) v));
            }

            OreBuilderBuilderProvider obbp = new OreBuilderBuilderProvider();
            List<CompactOre> enabledOres = new ArrayList<>();
            Set<String> activeOreMods = new HashSet<>(), inactiveOreMods = new HashSet<>();
            int disabledOres = 0;
            ModList modList = ModList.get();
            for (ResourceLocation orename : configs.keySet()) {
                OreConfigHolder och = new OreConfigHolder(orename, obbp);
                och.setConfig(configs.get(orename));
                if(modList.isLoaded(orename.getNamespace())) {
                    try {
                        enabledOres.add(och.buildOre());
                    } catch (RuntimeException ex) {
                        LOGGER.warn("Failed to load ore " + orename + ": " + ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    activeOreMods.add(orename.getNamespace());
                } else {
                    disabledOres++;
                    inactiveOreMods.add(orename.getNamespace());
                }
            }

            LOGGER.info("Successfully loaded " + (enabledOres.size() + disabledOres) + " compact ores for a total of " + (activeOreMods.size() + inactiveOreMods.size()) + " mods.");
            LOGGER.info("\t" + enabledOres.size() + " ores for " + activeOreMods.size() + " mods are active.");
            LOGGER.info("\t" + disabledOres + " ores for " + inactiveOreMods.size() + " mods will not be enabled because their mod is not loaded.");

            enabledOres.sort(CompactOre::compareTo);
            enabledOres.forEach(o -> LOGGER.debug(o.getBaseBlockRegistryName()));

            return enabledOres;

        } catch(Exception e) {

            cfm.handleConfigLoadingFailed(e);
            return new ArrayList<>(0);

        }

    }

    private static class OreBuilderBuilderProvider implements Function<ConfigFile, OreBuilder> {

        private final Map<ConfigFile, OreBuilder.Builder> builders = new HashMap<>();

        @Override
        public synchronized OreBuilder apply(ConfigFile config) {
            if(builders.containsKey(config)) {
                return builders.get(config).build();
            } else {
                OreBuilder.Builder builder = createBuilder(config);
                builders.put(config, builder);
                return builder.build();
            }
        }

        private OreBuilder.Builder createBuilder(ConfigFile config) {
            OreBuilder.Builder builder = OreBuilder.Builder.create();
            if(config.hasLocalConfig()) {
                builder.generateTexture(config.getLocalConfigValue("generateTexture"))
                        .maxOreLayerColorDiff(config.getLocalConfigValue("maxOreLayerColorDiff"))
                        .oreTexture(Utils.parseResourceLocationExtra(config.getLocalConfigValue("oreTexture"), config.getFilenameNamespace()))
                        .rockTexture(Utils.parseResourceLocationExtra(config.getLocalConfigValue("rockTexture"), config.getFilenameNamespace()))
                        .retrogen(config.getLocalConfigValue("retrogen"))
                        .useGetDrops(config.getLocalConfigValue("useGetDrops"))
                        .minRolls(config.getLocalConfigValue("minRolls"))
                        .maxRolls(config.getLocalConfigValue("maxRolls"))
                        .spawnProbability(Optional.ofNullable(config.<Double>getLocalConfigValue("spawnProbability")).map(p -> (float)(double)p).orElse(null))
                        .breakTimeMultiplier(Optional.ofNullable(config.<Double>getLocalConfigValue("breakTimeMultiplier")).map(p -> (float)(double)p).orElse(null));
            }
            return builder;
        }

    }

}
