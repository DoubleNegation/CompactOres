package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class CompactOreBlock extends Block {

    private ResourceLocation baseBlockLoc;
    private Block baseBlock = null;

    public CompactOreBlock(ResourceLocation registryName, ResourceLocation baseBlockLoc) {
        super(Properties.create(Material.ROCK).sound(SoundType.STONE));
        this.baseBlockLoc = baseBlockLoc;
        setRegistryName(registryName);
    }

    Block baseBlock() {
        return baseBlock != null ? baseBlock : ForgeRegistries.BLOCKS.getValue(baseBlockLoc);
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        return baseBlock().getExplosionResistance(state, world, pos, exploder, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, IWorldReader world, BlockPos pos, int fortune, int silktouch) {
        return baseBlock().getExpDrop(state, world, pos, fortune, silktouch);
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return baseBlock().getHarvestLevel(state);
    }

    @Nullable
    @Override
    public ToolType getHarvestTool(BlockState state) {
        return baseBlock().getHarvestTool(state);
    }

    @Override
    public float getBlockHardness(BlockState state, IBlockReader p_176195_2_, BlockPos pos) {
        return baseBlock().getBlockHardness(state, p_176195_2_, pos);
    }

    @Override
    public float getExplosionResistance() {
        return baseBlock().getExplosionResistance();
    }

    @Override
    public Material getMaterial(BlockState state) {
        return baseBlock().getMaterial(state);
    }

    @Override
    public MaterialColor getMaterialColor(BlockState state, IBlockReader p_180659_2_, BlockPos pos) {
        return baseBlock().getMaterialColor(state, p_180659_2_, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return baseBlock().canHarvestBlock(state, world, pos, player);
    }

    @Override
    public boolean isToolEffective(BlockState state, ToolType tool) {
        return baseBlock().isToolEffective(state, tool);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return baseBlock().getSoundType(state, world, pos, entity);
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return baseBlock().getSoundType(state);
    }

    @Override
    public ResourceLocation getLootTable() {
        return getRegistryName();
    }

}
