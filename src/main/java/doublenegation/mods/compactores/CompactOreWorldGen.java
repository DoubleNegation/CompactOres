package doublenegation.mods.compactores;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompactOreWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void init(List<CompactOre> ores) {
        Set<CompactOre> experimentalGeneratorOres = new HashSet<>();
        for(CompactOre ore : ores) {
            if(ore.getBaseBlock() == null) continue; // invalid block specified - can not generate that
            if(ore.isExperimentalGenerator()) {
                experimentalGeneratorOres.add(ore);
                continue;
            }
            LOGGER.warn("Using the non-experimental world generator is not possible anymore. {} will not generate.", ore.getSerializedName());
        }
        if(experimentalGeneratorOres.size() > 0) {
            ExperimentalWorldGen.init(experimentalGeneratorOres);
        }
    }
    
}
