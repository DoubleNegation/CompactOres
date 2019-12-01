package doublenegation.mods.compactores;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class CompactOreTexture {

    public static BufferedImage generate(BufferedImage base, BufferedImage ore) {
        int w, h;
        if((w = base.getWidth()) != ore.getWidth() || (h = base.getHeight()) != ore.getHeight()) {
            CompactOres.LOGGER.error("Unequal ore/underlying texture dimensions - using missing texture instead");
            throw new RuntimeException();
        }
        BufferedImage oreLayer = new BufferedImage(ore.getWidth(), ore.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                if(ore.getRGB(x, y) != base.getRGB(x, y)) {
                    oreLayer.setRGB(x, y, ore.getRGB(x, y));
                }
            }
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(base, 0, 0, null);
        g.drawImage(oreLayer, 0, 0, null);
        g.drawImage(oreLayer, 1, 1, null);
        g.drawImage(oreLayer, -1, -1, null);
        g.drawImage(oreLayer, 1, 0, null);
        return result;
    }

}
