package doublenegation.mods.compactores;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.storage.loot.LootContext;

import javax.annotation.Nullable;
import java.util.List;

public class CompactOreBlock extends Block {

    private final CompactOre ore;

    public CompactOreBlock(CompactOre ore) {
        // all vanilla stone and netherrack ores have hardness and resistance values of 3, so use 3 here too
        // deepslate ones have hardness 4.5, resistance 3, but this may be very difficult to implement
        super(Properties.of(Material.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(3, 3));
        this.ore = ore;
    }

    public CompactOre getOre() {
        return ore;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
        return ore.getBaseBlock().getExplosionResistance(ore.getBaseBlock().defaultBlockState(), world, pos, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader world, BlockPos pos, int fortune, int silktouch) {
        int r = ore.getMinRolls() + RANDOM.nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
        return ore.getBaseBlock().getExpDrop(ore.getBaseBlock().defaultBlockState(), world, pos, fortune, silktouch) * r;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter worldIn, BlockPos pos) {
        return ore.getBaseBlock().defaultBlockState().getDestroyProgress(player, worldIn, pos);
    }

    @Override
    public MaterialColor defaultMaterialColor() {
        return ore.getBaseBlock().defaultMaterialColor();
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return ore.getBaseBlock().canHarvestBlock(ore.getBaseBlock().defaultBlockState(), world, pos, player);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, @Nullable Entity entity) {
        return ore.getBaseBlock().getSoundType(ore.getBaseBlock().defaultBlockState(), world, pos, entity);
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return ore.getBaseBlock().getSoundType(ore.getBaseBlock().defaultBlockState());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        if(ore.isUseGetDrops()) {
            List<ItemStack> parentList = super.getDrops(state, builder);
            List<ItemStack> oreList = ore.getBaseBlock().getDrops(ore.getBaseBlock().defaultBlockState(), builder);
            int r = ore.getMinRolls() + RANDOM.nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
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
