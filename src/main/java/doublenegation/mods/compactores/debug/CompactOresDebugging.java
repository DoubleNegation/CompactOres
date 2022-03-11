package doublenegation.mods.compactores.debug;

import doublenegation.mods.compactores.CompactOres;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.DistExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompactOresDebugging {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static boolean enabled = false;
    
    public static void enable() {
        enabled = true;
        LOGGER.info("Compact Ores is running in debug mode. Consider disabling this in production.");
    }
    
    public static boolean enabled() {
        return enabled;
    }
    
    public static void init() {
        // always register the command
        // it will show an error message if debugging is disabled
        MinecraftForge.EVENT_BUS.addListener(CompactOresCommand::register);
        if(!enabled) {
            LOGGER.info("Compact Ores is running in production mode.");
            return;
        }
        MinecraftForge.EVENT_BUS.addListener((final EntityJoinWorldEvent event) -> {
            if(event.getWorld().isClientSide && event.getEntity() instanceof Player) {
                event.getEntity().sendMessage(new TranslatableComponent("chat.compactores.debugging_enabled"), event.getEntity().getUUID());
            }
        });
        WorldGenDebugging.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            TextureDumper.init();
            TextureEditor.init();
        });
    }
    
    static Flags getFlags(MinecraftServer server) {
        ServerLevel world = server.getLevel(Level.OVERWORLD);
        if(world == null) throw new IllegalStateException("Server does not have an overworld, can not access Debug Data.");
        return getFlags(world);
    }
    
    private static Flags getFlags(ServerLevel world) {
        return world.getDataStorage().get(Flags::read, Flags.NAME);
    }

    static class Flags extends SavedData {
        private static final String NAME = CompactOres.MODID + "_DebugFlags";

        private boolean debugWorldGen = false;

        private Flags(boolean debugWorldGen) {
            this.debugWorldGen = debugWorldGen;
        }

        public static Flags read(CompoundTag nbt) {
            boolean debugWorldGen = nbt.getBoolean("debugWorldGen");
            return new Flags(debugWorldGen);
        }

        @Override
        public CompoundTag save(CompoundTag compound) {
            compound.putBoolean("debugWorldGen", debugWorldGen);
            return compound;
        }
        
        boolean isDebugWorldGen() {
            return debugWorldGen;
        }
        
        void setDebugWorldGen(boolean debugWorldGen) {
            this.debugWorldGen = debugWorldGen;
        }
    }
    
}
