package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.event.RegisterCommandsEvent;

public class CompactOresCommand {
    
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("compactores")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("debugworldgen").executes(WorldGenDebugging::executeCommand))
                .then(Commands.literal("genoretester").executes(OreTester::executeCommand))
                .then(Commands.literal("texturedump").executes(CompactOresCommand::executeClientSideCommand))
                .then(Commands.literal("textureeditor").executes(CompactOresCommand::executeClientSideCommand)));
    }

    public static int executeClientSideCommand(CommandContext<CommandSourceStack> ctx) {
        // This will only happen if the player hasn't enabled debugging.
        // If debugging is enabled, one of the ClientChatEvent handlers stops the command from going to the server
        // and handles the command client-side.
        ctx.getSource().sendFailure(new TranslatableComponent("commands.compactores.debugging_disabled"));
        return 0;
    }
    
}
