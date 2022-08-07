package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class CompactOreBlockState  extends BlockState {

    private final CompactOre ore;

    public CompactOreBlockState(Block p_61042_, ImmutableMap<Property<?>, Comparable<?>> p_61043_, MapCodec<BlockState> p_61044_, CompactOre ore) {
        super(p_61042_, p_61043_, p_61044_);
        this.ore = ore;
    }

    @Override
    public float getDestroySpeed(BlockGetter p_60801_, BlockPos p_60802_) {
        return ore.getBaseBlock().defaultBlockState().getDestroySpeed(p_60801_, p_60802_) * ore.getBreakTimeMultiplier();
    }

    @Override
    public SoundType getSoundType() {
        return ore.getBaseBlock().defaultBlockState().getSoundType();
    }

    @Override
    public SoundType getSoundType(LevelReader level, BlockPos pos, Entity entity) {
        return ore.getBaseBlock().defaultBlockState().getSoundType(level, pos, entity);
    }

}
