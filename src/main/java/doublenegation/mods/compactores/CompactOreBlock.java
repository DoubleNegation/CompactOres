package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CompactOreBlock extends Block {

    public static IProperty<CompactOre> ORE_PROPERTY = new CompactOreProperty<>("ore", CompactOre.class, CompactOres.compactOres());

    public CompactOreBlock() {
        super(Properties.create(Material.ROCK).sound(SoundType.STONE));
        this.setDefaultState(this.stateContainer.getBaseState().with(ORE_PROPERTY, CompactOres.compactOres().get(0)));
    }

    Block baseBlock(BlockState state) {
        // the base block is null if a block name which does not exist was specified in the config
        // convert the null to stone here so that the game doesn't crash (but block will behave like stone)
        return Optional.ofNullable(state).filter(st -> st.getBlock() == this).map(st -> st.get(ORE_PROPERTY).getBaseBlock()).orElse(Blocks.STONE);
    }

    CompactOre ore(BlockState state, boolean defaultToMissing) {
        return Optional.ofNullable(state).filter(st -> st.getBlock() == this).map(st -> st.get(ORE_PROPERTY)).orElse(CompactOres.compactOres().get(defaultToMissing ? 0 : 1));
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
        CompactOre ore = ore(state, false);
        return CompactOres.COMPACT_ORE_ITEM.get().getStackOfOre(ore, 1);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new CompactOreTileEntity(ore(state, true));
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        return baseBlock(state).getExplosionResistance(state, world, pos, exploder, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, IWorldReader world, BlockPos pos, int fortune, int silktouch) {
        Random rand = Optional.of(world.getChunk(pos)).map(IChunk::getWorldForge).map(IWorld::getRandom).orElse(new Random());
        CompactOre ore = ore(state, true);
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

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        CompactOre ore = ore(state, true);
        if(ore.isUseGetDrops()) {
            List<ItemStack> parentList = super.getDrops(state, builder);
            List<ItemStack> oreList = ore.getBaseBlock().getDrops(state, builder);
            Random rand = builder.getWorld().getRandom();
            int r = ore.getMinRolls() + rand.nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
            for(int i = 0; i < r; i++) {
                for(ItemStack stack : oreList) {
                    parentList.add(new ItemStack(stack.getItem(), stack.getCount(), stack.getTag()));
                }
            }
            return parentList;
        } else {
            return super.getDrops(state, builder);
        }
    }

}
