package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveFormat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WorldGenDebugging {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final Set<ResourceLocation> ores = new HashSet<>();
    private static Method getLoadedChunksIterable;
    private static Field anvilConverterForAnvilFile;
    
    static void init() {
        MinecraftForge.EVENT_BUS.addListener(WorldGenDebugging::onChunkLoad);
        ores.add(Blocks.AIR.getRegistryName());
        CompactOres.compactOres().forEach(ore -> {
            ores.add(ore.getBaseBlockRegistryName());
            ores.add(ore.name());
        });
        try {
            getLoadedChunksIterable = ObfuscationReflectionHelper.findMethod(ChunkManager.class, "func_223491_f"/*getLoadedChunksIterable*/);
            getLoadedChunksIterable.setAccessible(true);
        } catch(Exception e) {
            LOGGER.error("Unable to find ChunkManager#getLoadedChunksIterable - `/compactores debugworldgen` will not clear the world immediately.", e);
        }
        try {
            anvilConverterForAnvilFile = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_71310_m"/*anvilConverterForAnvilFile*/);
            anvilConverterForAnvilFile.setAccessible(true);
        } catch(Exception e) {
            LOGGER.error("Unable to find MinecraftServer#anvilConverterForAnvilFile", e);
        }
    }
    
    public static int executeCommand(CommandContext<CommandSource> ctx) {
        if(!CompactOresDebugging.enabled()) {
            ctx.getSource().sendErrorMessage(new TranslationTextComponent("commands.compactores.debugging_disabled"));
            return 0;
        }
        CompactOresDebugging.Flags flags = CompactOresDebugging.getFlags(ctx.getSource().getServer());
        if(flags.isDebugWorldGen()) {
            ctx.getSource().sendErrorMessage(new TranslationTextComponent("commands.compactores.debugworldgen.failure"));
            return 0;
        }
        flags.setDebugWorldGen(true);
        flags.markDirty();
        ctx.getSource().sendFeedback(new TranslationTextComponent("commands.compactores.debugworldgen.success", tryFindWorldName(ctx.getSource().getServer())), true);
        for(ServerWorld world : ctx.getSource().getServer().getWorlds()) {
            for(ChunkHolder chunkHolder : enumerateChunksOfWorld(world)) {
                Chunk chunk;
                if(!chunkHolder.isAccessible() || (chunk = chunkHolder.getChunkIfComplete()) == null) {
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
        IWorld iWorld = event.getWorld();
        if(iWorld.isRemote() || !(iWorld instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = (ServerWorld) iWorld;
        CompactOresDebugging.Flags flags = CompactOresDebugging.getFlags(world.getServer());
        if(!flags.isDebugWorldGen()) return;
        world.getServer().enqueue(new TickDelayedTask(0, () -> processChunk(world, event.getChunk())));
    }
    
    private static void processChunk(ServerWorld world, IChunk chunk) {
        long start = System.currentTimeMillis();
        int count = 0;
        ChunkPos cp = chunk.getPos();
        int cx = 16 * cp.x;
        int cz = 16 * cp.z;
        for(int y = 0; y < world.getHeight(); y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    if(!ores.contains(chunk.getBlockState(new BlockPos(x, y, z)).getBlock().getRegistryName())) {
                        world.setBlockState(new BlockPos(cx + x, y, cz + z), Blocks.AIR.getDefaultState(), 2 | 16);
                        count++;
                    }
                }
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.info("Removed {} blocks from chunk ({}|{}|{}) in {}ms.", count, world.getDimensionKey().getLocation(), cp.x, cp.z, (end - start));
    }

    private static ITextComponent tryFindWorldName(MinecraftServer server) {
        if(anvilConverterForAnvilFile == null) return new TranslationTextComponent("commands.compactores.debugworldgen.success.fallback_world_name");
        try {
            SaveFormat.LevelSave level = (SaveFormat.LevelSave) anvilConverterForAnvilFile.get(server);
            return new StringTextComponent(level.getSaveName());
        } catch(Exception e) {
            return new TranslationTextComponent("commands.compactores.debugworldgen.success.fallback_world_name");
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Iterable<ChunkHolder> enumerateChunksOfWorld(ServerWorld world) {
        if(getLoadedChunksIterable == null) return new ArrayList<>(0);
        try {
            return (Iterable<ChunkHolder>) getLoadedChunksIterable.invoke(world.getChunkProvider().chunkManager);
        } catch(Exception e) {
            LOGGER.error("Unable to obtain chunks of world " + world.getDimensionKey().getLocation() + " - world will not be cleared immediately", e);
            return new ArrayList<>(0);
        }
    }
    
}
