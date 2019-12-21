package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CompactOreBlock extends Block {

    private ResourceLocation baseBlockLoc;
    private Block baseBlock = null;
    private boolean useGetDrops;
    private int minRolls, maxRolls;

    public CompactOreBlock(ResourceLocation registryName, ResourceLocation baseBlockLoc, boolean useGetDrops,
                           int minRolls, int maxRolls) {
        super(Properties.create(Material.ROCK).sound(SoundType.STONE));
        this.baseBlockLoc = baseBlockLoc;
        this.useGetDrops = useGetDrops;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
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

    @Override
    public List<ItemStack> getDrops(BlockState p_220076_1_, LootContext.Builder p_220076_2_) {
        if(useGetDrops) {
            List<ItemStack> drops = baseBlock().getDrops(p_220076_1_, p_220076_2_);
            List<ItemStack> ret = new ArrayList<>();
            int r = minRolls + new Random().nextInt(maxRolls - minRolls + 1);
            for(int i = 0; i < r; i++) {
                for(ItemStack stack : drops) {
                    // adding the same item stack several times doesn't work properly - copy it instead
                    ret.add(stack.copy());
                }
            }
            return ret;
        } else {
            return super.getDrops(p_220076_1_, p_220076_2_);
        }
    }

    @Override
    public String getTranslationKey() {
        // dirty hack to make Hwyla behave
        return getNameTextComponent().getFormattedText();
    }

    @Override
    public ITextComponent getNameTextComponent() {
        // provides translation to the BlockItem
        return new TranslationTextComponent("block.compactores.compact_ore", baseBlock().getNameTextComponent());
    }
}
