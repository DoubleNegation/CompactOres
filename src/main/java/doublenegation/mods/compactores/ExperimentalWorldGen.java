package doublenegation.mods.compactores;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
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
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExperimentalWorldGen {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private static Map<Block, CompactOre> oreByBlock;
    private static final Map<ServerChunkCache, BlockableEventLoop<Runnable>> eventLoopCache = new WeakHashMap<>(3);
    
    public static void init(Set<CompactOre> ores) {
        LOGGER.info("Experimental Compact Ores world gen is active for {} ores.", ores.size());
        oreByBlock = ores.stream().collect(Collectors.toMap(CompactOre::getBaseBlock, Function.identity()));
        MinecraftForge.EVENT_BUS.addListener(ExperimentalWorldGen::onChunkLoad);
    }
    
    private static void onChunkLoad(ChunkEvent.Load event) {
        if(!(event.getWorld() instanceof ServerLevel)) return;
        ServerLevel world = (ServerLevel) event.getWorld();
        WorldGenData data = world.getDataStorage().get(WorldGenData::read, WorldGenData.NAME);
        if(data == null) {
            world.getDataStorage().set(WorldGenData.NAME, data = WorldGenData.createNew());
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        Set<ResourceLocation> newOres = data.missingOresForChunk(chunkPos,
                loc -> Optional.ofNullable(CompactOres.getFor(loc)).map(CompactOre::isRetrogen).orElse(false));
        if(!newOres.isEmpty()) {
            BlockableEventLoop<Runnable> loop = eventLoopForWorld(world);
            if(loop == null) {
                LOGGER.error("Failed to obtain ServerChunkCache.mainThreadProcessor, can not generate Compact Ores.");
                return;
            }
            WorldGenData finalData = data;
            loop.submitAsync(() -> {
                generate(world, chunkPos, newOres);
                finalData.generated(chunkPos, newOres);
            });
        }
    }

    private static BlockableEventLoop<Runnable> eventLoopForWorld(ServerLevel world) {
        if(eventLoopCache.containsKey(world.getChunkSource())) {
            return eventLoopCache.get(world.getChunkSource());
        } else {
            BlockableEventLoop<Runnable> loop = ObfuscationReflectionHelper.getPrivateValue(ServerChunkCache.class, world.getChunkSource(), "f_8332_" /*mainThreadProcessor*/);
            if(loop != null) {
                eventLoopCache.put(world.getChunkSource(), loop);
            }
            return loop;
        }
    }
    
    private static void generate(ServerLevel world, ChunkPos pos, Set<ResourceLocation> ores) {
        LOGGER.debug("Generating {} compact ore types in chunk ({}|{}|{})", ores.size(),
                world.dimension().location(), pos.x, pos.z);
        // initialize the random number generator
        // the "123" parameter would normally be some kind of sequence number that is incremented by one for every
        // feature that is generated, but since we're not generating as a feature in the sequence of features, we
        // just hardcode any old number and call it a day.
        WorldgenRandom rand = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
        rand.setFeatureSeed(world.getSeed(), 123, GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
        // now generate, using the same rng calls in the same order as the other generator would use.
        // can't lead to the same ore pattern though, since our seed will be different :(
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < world.getHeight(); y++) {
                    float randVal = rand.nextFloat();
                    BlockPos blockPos = new BlockPos(16 * pos.x + x, y, 16 * pos.z + z);
                    Block block = world.getBlockState(blockPos).getBlock();
                    if(ores.contains(block.getRegistryName()) && randVal <= oreByBlock.get(block).getSpawnProbability()) {
                        world.setBlock(blockPos, oreByBlock.get(block).getCompactOreBlock().defaultBlockState(), 2 | 16);
                    }
                }
            }
        }
    }
    
    private static class WorldGenData extends SavedData {
        private static final String NAME = CompactOres.MODID + "_WorldGenData";
        
        private final int currentSetup;
        private final List<Set<ResourceLocation>> setups;
        private final Map<ChunkPos, Integer> chunkStates;

        private WorldGenData(int currentSetup, List<Set<ResourceLocation>> setups, Map<ChunkPos, Integer> chunkStates) {
            this.currentSetup = currentSetup;
            this.setups = setups;
            this.chunkStates = chunkStates;
        }

        public static WorldGenData read(CompoundTag nbt) {
            int theCurrentSetup = -1;
            List<Set<ResourceLocation>> theSetups = new ArrayList<>();
            Map<ChunkPos, Integer> theChunkStates = new HashMap<>();
            Set<ResourceLocation> currentSetup = CompactOres.compactOres().stream().map(CompactOre::getBaseBlockRegistryName).collect(Collectors.toSet());
            ListTag setups = nbt.getList("setups", Tag.TAG_LIST);
            for(int i = 0; i < setups.size(); i++) {
                theSetups.add(i, setups.getList(i).stream().map(Tag::getAsString).map(Utils::parseResourceLocation).collect(Collectors.toSet()));
                if(theCurrentSetup == -1 && currentSetup.equals(theSetups.get(i))) {
                    theCurrentSetup = i;
                }
            }
            if(theCurrentSetup == -1) {
                theCurrentSetup = setups.size();
                theSetups.add(currentSetup);
            }
            CompoundTag chunks = nbt.getCompound("chunks");
            for(String regionKey : chunks.getAllKeys()) {
                String[] tk = regionKey.split("\\.");
                if(tk.length != 3 || !tk[0].equals("r")) {
                    LOGGER.warn("Unexpected region key in {}: {}", NAME, regionKey);
                }
                int rrx = 32 * Integer.parseInt(tk[1]);
                int rrz = 32 * Integer.parseInt(tk[2]);
                int[] data = chunks.getIntArray(regionKey);
                for(int i = 0; i < data.length; i++) {
                    if(data[i] >= 0) {
                        theChunkStates.put(new ChunkPos(rrx + i % 32, rrz + i / 32), data[i]);
                    }
                }
            }
            return new WorldGenData(theCurrentSetup, theSetups, theChunkStates);
        }

        private static WorldGenData createNew() {
            CompoundTag root = new CompoundTag();
            root.put("setups", new ListTag());
            root.put("chunks", new CompoundTag());
            return read(root);
        }

        @Override
        public CompoundTag save(CompoundTag compound) {
            ListTag setups = new ListTag();
            for(Set<ResourceLocation> setup : this.setups) {
                // unused "setups" are currently also saved to disk - is that a problem?
                // they shouldn't be too large, right? and removing them properly would be a lot of effort.
                ListTag setupNBT = new ListTag();
                setupNBT.addAll(setup.stream().map(ResourceLocation::toString).map(StringTag::valueOf).collect(Collectors.toList()));
                setups.add(setupNBT);
            }
            compound.put("setups", setups);
            CompoundTag chunks = new CompoundTag();
            for(ChunkPos chunk : chunkStates.keySet()) {
                int rx = (int)Math.floor(chunk.x / 32f);
                int rz = (int)Math.floor(chunk.z / 32f);
                String key = "r." + rx + "." + rz;
                if(!chunks.contains(key)) {
                    int[] d = new int[32 * 32];
                    Arrays.fill(d, -1);
                    chunks.put(key, new IntArrayTag(d));
                }
                ((IntArrayTag)chunks.get(key)).set(32 * (chunk.z - 32 * rz) + (chunk.x - 32 * rx), IntTag.valueOf(chunkStates.get(chunk)));
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
            setDirty();
        }
        
    }
    
}
