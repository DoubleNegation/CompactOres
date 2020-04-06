package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.placement.ConfiguredPlacement;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.SimplePlacement;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompactOreWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void init(List<CompactOre> ores) {
        Map<Float, Set<CompactOre>> normalGeneratingOresByProbability = new HashMap<>();
        Map<Float, Set<CompactOre>> lateGeneratingOresByProbability = new HashMap<>();
        for(CompactOre ore : ores) {
            if(!ore.isReal()) continue; // prevent feature for dummy ore with 0% chance
            Map<Float, Set<CompactOre>> m = ore.isLateGeneration() ? lateGeneratingOresByProbability : normalGeneratingOresByProbability;
            if(!m.containsKey(ore.getSpawnProbability())) {
                m.put(ore.getSpawnProbability(), new HashSet<>());
            }
            m.get(ore.getSpawnProbability()).add(ore);
        }
        Set<ConfiguredFeature<?, ?>> normalGeneratingConfiguredFeatures =
                normalGeneratingOresByProbability.keySet().stream()
                        .map(prob -> make(prob, normalGeneratingOresByProbability.get(prob)))
                        .collect(Collectors.toSet());
        Set<ConfiguredFeature<?, ?>> lateGeneratingConfiguredFeatures =
                lateGeneratingOresByProbability.keySet().stream()
                        .map(prob -> make(prob, lateGeneratingOresByProbability.get(prob)))
                        .collect(Collectors.toSet());
        LOGGER.info("Registering " + (normalGeneratingConfiguredFeatures.size() + lateGeneratingConfiguredFeatures.size()) +
                " world generation features (" + normalGeneratingConfiguredFeatures.size() + " normal, " +
                lateGeneratingConfiguredFeatures.size() + " late)");
        for(Biome biome : ForgeRegistries.BIOMES) {
            normalGeneratingConfiguredFeatures.forEach(
                    feature -> biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature));
            lateGeneratingConfiguredFeatures.forEach(
                    feature -> biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, feature));
        }
    }

    private static ConfiguredFeature<?, ?> make(float prob, Set<CompactOre> ores) {
        return new ConfiguredFeature<>(
                CompactOres.MULTI_REPLACE_BLOCK.get(),
                new MultiReplaceBlockConfig(
                        ores.stream().collect(Collectors.toMap(
                                ore -> ore.getBaseBlock().getDefaultState(),
                                ore -> CompactOres.COMPACT_ORE.get().getDefaultState().with(CompactOreBlock.ORE_PROPERTY, ore))))
        ).func_227228_a_(new ConfiguredPlacement<>(
                CompactOres.ALL_WITH_PROBABILITY.get(),
                new ProbabilityConfig(prob)
        ));
    }

    public static class ProbabilityConfig implements IPlacementConfig {
        public final float probability;
        public ProbabilityConfig(float probability) {
            this.probability = probability;
        }
        @Override
        public <T> Dynamic<T> serialize(DynamicOps<T> dynamicOps) {
            return new Dynamic<>(dynamicOps, dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("probability"), dynamicOps.createFloat(probability))));
        }
        public static ProbabilityConfig deserialize(Dynamic<?> dynamic) {
            return new ProbabilityConfig(dynamic.get("probability").asFloat(.1f));
        }
    }

    public static class AllWithProbability extends SimplePlacement<ProbabilityConfig> {
        public AllWithProbability(Function<Dynamic<?>, ? extends ProbabilityConfig> p_i51362_1_) {
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
        public final Map<BlockState, BlockState> replacementMap;
        public MultiReplaceBlockConfig(Map<BlockState, BlockState> replacementMap) {
            this.replacementMap = Collections.unmodifiableMap(replacementMap);
        }
        public <T> Dynamic<T> serialize(DynamicOps<T> ops) {
            Map<T, T> serMap = new HashMap<>();
            for(BlockState key : replacementMap.keySet()) {
                serMap.put(BlockState.serialize(ops, key).getValue(), BlockState.serialize(ops, replacementMap.get(key)).getValue());
            }
            return new Dynamic<>(ops, ops.createMap(ImmutableMap.of(ops.createString("replacements"), ops.createMap(serMap))));
        }
        public static <T> MultiReplaceBlockConfig deserialize(Dynamic<T> d) {
            return new MultiReplaceBlockConfig(d.get("replacements").asMap(BlockState::deserialize, BlockState::deserialize));
        }
    }

    public static class MultiReplaceBlockFeature extends Feature<MultiReplaceBlockConfig> {
        public MultiReplaceBlockFeature(Function<Dynamic<?>, ? extends MultiReplaceBlockConfig> arg0) {
            super(arg0);
        }
        @Override
        public boolean place(IWorld worldIn, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, MultiReplaceBlockConfig config) {
            if(config.replacementMap.containsKey(worldIn.getBlockState(pos))) {
                worldIn.setBlockState(pos, config.replacementMap.get(worldIn.getBlockState(pos)), 2);
            }
            return true;
        }
    }

}
