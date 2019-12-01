package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.ReplaceBlockConfig;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.world.gen.placement.SimplePlacement;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;

import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

public class CompactOreWorldGen {

    @ObjectHolder("compactores:all_with_probability")
    private static Placement<ProbabilityConfig> ALL_WITH_PROBABILITY_PLACEMENT = null;

    public static void init(Map<ResourceLocation, CompactOre> ores) {
        for(Biome biome : ForgeRegistries.BIOMES) {
            for(CompactOre ore : ores.values()) {
                biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION /*stage immediately after UNDERGROUND_ORES*/,
                        Biome.createDecoratedFeature(Feature.EMERALD_ORE /*ReplaceBlockFeature*/,
                                new ReplaceBlockConfig(ore.getBaseBlock().getDefaultState(), ore.getBlock().getDefaultState()),
                                ALL_WITH_PROBABILITY_PLACEMENT,
                                new ProbabilityConfig(ore.getSpawnProbability())));
            }
        }
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

}
