package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

public class CompactOreBlock extends Block {

    public static final IProperty<CompactOre> ORE_PROPERTY = new CompactOreProperty<>("ore", CompactOre.class, CompactOres.compactOres());

    public CompactOreBlock() {
        super(Properties.create(Material.ROCK).sound(SoundType.STONE));
        this.setDefaultState(this.stateContainer.getBaseState().with(ORE_PROPERTY, CompactOres.compactOres().get(0)));
    }

    Block baseBlock(BlockState state) {
        return state.get(ORE_PROPERTY).getBaseBlock();
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(ORE_PROPERTY);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        state = state == null ? CompactOres.COMPACT_ORE.get().getDefaultState() : state;
        CompactOreBlockItem item = CompactOres.COMPACT_ORE_ITEM.get();
        CompactOre ore = item.findOreForStack(context.getItem());
        return state.with(CompactOreBlock.ORE_PROPERTY, ore);
    }


    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        CompactOre ore = state.get(ORE_PROPERTY);
        return CompactOres.COMPACT_ORE_ITEM.get().getStackOfOre(ore, 1);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new CompactOreTileEntity(state.get(ORE_PROPERTY));
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        return baseBlock(state).getExplosionResistance(state, world, pos, exploder, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, IWorldReader world, BlockPos pos, int fortune, int silktouch) {
        Random rand = Optional.of(world.getChunk(pos)).map(IChunk::getWorldForge).map(IWorld::getRandom).orElse(new Random());
        CompactOre ore = state.get(ORE_PROPERTY);
        int r = ore.getMinRolls() + rand.nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
        return baseBlock(state).getExpDrop(state, world, pos, fortune, silktouch) * r;
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return baseBlock(state).getHarvestLevel(state);
    }

    @Nullable
    @Override
    public ToolType getHarvestTool(BlockState state) {
        return baseBlock(state).getHarvestTool(state);
    }

    @Override
    public float getBlockHardness(BlockState state, IBlockReader p_176195_2_, BlockPos pos) {
        return baseBlock(state).getBlockHardness(state, p_176195_2_, pos);
    }

    @Override
    public Material getMaterial(BlockState state) {
        return baseBlock(state).getMaterial(state);
    }

    @Override
    public MaterialColor getMaterialColor(BlockState state, IBlockReader p_180659_2_, BlockPos pos) {
        return baseBlock(state).getMaterialColor(state, p_180659_2_, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return baseBlock(state).canHarvestBlock(state, world, pos, player);
    }

    @Override
    public boolean isToolEffective(BlockState state, ToolType tool) {
        return baseBlock(state).isToolEffective(state, tool);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return baseBlock(state).getSoundType(state, world, pos, entity);
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return baseBlock(state).getSoundType(state);
    }

}
