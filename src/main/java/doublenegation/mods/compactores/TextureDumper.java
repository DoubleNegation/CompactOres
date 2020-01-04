package doublenegation.mods.compactores;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileOutputStream;

public class TextureDumper {

    public static void dump(CompactOre ore, byte[] texture) {
        try {
            File dir = new File(Minecraft.getInstance().gameDir, "compactores_texture_dumps");
            if(!dir.exists()) dir.mkdir();
            File fname = new File(dir, ore.getRegistryName().getPath() + "_" + System.currentTimeMillis() + ".png");
            try(FileOutputStream fos = new FileOutputStream(fname)) {
                fos.write(texture);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
