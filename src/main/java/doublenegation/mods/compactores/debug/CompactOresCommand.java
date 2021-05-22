package doublenegation.mods.compactores.debug;

import net.minecraft.command.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;

public class CompactOresCommand {
    
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("compactores")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("debugworldgen").executes(WorldGenDebugging::executeCommand)));
    }
    
}
