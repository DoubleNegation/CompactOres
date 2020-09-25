package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeGenerationSettings;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.placement.ConfiguredPlacement;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.SimplePlacement;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompactOreWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();

//    public static void init(List<CompactOre> ores) {
//        Map<Float, Set<CompactOre>> normalGeneratingOresByProbability = new HashMap<>();
//        Map<Float, Set<CompactOre>> lateGeneratingOresByProbability = new HashMap<>();
//        for(CompactOre ore : ores) {
//            if(ore.getBaseBlock() == null) continue; // invalid block specified - can not generate that
//            Map<Float, Set<CompactOre>> m = ore.isLateGeneration() ? lateGeneratingOresByProbability : normalGeneratingOresByProbability;
//            if(!m.containsKey(ore.getSpawnProbability())) {
//                m.put(ore.getSpawnProbability(), new HashSet<>());
//            }
//            m.get(ore.getSpawnProbability()).add(ore);
//        }
//        Set<ConfiguredFeature<?, ?>> normalGeneratingConfiguredFeatures =
//                normalGeneratingOresByProbability.keySet().stream()
//                        .map(prob -> make(prob, normalGeneratingOresByProbability.get(prob)))
//                        .collect(Collectors.toSet());
//        Set<ConfiguredFeature<?, ?>> lateGeneratingConfiguredFeatures =
//                lateGeneratingOresByProbability.keySet().stream()
//                        .map(prob -> make(prob, lateGeneratingOresByProbability.get(prob)))
//                        .collect(Collectors.toSet());
//        LOGGER.info("Registering " + (normalGeneratingConfiguredFeatures.size() + lateGeneratingConfiguredFeatures.size()) +
//                " world generation features (" + normalGeneratingConfiguredFeatures.size() + " normal, " +
//                lateGeneratingConfiguredFeatures.size() + " late)");
//        /*for(Biome biome : ForgeRegistries.BIOMES) {
//            normalGeneratingConfiguredFeatures.forEach(
//                    feature -> biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature));
//            lateGeneratingConfiguredFeatures.forEach(
//                    feature -> biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, feature));
//        }*/
//        AtomicInteger cnt = new AtomicInteger(0);
//        normalGeneratingConfiguredFeatures.forEach(feature -> Registry.register(WorldGenRegistries.field_243653_e,
//                new ResourceLocation(CompactOres.MODID, "compactores_feature_" + cnt.getAndIncrement()), feature));
//        lateGeneratingConfiguredFeatures.forEach(feature -> Registry.register(WorldGenRegistries.field_243653_e,
//                new ResourceLocation(CompactOres.MODID, "compactores_feature_" + cnt.getAndIncrement()), feature));
//        for(Biome biome : ForgeRegistries.BIOMES) {
//            normalGeneratingConfiguredFeatures.forEach(feature -> register(biome, GenerationStage.Decoration.UNDERGROUND_ORES, feature));
//            lateGeneratingConfiguredFeatures.forEach(feature -> register(biome, GenerationStage.Decoration.UNDERGROUND_DECORATION, feature));
//        }
//        /*AtomicInteger cnt = new AtomicInteger(0);// do this block if it doesn't work without
//        normalGeneratingConfiguredFeatures.forEach(
//                feature -> Registry.register(WorldGenRegistries.field_243653_e,
//                        new ResourceLocation(CompactOres.MODID, "cf" + (cnt.getAndIncrement())),
//                        feature));
//        lateGeneratingConfiguredFeatures.forEach(
//                feature -> Registry.register(WorldGenRegistries.field_243653_e,
//                        new ResourceLocation(CompactOres.MODID, "cf" + (cnt.getAndIncrement())),
//                        feature));
//        for(Map.Entry<RegistryKey<Biome>, Biome> biome : WorldGenRegistries.field_243657_i.getEntries()) {
//            normalGeneratingConfiguredFeatures.forEach(
//                    feature -> register(biome.getValue(), GenerationStage.Decoration.UNDERGROUND_ORES, feature));
//            lateGeneratingConfiguredFeatures.forEach(
//                    feature -> register(biome.getValue(), GenerationStage.Decoration.UNDERGROUND_DECORATION, feature));
//        }*/
//    }
//
//    private static ConfiguredFeature<?, ?> make(float prob, Set<CompactOre> ores) {
//        /*return new ConfiguredFeature<>(
//                CompactOres.MULTI_REPLACE_BLOCK.get(),
//                new MultiReplaceBlockConfig(
//                        ores.stream().collect(Collectors.toMap(
//                                ore -> ore.getBaseBlock().getDefaultState(),
//                                ore -> ore.getCompactOreBlock().getDefaultState())))
//        ).withPlacement(new ConfiguredPlacement<>(
//                CompactOres.ALL_WITH_PROBABILITY.get(),
//                new ProbabilityConfig(prob)
//        ));*/
//        return CompactOres.MULTI_REPLACE_BLOCK.get().withConfiguration(new MultiReplaceBlockConfig(
//                ores.stream().collect(Collectors.toMap(
//                        ore -> ore.getBaseBlock().getDefaultState(),
//                        ore -> ore.getCompactOreBlock().getDefaultState()
//                ))
//        )).withPlacement(CompactOres.ALL_WITH_PROBABILITY.get().configure(new ProbabilityConfig(prob)));
//    }
//    
//    /** This seems like a super hacky solution but as far as I can tell, there is no better one yet. */
//    /*private static void register(Biome biome, GenerationStage.Decoration stage, ConfiguredFeature<?, ?> feature) {
//        List<List<Supplier<ConfiguredFeature<?, ?>>>> features = biome.func_242440_e().func_242498_c();
//        if(features instanceof ImmutableList<?> && (features.size() <= stage.ordinal() || features.get(stage.ordinal()) instanceof ImmutableList<?>)) {
//            features = new ArrayList<>(features);
//            ObfuscationReflectionHelper.setPrivateValue(BiomeGenerationSettings.class, biome.func_242440_e(), features, "field_242484_f");
//        }
//        while(features.size() <= stage.ordinal()) {
//            features.add(new ArrayList<>());
//        }
//        if(features.get(stage.ordinal()) instanceof ImmutableList) {
//            features.set(stage.ordinal(), new ArrayList<>(features.get(stage.ordinal())));
//        }
//        features.get(stage.ordinal()).add(() -> feature);
//    }*/
//    private static void register(Biome biome, GenerationStage.Decoration stage, ConfiguredFeature<?, ?> feature) {
//        List<List<Supplier<ConfiguredFeature<?, ?>>>> features = biome.func_242440_e().func_242498_c();
//        features = new ArrayList<>(features);
//        ObfuscationReflectionHelper.setPrivateValue(BiomeGenerationSettings.class, biome.func_242440_e(), features, "field_242484_f");
//        while(features.size() <= stage.ordinal()) {
//            features.add(new ArrayList<>());
//        }
//        features.set(stage.ordinal(), new ArrayList<>(features.get(stage.ordinal())));
//        features.get(stage.ordinal()).add(() -> feature);
//    }
//
//    public static class ProbabilityConfig implements IPlacementConfig {
//        public static final Codec<ProbabilityConfig> codec = Codec.FLOAT.fieldOf("probability")
//                .xmap(ProbabilityConfig::new, config -> config.probability).codec();
//        public final float probability;
//        public ProbabilityConfig(float probability) {
//            this.probability = probability;
//        }
//    }
//
//    public static class AllWithProbability extends SimplePlacement<ProbabilityConfig> {
//        public AllWithProbability(Codec<ProbabilityConfig> p_i51362_1_) {
//            super(p_i51362_1_);
//        }
//        @Override
//        protected Stream<BlockPos> getPositions(Random random, ProbabilityConfig probabilityConfig, BlockPos blockPos) {
//            Stream.Builder<BlockPos> builder = Stream.builder();
//            for(int x = 0; x < 16; x++) {
//                for(int z = 0; z < 16; z++) {
//                    for(int y = 0; y < 256; y++) {
//                        if(random.nextFloat() <= probabilityConfig.probability) {
//                            builder.add(blockPos.add(x, y, z));
//                        }
//                    }
//                }
//            }
//            return builder.build();
//        }
//    }
//
//    public static class MultiReplaceBlockConfig implements IFeatureConfig {
//        public static final Codec<MultiReplaceBlockConfig> codec =
//                Codec.unboundedMap(BlockState.BLOCKSTATE_CODEC, BlockState.BLOCKSTATE_CODEC).fieldOf("replacementMap")
//                        .xmap(MultiReplaceBlockConfig::new, config -> config.replacementMap).codec();
//        public final Map<BlockState, BlockState> replacementMap;
//        public MultiReplaceBlockConfig(Map<BlockState, BlockState> replacementMap) {
//            this.replacementMap = Collections.unmodifiableMap(replacementMap);
//        }
//    }
//
//    public static class MultiReplaceBlockFeature extends Feature<MultiReplaceBlockConfig> {
//        public MultiReplaceBlockFeature(Codec<MultiReplaceBlockConfig> arg0) {
//            super(arg0);
//        }
//        @Override
//        public boolean func_241855_a(ISeedReader worldIn, ChunkGenerator generator, Random rand, BlockPos pos, MultiReplaceBlockConfig config) {
//            if(config.replacementMap.containsKey(worldIn.getBlockState(pos))) {
//                worldIn.setBlockState(pos, config.replacementMap.get(worldIn.getBlockState(pos)), 2);
//            }
//            return true;
//        }
//    }

}
