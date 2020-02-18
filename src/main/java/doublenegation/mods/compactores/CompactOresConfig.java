package doublenegation.mods.compactores;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompactOresConfig {

    public static final CompactOresConfig CONFIG;
    public static final ForgeConfigSpec SPEC;

    public static String ACTIVE_CONFIG_FILE_NAME = CompactOres.MODID + "-common-active-DO-NOT-EDIT-CHANGES-WILL-NOT-PERSIST.toml";

    static {
        final Pair<CompactOresConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CompactOresConfig::new);
        CONFIG = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    public static void prepareConfigFiles() {
        // Copy the default config as the config file if there is no config file yet
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CompactOres.MODID + "-common.toml");
        if(!Files.exists(configFile)) {
            try {
                Files.copy(CompactOresConfig.class.getResourceAsStream("/assets/" + CompactOres.MODID + "/default_config.toml"), configFile);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        // Copy the config file to a new "actual config file" that will be loaded by forge
        Path actualConfigFile = FMLPaths.CONFIGDIR.get().resolve(ACTIVE_CONFIG_FILE_NAME);
        try {
            Files.copy(configFile, actualConfigFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
        // The actual config will be deleted when the game exits.
        actualConfigFile.toFile().deleteOnExit();
    }

    private ForgeConfigSpec.DoubleValue globalCompactOreProbability;
    private ForgeConfigSpec.IntValue globalMinRolls;
    private ForgeConfigSpec.IntValue globalMaxRolls;
    private ForgeConfigSpec.BooleanValue globalRedrawOreBase;
    private ForgeConfigSpec.ConfigValue<List<?>> ores;

    public CompactOresConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("This configuration file has been generated from compactores-common.toml while the game",
                "was launching and will be deleted when the game is quit. Any changes made to this",
                "file will be lost when the game exits. To change the configuration of compact ores,",
                "edit compactores-common.toml.");
        builder.push("global");
        globalCompactOreProbability = builder.defineInRange("compactOreProbability", .1, 0, 1);
        globalMinRolls = builder.defineInRange("minRolls", 3, 0, Integer.MAX_VALUE);
        globalMaxRolls = builder.defineInRange("maxRolls", 5, 0, Integer.MAX_VALUE);
        globalRedrawOreBase = builder.define("redrawOreBase", false);
        builder.pop();
        ores = builder.defineList("ore", new ArrayList<>(), e -> true);
    }

    public List<CompactOre> bake() {
        double globalCompactOreProbability = this.globalCompactOreProbability.get();
        int globalMinRolls = this.globalMinRolls.get();
        int globalMaxRolls = this.globalMaxRolls.get();
        boolean globalRedrawOreBase = this.globalRedrawOreBase.get();
        List<CompactOre> compactOres = new ArrayList<>();
        int notLoaded = 0;
        List<?> ores = this.ores.get();
        for(Object o : ores) {
            if(o instanceof Config) {
                Config c = (Config) o;
                if(!hasString(c, "oreBlock")) {
                    CompactOres.LOGGER.warn("Ore config is missing oreBlock value (" + c + ")");
                    continue;
                }
                if(!hasString(c, "oreTexture")) {
                    CompactOres.LOGGER.warn("Ore config is missing oreTexture value (" + c + ")");
                    continue;
                }
                if(!hasString(c, "rockTexture")) {
                    CompactOres.LOGGER.warn("Ore config is missing rockTexture value (" + c + ")");
                    continue;
                }
                ResourceLocation oreBlock = Utils.parseResourceLocation(c.get("oreBlock"));
                ResourceLocation oreTexture = Utils.parseResourceLocation(c.get("oreTexture"));
                ResourceLocation rockTexture = Utils.parseResourceLocation(c.get("rockTexture"));
                double compactOreProbability = c.getOrElse("compactOreProbability", globalCompactOreProbability);
                int minRolls = c.getOrElse("minRolls", globalMinRolls);
                int maxRolls = c.getOrElse("maxRolls", globalMaxRolls);
                int maxOreLayerColorDiff = c.getOrElse("maxOreLayerColorDiff", 50);
                boolean lateGeneration = c.getOrElse("lateGeneration", false);
                if(ModList.get().isLoaded(oreBlock.getNamespace())) {
                    compactOres.add(new CompactOre(oreBlock, minRolls, maxRolls, oreTexture, rockTexture,
                            (float)compactOreProbability, maxOreLayerColorDiff, lateGeneration));
                } else {
                    notLoaded++;
                }
            } else {
                CompactOres.LOGGER.warn("Found invalid ore configuration (not a Config object) " + o);
            }
        }
        compactOres.sort(CompactOre::compareTo);
        CompactOres.LOGGER.info(compactOres.size() + " compact ores were loaded from configuration and " + notLoaded +
                " further ores were skipped because their mod is not loaded.");
        CompactOreTexture.setRedrawOreBase(globalRedrawOreBase);
        return compactOres;
    }

    private static boolean hasString(Config c, String key) {
        return c.contains(key) && (c.get(key) instanceof String);
    }

}
