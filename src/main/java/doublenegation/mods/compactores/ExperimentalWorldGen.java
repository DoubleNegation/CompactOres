package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.ChunkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExperimentalWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private static Map<Block, CompactOre> oreByBlock;
    
    public static void init(Set<CompactOre> ores) {
        LOGGER.info("Experimental Compact Ores world gen is active for {} ores.", ores.size());
        oreByBlock = ores.stream().collect(Collectors.toMap(CompactOre::getBaseBlock, Function.identity()));
        MinecraftForge.EVENT_BUS.addListener(ExperimentalWorldGen::onChunkLoad);
    }
    
    private static void onChunkLoad(ChunkEvent.Load event) {
        if(!(event.getWorld() instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.getWorld();
        WorldGenData data = world.getSavedData().getOrCreate(WorldGenData::new, WorldGenData.NAME);
        ChunkPos chunkPos = event.getChunk().getPos();
        Set<ResourceLocation> newOres = data.missingOresForChunk(chunkPos,
                loc -> Optional.ofNullable(CompactOres.getFor(loc)).map(CompactOre::isRetrogen).orElse(false));
        if(!newOres.isEmpty()) {
            world.getServer().enqueue(new TickDelayedTask(0, () -> {
                generate(world, chunkPos, newOres);
                data.generated(chunkPos, newOres);
            }));
        }
    }
    
    private static void generate(ServerWorld world, ChunkPos pos, Set<ResourceLocation> ores) {
        LOGGER.debug("Generating {} compact ore types in chunk ({}|{}|{})", ores.size(),
                world.getDimensionKey().getLocation(), pos.x, pos.z);
        // initialize the random number generator
        // the "123" parameter would normally be some kind of sequence number that is incremented by one for every
        // feature that is generated, but since we're not generating as a feature in the sequence of features, we
        // just hardcode any old number and call it a day.
        SharedSeedRandom rand = new SharedSeedRandom();
        rand.setFeatureSeed(world.getSeed(), 123, GenerationStage.Decoration.UNDERGROUND_ORES.ordinal());
        // now generate, using the same rng calls in the same order as the other generator would use.
        // can't lead to the same ore pattern though, since our seed will be different :(
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < world.getHeight(); y++) {
                    float randVal = rand.nextFloat();
                    BlockPos blockPos = new BlockPos(16 * pos.x + x, y, 16 * pos.z + z);
                    Block block = world.getBlockState(blockPos).getBlock();
                    if(ores.contains(block.getRegistryName()) && randVal <= oreByBlock.get(block).getSpawnProbability()) {
                        world.setBlockState(blockPos, oreByBlock.get(block).getCompactOreBlock().getDefaultState(), 2 | 16);
                    }
                }
            }
        }
    }
    
    private static class WorldGenData extends WorldSavedData {
        private static final String NAME = CompactOres.MODID + "_WorldGenData";
        
        private int currentSetup;
        private final List<Set<ResourceLocation>> setups = new ArrayList<>();
        private final Map<ChunkPos, Integer> chunkStates = new HashMap<>();

        private WorldGenData() {
            super(NAME);
            // defaults
            currentSetup = 0;
            setups.add(0, CompactOres.compactOres().stream().map(CompactOre::getBaseBlockRegistryName).collect(Collectors.toSet()));
        }

        @Override
        public void read(CompoundNBT nbt) {
            this.currentSetup = -1;
            this.setups.clear();
            this.chunkStates.clear();
            Set<ResourceLocation> currentSetup = CompactOres.compactOres().stream().map(CompactOre::getBaseBlockRegistryName).collect(Collectors.toSet());
            ListNBT setups = nbt.getList("setups", Constants.NBT.TAG_LIST);
            for(int i = 0; i < setups.size(); i++) {
                this.setups.add(i, setups.getList(i).stream().map(INBT::getString).map(Utils::parseResourceLocation).collect(Collectors.toSet()));
                if(this.currentSetup == -1 && currentSetup.equals(this.setups.get(i))) {
                    this.currentSetup = i;
                }
            }
            if(this.currentSetup == -1) {
                this.currentSetup = setups.size();
                this.setups.add(currentSetup);
            }
            CompoundNBT chunks = nbt.getCompound("chunks");
            for(String regionKey : chunks.keySet()) {
                String[] tk = regionKey.split("\\.");
                if(tk.length != 3 || !tk[0].equals("r")) {
                    LOGGER.warn("Unexpected region key in {}: {}", NAME, regionKey);
                }
                int rrx = 32 * Integer.parseInt(tk[1]);
                int rrz = 32 * Integer.parseInt(tk[2]);
                int[] data = chunks.getIntArray(regionKey);
                for(int i = 0; i < data.length; i++) {
                    if(data[i] >= 0) {
                        chunkStates.put(new ChunkPos(rrx + i % 32, rrz + i / 32), data[i]);
                    }
                }
            }
        }

        @Override
        public CompoundNBT write(CompoundNBT compound) {
            ListNBT setups = new ListNBT();
            for(Set<ResourceLocation> setup : this.setups) {
                // unused "setups" are currently also saved to disk - is that a problem?
                // they shouldn't be too large, right? and removing them properly would be a lot of effort.
                ListNBT setupNBT = new ListNBT();
                setupNBT.addAll(setup.stream().map(ResourceLocation::toString).map(StringNBT::valueOf).collect(Collectors.toList()));
                setups.add(setupNBT);
            }
            compound.put("setups", setups);
            CompoundNBT chunks = new CompoundNBT();
            for(ChunkPos chunk : chunkStates.keySet()) {
                int rx = (int)Math.floor(chunk.x / 32f);
                int rz = (int)Math.floor(chunk.z / 32f);
                String key = "r." + rx + "." + rz;
                if(!chunks.contains(key)) {
                    int[] d = new int[32 * 32];
                    Arrays.fill(d, -1);
                    chunks.put(key, new IntArrayNBT(d));
                }
                ((IntArrayNBT)chunks.get(key)).set(32 * (chunk.z - 32 * rz) + (chunk.x - 32 * rx), IntNBT.valueOf(chunkStates.get(chunk)));
            }
            compound.put("chunks", chunks);
            return compound;
        }

        private Set<ResourceLocation> missingOresForChunk(ChunkPos pos, Predicate<ResourceLocation> retrogenAllowed) {
            if(!chunkStates.containsKey(pos)) return setups.get(currentSetup);
            if(chunkStates.get(pos) == currentSetup) return Collections.emptySet();
            Set<ResourceLocation> setup = setups.get(chunkStates.get(pos));
            return setups.get(currentSetup).stream().filter(ore -> !setup.contains(ore)).filter(retrogenAllowed).collect(Collectors.toSet());
        }

        private void generated(ChunkPos pos, Set<ResourceLocation> newOres) {
            Set<ResourceLocation> currentOresInChunk = chunkStates.containsKey(pos) ? new HashSet<>(setups.get(chunkStates.get(pos))) : new HashSet<>();
            currentOresInChunk.addAll(newOres);
            // find the setup which now matches the chunk, or create a new one if necessary
            if(currentOresInChunk.equals(setups.get(currentSetup))) {
                chunkStates.put(pos, currentSetup);
            } else {
                boolean found = false;
                for(int i = 0; i < setups.size(); i++) {
                    if(currentOresInChunk.equals(setups.get(i))) {
                        chunkStates.put(pos, i);
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    int setup = setups.size();
                    setups.add(setup, currentOresInChunk);
                    chunkStates.put(pos, setup);
                }
            }
            markDirty();
        }
        
    }
    
}
