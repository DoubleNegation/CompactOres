package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextureDumper {

    private static final Logger LOGGER = LogManager.getLogger();
    
    static void init() {
        MinecraftForge.EVENT_BUS.addListener(TextureDumper::onClientChat);
    }

    public static int executeCommandServer(CommandContext<CommandSource> ctx) {
        // This will only happen if the player hasn't enabled debugging.
        // If debugging is enabled, the ClientChatEvent handler stops the command from going to the server
        // and handles the texture dumping client-side.
        ctx.getSource().sendErrorMessage(new TranslationTextComponent("commands.compactores.debugging_disabled"));
        return 0;
    }

    private static void onClientChat(ClientChatEvent event) {
        if(event.getMessage().replaceAll(" +", " ").equals("/compactores texturedump")) {
            event.setCanceled(true);
            Minecraft.getInstance().ingameGUI.getChatGUI().addToSentMessages(event.getMessage());
            File dumpsDir = new File(Minecraft.getInstance().gameDir, "compactores_texture_dumps");
            if((dumpsDir.exists() && !dumpsDir.isDirectory()) || (!dumpsDir.exists() && !dumpsDir.mkdir())) {
                error();
                LOGGER.error("Can not write texture dump: failed to create compactores_texture_dumps directory");
                return;
            }
            long subdirId = System.currentTimeMillis();
            File subdir;
            while((subdir = new File(dumpsDir, "" + (subdirId))).exists()) {
                subdirId++;
            }
            if(!subdir.mkdir()) {
                error();
                LOGGER.error("Can not write texture dump: failed to create directory for dump");
                return;
            }
            try {
                int read;
                byte[] buf = new byte[4096];
                for (CompactOre ore : CompactOres.compactOres()) {
                    try (InputStream is = CompactOres.getGeneratedResource(ResourcePackType.CLIENT_RESOURCES, 
                                new ResourceLocation(CompactOres.MODID, "textures/" + ore.name().getPath() + ".png"));
                            FileOutputStream fos = new FileOutputStream(new File(subdir, ore.getString() + ".png"))) {
                        while((read = is.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                    }
                }
                // show success message
                ClientPlayerEntity player = Minecraft.getInstance().player;
                if(player != null) {
                    final File subdir_ = subdir;
                    player.sendMessage(new TranslationTextComponent("commands.compactores.texturedump.success", 
                                new StringTextComponent(subdir_.getAbsolutePath()).mergeStyle(TextFormatting.UNDERLINE)
                                    .modifyStyle(s -> s.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, subdir_.getAbsolutePath())))), 
                            player.getUniqueID());
                }
            } catch(IOException e) {
                error();
                LOGGER.error("Failed to write texture dump", e);
            }
        }
    }
    
    private static void error() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if(player == null) return;
        player.sendMessage(new TranslationTextComponent("commands.compactores.texturedump.failure").mergeStyle(TextFormatting.RED), player.getUniqueID());
    }
    
}
