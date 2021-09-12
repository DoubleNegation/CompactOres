package doublenegation.mods.compactores.debug;

import doublenegation.mods.compactores.CompactOres;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
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
            if(event.getWorld().isRemote && event.getEntity() instanceof PlayerEntity) {
                event.getEntity().sendMessage(new TranslationTextComponent("chat.compactores.debugging_enabled"), event.getEntity().getUniqueID());
            }
        });
        WorldGenDebugging.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            TextureDumper.init();
            TextureEditor.init();
        });
    }
    
    static Flags getFlags(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if(world == null) throw new IllegalStateException("Server does not have an overworld, can not access Debug Data.");
        return getFlags(world);
    }
    
    private static Flags getFlags(ServerWorld world) {
        return world.getSavedData().getOrCreate(Flags::new, Flags.NAME);
    }

    static class Flags extends WorldSavedData {
        private static final String NAME = CompactOres.MODID + "_DebugFlags";

        private boolean debugWorldGen = false;

        private Flags() {
            super(NAME);
        }

        @Override
        public void read(CompoundNBT nbt) {
            debugWorldGen = nbt.getBoolean("debugWorldGen");
        }

        @Override
        public CompoundNBT write(CompoundNBT compound) {
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
