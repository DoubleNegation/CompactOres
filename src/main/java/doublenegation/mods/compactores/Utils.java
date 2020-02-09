package doublenegation.mods.compactores;

import net.minecraft.util.ResourceLocation;

public class Utils {

    public static ResourceLocation parseResourceLocation(String str) {
        if(!str.contains(":")) return new ResourceLocation("minecraft", str);
        String namespace = str.split(":")[0];
        String path = str.substring(namespace.length() + 1);
        return new ResourceLocation(namespace, path);
    }

}
