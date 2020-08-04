package doublenegation.mods.compactores.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import doublenegation.mods.compactores.Utils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigFile {

    private static final Logger LOGGER = LogManager.getLogger();

    private Config globalConfig;
    private Config localConfig;
    private final Map<ResourceLocation, Config> oreConfigs = new HashMap<>();
    private final String filenameNamespace;

    public ConfigFile(FileConfig config) {
        String filename = config.getFile().getName();
        filenameNamespace = filename.substring(0, filename.length() - ".toml".length());
        for(String key : config.valueMap().keySet()) {
            Object o = config.get(key);
            if(!(o instanceof Config)) {
                LOGGER.warn("Config file " + filename + " contains non-Config root key \"" + key + "\"");
                continue;
            }
            Config c = (Config)o;
            if(key.equals("!global")) {
                globalConfig = c;
            } else if(key.equals("!local")) {
                localConfig = c;
            } else {
                try {
                    ResourceLocation blockName = Utils.parseResourceLocationExtra(key, filenameNamespace);
                    oreConfigs.put(blockName, c);
                } catch(ResourceLocationException e) {
                    LOGGER.warn("Config file " + filename + " contains illegal resource name \"" + key + "\": " + e.getMessage());
                }
            }
        }
    }

    public boolean hasGlobalConfig() {
        return globalConfig != null;
    }

    public <T> T getGlobalConfigValue(String key) {
        return globalConfig.get(key);
    }

    public boolean hasLocalConfig() {
        return localConfig != null;
    }

    public <T> T getLocalConfigValue(String key) {
        return localConfig.get(key);
    }

    public Set<ResourceLocation> getOres() {
        return oreConfigs.keySet();
    }

    public boolean containsOre(ResourceLocation loc) {
        return oreConfigs.containsKey(loc);
    }

    public <T> T getOreConfigValue(ResourceLocation ore, String key) {
        return oreConfigs.get(ore).get(key);
    }

    public String getFilenameNamespace() {
        return filenameNamespace;
    }

}
