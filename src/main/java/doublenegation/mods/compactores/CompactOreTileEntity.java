package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class CompactOreTileEntity extends TileEntity {

    private Block ore;
    private String backupName;

    public CompactOreTileEntity() {
        super(CompactOres.COMPACT_ORE_TE.get());
    }

    public CompactOreTileEntity(CompactOre ore) {
        super(CompactOres.COMPACT_ORE_TE.get());
        this.ore = ore.getBaseBlock();
    }

    private CompoundNBT writeDataToNBT(CompoundNBT compound) {
        compound.putString("ore", ore == null || ore.getRegistryName() == null ?
                (backupName == null ? "null" : backupName) : ore.getRegistryName().toString());
        return compound;
    }

    private CompoundNBT readDataFromNBT(CompoundNBT compound) {
        ResourceLocation oreName = Utils.parseResourceLocation(compound.getString("ore"));
        if(ForgeRegistries.BLOCKS.containsKey(oreName)) {
            ore = ForgeRegistries.BLOCKS.getValue(oreName);
        } else {
            ore = null;
            backupName = compound.getString("ore");
        }
        if(hasWorld()) {
            CompactOre compactOre = CompactOres.getFor(oreName);
            if(compactOre == null) compactOre = CompactOres.compactOres().get(0);
            getWorld().setBlockState(getPos(), getBlockState().with(CompactOreBlock.ORE_PROPERTY, compactOre));
        }
        return compound;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return super.write(writeDataToNBT(compound));
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, readDataFromNBT(compound));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return writeDataToNBT(super.getUpdateTag());
    }

    /* TODO: this method seems to have gone static (func_235657_b_), find out what to do now (if anything)
    @Override
    public void handleUpdateTag(CompoundNBT tag) {
        super.handleUpdateTag(readDataFromNBT(tag));
    }*/

}
