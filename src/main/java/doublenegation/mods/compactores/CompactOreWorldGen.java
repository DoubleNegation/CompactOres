package doublenegation.mods.compactores;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.SimplePlacement;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompactOreWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private static Set<ConfiguredFeature<?, ?>> normalFeatures = new HashSet<>();
    private static Set<ConfiguredFeature<?, ?>> lateFeatures = new HashSet<>();

    public static void init(List<CompactOre> ores) {
        Map<Float, Set<CompactOre>> normalGeneratingOresByProbability = new HashMap<>();
        Map<Float, Set<CompactOre>> lateGeneratingOresByProbability = new HashMap<>();
        for(CompactOre ore : ores) {
            if(ore.getBaseBlock() == null) continue; // invalid block specified - can not generate that
            Map<Float, Set<CompactOre>> m = ore.isLateGeneration() ? lateGeneratingOresByProbability : normalGeneratingOresByProbability;
            if(!m.containsKey(ore.getSpawnProbability())) {
                m.put(ore.getSpawnProbability(), new HashSet<>());
            }
            m.get(ore.getSpawnProbability()).add(ore);
        }
        normalFeatures = normalGeneratingOresByProbability.keySet().stream()
                        .map(prob -> make(prob, normalGeneratingOresByProbability.get(prob)))
                        .collect(Collectors.toSet());
        lateFeatures = lateGeneratingOresByProbability.keySet().stream()
                        .map(prob -> make(prob, lateGeneratingOresByProbability.get(prob)))
                        .collect(Collectors.toSet());
        LOGGER.info("Registering " + (normalFeatures.size() + lateFeatures.size()) + " world generation features (" + 
                normalFeatures.size() + " normal, " + lateFeatures.size() + " late)");
    }
    
    public static void register(BiomeLoadingEvent event) {
        // TODO: this probably runs too early to reliably work with other mod's ores. find a better way.
        BiomeGenerationSettingsBuilder generation = event.getGeneration();
        normalFeatures.forEach(feature -> generation.withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature));
        lateFeatures.forEach(feature -> generation.withFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, feature));
    }

    private static ConfiguredFeature<?, ?> make(float prob, Set<CompactOre> ores) {
        return CompactOres.MULTI_REPLACE_BLOCK.get().withConfiguration(new MultiReplaceBlockConfig(
                ores.stream().collect(Collectors.toMap(
                        ore -> ore.getBaseBlock().getDefaultState(),
                        ore -> ore.getCompactOreBlock().getDefaultState()
                ))
        )).withPlacement(CompactOres.ALL_WITH_PROBABILITY.get().configure(new ProbabilityConfig(prob)));
    }
    
    public static class ProbabilityConfig implements IPlacementConfig {
        public static final Codec<ProbabilityConfig> codec = Codec.FLOAT.fieldOf("probability")
                .xmap(ProbabilityConfig::new, config -> config.probability).codec();
        public final float probability;
        public ProbabilityConfig(float probability) {
            this.probability = probability;
        }
    }

    public static class AllWithProbability extends SimplePlacement<ProbabilityConfig> {
        public AllWithProbability(Codec<ProbabilityConfig> p_i51362_1_) {
            super(p_i51362_1_);
        }
        @Override
        protected Stream<BlockPos> getPositions(Random random, ProbabilityConfig probabilityConfig, BlockPos blockPos) {
            Stream.Builder<BlockPos> builder = Stream.builder();
            for(int x = 0; x < 16; x++) {
                for(int z = 0; z < 16; z++) {
                    for(int y = 0; y < 256; y++) {
                        if(random.nextFloat() <= probabilityConfig.probability) {
                            builder.add(blockPos.add(x, y, z));
                        }
                    }
                }
            }
            return builder.build();
        }
    }

    public static class MultiReplaceBlockConfig implements IFeatureConfig {
        public static final Codec<MultiReplaceBlockConfig> codec =
                Codec.unboundedMap(BlockState.CODEC, BlockState.CODEC).fieldOf("replacementMap")
                        .xmap(MultiReplaceBlockConfig::new, config -> config.replacementMap).codec();
        public final Map<BlockState, BlockState> replacementMap;
        public MultiReplaceBlockConfig(Map<BlockState, BlockState> replacementMap) {
            this.replacementMap = Collections.unmodifiableMap(replacementMap);
        }
    }

    public static class MultiReplaceBlockFeature extends Feature<MultiReplaceBlockConfig> {
        public MultiReplaceBlockFeature(Codec<MultiReplaceBlockConfig> arg0) {
            super(arg0);
        }
        @Override
        public boolean func_241855_a(ISeedReader worldIn, ChunkGenerator generator, Random rand, BlockPos pos, MultiReplaceBlockConfig config) {
            if(config.replacementMap.containsKey(worldIn.getBlockState(pos))) {
                worldIn.setBlockState(pos, config.replacementMap.get(worldIn.getBlockState(pos)), 2);
            }
            return true;
        }
    }

}
