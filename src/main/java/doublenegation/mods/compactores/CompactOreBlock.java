package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CompactOreBlock extends Block {

    private final CompactOre ore;

    public CompactOreBlock(CompactOre ore) {
        // all vanilla ores (except gilded blackstone) have hardness and resistance values of 3, so use 3 here too
        super(Properties.create(Material.ROCK).sound(SoundType.STONE).setRequiresTool().hardnessAndResistance(3, 3));
        this.ore = ore;
    }

    public CompactOre getOre() {
        return ore;
    }

    @Override
    public float getExplosionResistance(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion) {
        return ore.getBaseBlock().getExplosionResistance(ore.getBaseBlock().getDefaultState(), world, pos, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, IWorldReader world, BlockPos pos, int fortune, int silktouch) {
        Random rand = Optional.of(world.getChunk(pos)).map(IChunk::getWorldForge).map(IWorld::getRandom).orElse(new Random());
        int r = ore.getMinRolls() + rand.nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
        return ore.getBaseBlock().getExpDrop(ore.getBaseBlock().getDefaultState(), world, pos, fortune, silktouch) * r;
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return ore.getBaseBlock().getHarvestLevel(ore.getBaseBlock().getDefaultState());
    }

    @Nullable
    @Override
    public ToolType getHarvestTool(BlockState state) {
        return ore.getBaseBlock().getHarvestTool(ore.getBaseBlock().getDefaultState());
    }

    @Override
    public float getPlayerRelativeBlockHardness(BlockState state, PlayerEntity player, IBlockReader worldIn, BlockPos pos) {
        return ore.getBaseBlock().getDefaultState().getPlayerRelativeBlockHardness(player, worldIn, pos);
    }

    @Override
    public MaterialColor getMaterialColor() {
        return ore.getBaseBlock().getMaterialColor();
    }

    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return ore.getBaseBlock().canHarvestBlock(ore.getBaseBlock().getDefaultState(), world, pos, player);
    }

    @Override
    public boolean isToolEffective(BlockState state, ToolType tool) {
        return ore.getBaseBlock().isToolEffective(ore.getBaseBlock().getDefaultState(), tool);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return ore.getBaseBlock().getSoundType(ore.getBaseBlock().getDefaultState(), world, pos, entity);
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return ore.getBaseBlock().getSoundType(ore.getBaseBlock().getDefaultState());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        if(ore.isUseGetDrops()) {
            List<ItemStack> parentList = super.getDrops(state, builder);
            List<ItemStack> oreList = ore.getBaseBlock().getDrops(ore.getBaseBlock().getDefaultState(), builder);
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
