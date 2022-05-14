package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class WorldGenDebugging {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final Set<ResourceLocation> ores = new HashSet<>();
    private static Method getChunks;
    private static Field serverLevelData;

    private static final List<Triple<TickTask, MinecraftServer, BiPredicate<TickTask, MinecraftServer>>> tasksForDelayedExecution = new ArrayList<>();
    
    static void init() {
        MinecraftForge.EVENT_BUS.addListener(WorldGenDebugging::onChunkLoad);
        ores.add(Blocks.AIR.getRegistryName());
        CompactOres.compactOres().forEach(ore -> {
            ores.add(ore.getBaseBlockRegistryName());
            ores.add(ore.name());
        });
        try {
            getChunks = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "m_140416_"/*getChunks*/);
            getChunks.setAccessible(true);
        } catch(Exception e) {
            LOGGER.error("Unable to find ChunkMap#getChunks - `/compactores debugworldgen` will not clear the world immediately.", e);
        }
        try {
            serverLevelData = ObfuscationReflectionHelper.findField(ServerLevel.class, "f_8549_"/*serverLevelData*/);
            serverLevelData.setAccessible(true);
        } catch(Exception e) {
            LOGGER.error("Unable to find ServerLevel#serverLevelData", e);
        }
        new Timer("Compact Ores World Gen Debugging Delayed Execution Thread").scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                synchronized(tasksForDelayedExecution) {
                    Iterator<Triple<TickTask, MinecraftServer, BiPredicate<TickTask, MinecraftServer>>> it = tasksForDelayedExecution.iterator();
                    while(it.hasNext()) {
                        Triple<TickTask, MinecraftServer, BiPredicate<TickTask, MinecraftServer>> task = it.next();
                        if(task.getRight().test(task.getLeft(), task.getMiddle())) {
                            task.getMiddle().submit(task.getLeft());
                            it.remove();
                        } else if(task.getMiddle().isStopped()) {
                            it.remove();
                        }
                    }
                }
            }
        }, 0, 100);
    }
    
    public static int executeCommand(CommandContext<CommandSourceStack> ctx) {
        if(!CompactOresDebugging.enabled()) {
            ctx.getSource().sendFailure(new TranslatableComponent("commands.compactores.debugging_disabled"));
            return 0;
        }
        CompactOresDebugging.Flags flags = CompactOresDebugging.getFlags(ctx.getSource().getServer());
        if(flags.isDebugWorldGen()) {
            ctx.getSource().sendFailure(new TranslatableComponent("commands.compactores.debugworldgen.failure"));
            return 0;
        }
        flags.setDebugWorldGen(true);
        flags.setDirty();
        ctx.getSource().sendSuccess(new TranslatableComponent("commands.compactores.debugworldgen.success", tryFindWorldName(ctx.getSource().getServer())), true);
        for(ServerLevel world : ctx.getSource().getServer().getAllLevels()) {
            for(ChunkHolder chunkHolder : enumerateChunksOfWorld(world)) {
                LevelChunk chunk;
                if(!chunkHolder.wasAccessibleSinceLastSave() || (chunk = chunkHolder.getFullChunk()) == null) {
                    // chunk is not fully loaded, can not process it right now
                    // this causes a one-chunk border of normal chunks to be left around the currently loaded area(s)
                    continue;
                }
                processChunk(world, chunk);
            }
        }
        return 0;
    }
    
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor iWorld = event.getWorld();
        if(iWorld.isClientSide() || !(iWorld instanceof ServerLevel)) {
            return;
        }
        ServerLevel world = (ServerLevel) iWorld;
        CompactOresDebugging.Flags flags = CompactOresDebugging.getFlags(world.getServer());
        if(!flags.isDebugWorldGen()) return;
        synchronized(tasksForDelayedExecution) {
            // i couldn't get the event loop functions in MinecraftServer to delay the tasks until the next tick,
            // so i'm doing this manually here. not pretty, but it works.
            tasksForDelayedExecution.add(
                    new ImmutableTriple<>(new TickTask(world.getServer().getTickCount(), () -> processChunk(world, event.getChunk())),
                                        world.getServer(),
                                        (task, server) -> server.getTickCount() - 20 >= task.getTick()));
        }
    }
    
    private static void processChunk(ServerLevel world, ChunkAccess chunk) {
        long start = System.currentTimeMillis();
        int count = 0;
        ChunkPos cp = chunk.getPos();
        int cx = 16 * cp.x;
        int cz = 16 * cp.z;
        for(int y = world.getMinBuildHeight(); y < world.getHeight(); y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    if(!ores.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock().getRegistryName())) {
                        world.setBlock(new BlockPos(cx + x, y, cz + z), Blocks.AIR.defaultBlockState(), 2 | 16);
                        count++;
                    }
                }
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.info("Removed {} blocks from chunk ({}|{}|{}) in {}ms.", count, world.dimension().location(), cp.x, cp.z, (end - start));
    }

    private static Component tryFindWorldName(MinecraftServer server) {
        if(serverLevelData == null) return new TranslatableComponent("commands.compactores.debugworldgen.success.fallback_world_name");
        try {
            ServerLevelData level = (ServerLevelData) serverLevelData.get(server.getAllLevels().iterator().next());
            return new TextComponent(level.getLevelName());
        } catch(Exception e) {
            return new TranslatableComponent("commands.compactores.debugworldgen.success.fallback_world_name");
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Iterable<ChunkHolder> enumerateChunksOfWorld(ServerLevel world) {
        if(getChunks == null) return new ArrayList<>(0);
        try {
            return (Iterable<ChunkHolder>) getChunks.invoke(world.getChunkSource().chunkMap);
        } catch(Exception e) {
            LOGGER.error("Unable to obtain chunks of world " + world.dimension().getRegistryName() + " - world will not be cleared immediately", e);
            return new ArrayList<>(0);
        }
    }
    
}
