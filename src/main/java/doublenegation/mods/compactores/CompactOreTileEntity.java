package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class CompactOreTileEntity extends TileEntity {

    private Block ore;

    public CompactOreTileEntity() {
        super(CompactOres.COMPACT_ORE_TE.get());
    }

    public CompactOreTileEntity(CompactOre ore) {
        super(CompactOres.COMPACT_ORE_TE.get());
        this.ore = ore.getBaseBlock();
    }

    private CompoundNBT writeDataToNBT(CompoundNBT compound) {
        compound.putString("ore", ore == null || ore.getRegistryName() == null ? "null" : ore.getRegistryName().toString());
        return compound;
    }

    private CompoundNBT readDataFromNBT(CompoundNBT compound) {
        ore = ForgeRegistries.BLOCKS.getValue(Utils.parseResourceLocation(compound.getString("ore")));
        if(hasWorld()) {
            CompactOre compactOre = CompactOres.getFor(ore.getRegistryName());
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
    public void read(CompoundNBT compound) {
        super.read(readDataFromNBT(compound));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return writeDataToNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(CompoundNBT tag) {
        super.handleUpdateTag(readDataFromNBT(tag));
    }

}
