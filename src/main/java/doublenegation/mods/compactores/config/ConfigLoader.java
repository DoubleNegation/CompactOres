package doublenegation.mods.compactores.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.ImmutableList;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOreTexture;
import doublenegation.mods.compactores.Utils;
import net.minecraft.util.ResourceLocation;
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
import java.util.function.BiFunction;

public class ConfigLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    public static List<CompactOre> loadOres() {

        LOGGER.info("Loading compactores configuration...");

        ConfigFileManager cfm = new ConfigFileManager();

        try {

            cfm.load();
            ConfigFile globalDefinition = null;
            ConfigFile globalCustomization = null;
            Map<ResourceLocation, ConfigFile> definitions = new HashMap<>();
            Map<ResourceLocation, ConfigFile> customizations = new HashMap<>();

            for (FileConfig cfg : cfm.getDefinitionConfigs()) {
                ConfigFile f = new ConfigFile(cfg, ConfigFile.Type.DEFINITION);
                if (f.hasGlobalConfig()) {
                    if (globalDefinition == null) {
                        globalDefinition = f;
                    } else {
                        LOGGER.fatal("Multiple global definition configs found:");
                        LOGGER.fatal("\t" + globalDefinition.getType().getDirname() + "/" + globalDefinition.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\t" + f.getType().getDirname() + "/" + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Multiple global definition configs found - aborting!");
                    }
                }
                for (ResourceLocation ore : f.getOres()) {
                    if (!definitions.containsKey(ore)) {
                        definitions.put(ore, f);
                    } else {
                        ConfigFile g = definitions.get(ore);
                        LOGGER.fatal("Ore " + ore + " is defined multiple times:");
                        LOGGER.fatal("\tin " + g.getType().getDirname() + "/" + g.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\tin " + f.getType().getDirname() + "/" + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Ore " + ore + " is defined multiple times - aborting!");
                    }
                }
            }

            for (FileConfig cfg : cfm.getCustomizationConfigs()) {
                ConfigFile f = new ConfigFile(cfg, ConfigFile.Type.CUSTOMIZATION);
                if (f.hasGlobalConfig()) {
                    if (globalCustomization == null) {
                        globalCustomization = f;
                    } else {
                        LOGGER.fatal("Multiple global customizations configs found:");
                        LOGGER.fatal("\t" + globalCustomization.getType().getDirname() + "/" + globalCustomization.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\t" + f.getType().getDirname() + "/" + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Multiple global customization configs found - aborting!");
                    }
                }
                for (ResourceLocation ore : f.getOres()) {
                    if (!definitions.containsKey(ore)) {
                        LOGGER.warn("Customization config found for undefined ore " + ore + " - ignoring!");
                        continue;
                    }
                    if (!customizations.containsKey(ore)) {
                        customizations.put(ore, f);
                    } else {
                        ConfigFile g = customizations.get(ore);
                        LOGGER.fatal("Ore " + ore + " is defined customized times:");
                        LOGGER.fatal("\tin " + g.getType().getDirname() + "/" + g.getFilenameNamespace() + ".toml");
                        LOGGER.fatal("\tin " + f.getType().getDirname() + "/" + f.getFilenameNamespace() + ".toml");
                        throw new IllegalStateException("Ore " + ore + " is customized multiple times - aborting!");
                    }
                }
            }

            if (globalDefinition != null) {
                Optional.ofNullable(globalDefinition.getGlobalConfigValue("generateTexture"))
                        .filter(v -> v instanceof Boolean).ifPresent(v -> OreBuilder.setGlobalGenerateTexture((boolean) v));
                Optional.ofNullable(globalDefinition.getGlobalConfigValue("maxOreLayerColorDiff"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMaxOreLayerColorDiff((int) v));
                Optional.ofNullable(globalDefinition.getGlobalConfigValue("redrawOreBase"))
                        .filter(v -> v instanceof Boolean).ifPresent(v -> CompactOreTexture.setRedrawOreBase((boolean) v));
            }
            if (globalCustomization != null) {
                Optional.ofNullable(globalCustomization.getGlobalConfigValue("minRolls"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMinRolls((int) v));
                Optional.ofNullable(globalCustomization.getGlobalConfigValue("maxRolls"))
                        .filter(v -> v instanceof Integer).ifPresent(v -> OreBuilder.setGlobalMaxRolls((int) v));
                Optional.ofNullable(globalCustomization.getGlobalConfigValue("spawnProbability"))
                        .filter(v -> v instanceof Number).ifPresent(v -> OreBuilder.setGlobalSpawnProbability((float) (double) v));
            }

            OreBuilderFactoryProvider obfp = new OreBuilderFactoryProvider();
            List<CompactOre> ores = new ArrayList<>();
            for (ResourceLocation orename : definitions.keySet()) {
                OreConfigHolder och = new OreConfigHolder(orename, obfp);
                och.setDefinitionConfig(definitions.get(orename));
                if (customizations.containsKey(orename)) {
                    och.setCustomizationConfig(customizations.get(orename));
                } else {
                    LOGGER.warn("No customization config specified for ore " + orename +
                            " - if you do not intend to customize the ore, it is recommended to specify an empty customization block anyways");
                }
                try {
                    ores.add(och.buildOre());
                } catch (RuntimeException ex) {
                    LOGGER.warn("Failed to load ore " + orename + ": " + ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            List<CompactOre> enabledOres = new ArrayList<>();
            Set<String> activeOreMods = new HashSet<>(), inactiveOreMods = new HashSet<>();
            ModList modList = ModList.get();
            for (CompactOre ore : ores) {
                String modid = ore.getBaseBlockRegistryName().getNamespace();
                if (modList.isLoaded(modid)) {
                    enabledOres.add(ore);
                    activeOreMods.add(modid);
                } else {
                    inactiveOreMods.add(modid);
                }
            }

            LOGGER.info("Successfully loaded " + ores.size() + " compact ores for a total of " + (activeOreMods.size() + inactiveOreMods.size()) + " mods.");
            LOGGER.info("\t" + enabledOres.size() + " ores for " + activeOreMods.size() + " mods are active.");
            LOGGER.info("\t" + (ores.size() - enabledOres.size()) + " ores for " + inactiveOreMods.size() + " mods will not be enabled because their mod is not loaded.");

            enabledOres.sort(CompactOre::compareTo);
            enabledOres.forEach(o -> LOGGER.debug(o.getBaseBlockRegistryName()));

            return enabledOres;

        } catch(Exception e) {

            cfm.handleConfigLoadingFailed(e);
            // return two dummy ores
            // at least two ores are required for anything to function (block states require it)
            final ResourceLocation stone = new ResourceLocation("minecraft", "stone");
            final ResourceLocation dirt = new ResourceLocation("minecraft", "dirt");
            final ResourceLocation stoneTex = new ResourceLocation("minecraft", "block/stone");
            return new ArrayList<>(ImmutableList.of(
                    new CompactOre(
                            /*          baseBlockLoc */ stone,
                            /*              minRolls */ 1,
                            /*              maxRolls */ 1,
                            /*        baseOreTexture */ stoneTex,
                            /* baseUnderlyingTexture */ stoneTex,
                            /*      spawnProbability */ 0,
                            /*  maxOreLayerColorDiff */ -1,
                            /*        lateGeneration */ false,
                            /*       generateTexture */ true),
                    new CompactOre(
                            /*          baseBlockLoc */ dirt,  // can't be the same block or world gen will crash
                            /*              minRolls */ 1,
                            /*              maxRolls */ 1,
                            /*        baseOreTexture */ stoneTex,
                            /* baseUnderlyingTexture */ stoneTex,
                            /*      spawnProbability */ 0,
                            /*  maxOreLayerColorDiff */ -1,
                            /*        lateGeneration */ false,
                            /*       generateTexture */ true)));

        }

    }

    private static class OreBuilderFactoryProvider implements BiFunction<ConfigFile, ConfigFile, OreBuilder> {

        private Map<ConfigFile, Map<ConfigFile, OreBuilder.Factory>> factories = new HashMap<>();

        @Override
        public synchronized OreBuilder apply(ConfigFile definitionConfig, ConfigFile customizationConfig) {
            if(!factories.containsKey(definitionConfig)) {
                factories.put(definitionConfig, new HashMap<>());
            }
            Map<ConfigFile, OreBuilder.Factory> factoryMap = factories.get(definitionConfig);
            if(factoryMap.containsKey(customizationConfig)) {
                return factoryMap.get(customizationConfig).create();
            } else {
                OreBuilder.Factory fact = createFactory(definitionConfig, customizationConfig);
                factoryMap.put(customizationConfig, fact);
                return fact.create();
            }
        }

        private OreBuilder.Factory createFactory(ConfigFile definitionConfig, ConfigFile customizationConfig) {
            OreBuilder.Factory fact = OreBuilder.Factory.createFactory();
            if(definitionConfig.hasLocalConfig()) {
                fact.generateTexture(definitionConfig.getLocalConfigValue("generateTexture"))
                        .maxOreLayerColorDiff(definitionConfig.getLocalConfigValue("maxOreLayerColorDiff"))
                        .oreTexture(Utils.parseResourceLocationExtra(definitionConfig.getLocalConfigValue("oreTexture"), definitionConfig.getFilenameNamespace()))
                        .rockTexture(Utils.parseResourceLocationExtra(definitionConfig.getLocalConfigValue("rockTexture"), definitionConfig.getFilenameNamespace()))
                        .lateGeneration(definitionConfig.getLocalConfigValue("lateGeneration"));
            }
            if(customizationConfig != null && customizationConfig.hasLocalConfig()) {
                fact.minRolls(customizationConfig.getLocalConfigValue("minRolls"))
                        .maxRolls(customizationConfig.getLocalConfigValue("maxRolls"))
                        .spawnProbability(customizationConfig.getLocalConfigValue("spawnProbability"));
            }
            return fact;
        }

    }

}
