package doublenegation.mods.compactores.debug;

import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
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

    private static void onClientChat(ClientChatEvent event) {
        if(event.getMessage().replaceAll(" +", " ").equals("/compactores texturedump")) {
            event.setCanceled(true);
            Minecraft.getInstance().gui.getChat().addRecentChat(event.getMessage());
            File dumpsDir = new File(Minecraft.getInstance().gameDirectory, "compactores_texture_dumps");
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
                    try (InputStream is = CompactOres.getGeneratedResource(PackType.CLIENT_RESOURCES,
                                new ResourceLocation(CompactOres.MODID, "textures/" + ore.name().getPath() + ".png"));
                         FileOutputStream fos = new FileOutputStream(new File(subdir, ore.getSerializedName() + ".png"))) {
                        while((read = is.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                    }
                }
                // show success message
                LocalPlayer player = Minecraft.getInstance().player;
                if(player != null) {
                    final File subdir_ = subdir;
                    player.sendMessage(new TranslatableComponent("commands.compactores.texturedump.success",
                                new TextComponent(subdir_.getAbsolutePath()).withStyle(ChatFormatting.UNDERLINE)
                                    .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, subdir_.getAbsolutePath())))),
                            player.getUUID());
                }
            } catch(IOException e) {
                error();
                LOGGER.error("Failed to write texture dump", e);
            }
        }
    }
    
    private static void error() {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        player.sendMessage(new TranslatableComponent("commands.compactores.texturedump.failure").withStyle(ChatFormatting.RED), player.getUUID());
    }
    
}
