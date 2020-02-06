package doublenegation.mods.compactores;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompactOresConfig {

    public static Set<CompactOre> loadConfigs() {
        CompactOres.LOGGER.debug("Loading configs!");
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(CompactOres.MODID + ".toml");
        if(!Files.exists(configFile)) {
            try {
                Files.copy(CompactOresConfig.class.getResourceAsStream("/assets/" + CompactOres.MODID + "/default_config.toml"), configFile);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        FileConfig conf = FileConfig.of(configFile);
        conf.load();
        CompactOres.LOGGER.debug("Config: " + configFile);
        Config global = conf.getOrElse("global", Config.inMemory());
        double globalCompactOreProbability = global.getOrElse("compactOreProbability", .1);
        int globalMinRolls = global.getOrElse("minRolls", 3);
        int globalMaxRolls = global.getOrElse("maxRolls", 5);
        CompactOreTexture.setRedrawOreBase(global.getOrElse("redrawOreBase", false));

        Set<CompactOre> ores = new HashSet<>();
        Map<String, Object> confMap = conf.valueMap();
        CompactOres.LOGGER.debug("Map entries: " + confMap.size());
        for(String s : confMap.keySet()) {
            if(s.equals("global")) continue;
            Object val = confMap.get(s);
            if(val instanceof Config) {
                Config c = (Config)val;
                if(!(hasString(c, "oreTexture") && hasString(c, "rockTexture"))) continue;
                ResourceLocation baseBlock = parseResourceLocation(s);
                ResourceLocation oreTexture = parseResourceLocation(c.get("oreTexture"));
                ResourceLocation rockTexture = parseResourceLocation(c.get("rockTexture"));
                float probability = (float) (double) c.getOrElse("compactOreProbability", globalCompactOreProbability);
                int minRolls = c.getOrElse("minRolls", globalMinRolls);
                int maxRolls = c.getOrElse("maxRolls", globalMaxRolls);
                boolean useGetDrops = c.getOrElse("useGetDrops", false);
                int maxOreLayerColorDiff = c.getOrElse("maxOreLayerColorDiff", 50);
                boolean lateGeneration = c.getOrElse("lateGeneration", false);
                if(ModList.get().isLoaded(baseBlock.getNamespace())) {
                    ores.add(new CompactOre(baseBlock, minRolls, maxRolls, oreTexture, rockTexture, probability, useGetDrops, maxOreLayerColorDiff, lateGeneration));
                } else {
                    CompactOres.LOGGER.info("Not creating a compact ore for " + baseBlock + " because the mod " + baseBlock.getNamespace() + " is not loaded.");
                }
            }
        }
        return ores;
    }

    private static ResourceLocation parseResourceLocation(String str) {
        if(!str.contains(":")) return new ResourceLocation("minecraft", str);
        String namespace = str.split(":")[0];
        String path = str.substring(namespace.length() + 1);
        return new ResourceLocation(namespace, path);
    }

    private static boolean hasString(Config c, String key) {
        return c.contains(key) && (c.get(key) instanceof String);
    }

}
