package doublenegation.mods.compactores.compat;

import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOreBlock;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.block.BlockState;
import net.minecraftforge.common.MinecraftForge;
import oreexcavation.events.EventExcavate;
import oreexcavation.groups.BlockEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class OreExcavationIntegration {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(OreExcavationIntegration::onExcavate);
        LOGGER.info("Registered Compact Ores Ore Excavation integration");
    }

    public static void onExcavate(final EventExcavate.Pre excavateEvent) {
        List<BlockEntry> entries = new ArrayList<>(excavateEvent.getAgent().blockGroup);
        // the block group list can be empty if only a state was used for initialization of the agent instead of a blockgroup
        // in that case, create a blockgroup with that one state
        if(entries.size() == 0) {
            entries.add(new BlockEntry(excavateEvent.getAgent().state));
        }
        // for each ore in the group, add the corresponding compact ore
        // there should never be a compact ore in the list because the compact ores block break handler runs before
        // the ore excavation block break handler and fires more block break events which causes ore excavation to
        // only act on the latest one, which can only be a normal ore fired by the compact ore, but never the compact
        // ore itself
        // so there doesn't need to be a case that accounts for a compact ore showing up the group
        for(BlockEntry entry : entries) {
            CompactOre ore = CompactOres.getFor(entry.idName);
            if(ore != null) {
                LOGGER.info("Excavating " + entry.idName + ", which has a compact variant - adding compact variant to block group");
                excavateEvent.getAgent().blockGroup.add(new StrictlyMatchingBlockStateEntry(CompactOres.COMPACT_ORE.get().getDefaultState().with(CompactOreBlock.ORE_PROPERTY, ore)));
            }
        }
    }

    public static class StrictlyMatchingBlockStateEntry extends BlockEntry {
        public StrictlyMatchingBlockStateEntry(BlockState state) {
            super(state);
        }
        @Override
        public boolean checkMatch(BlockState blockState) {
            return this.state.equals(blockState);
        }
    }

}
