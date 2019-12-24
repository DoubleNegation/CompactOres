package doublenegation.mods.compactores;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

public class CompactOreTexture {

    public static BufferedImage generate(BufferedImage base, BufferedImage ore, int maxOreLayerDiff) {
        int w, h;
        if((w = base.getWidth()) != ore.getWidth() || (h = base.getHeight()) != ore.getHeight()) {
            CompactOres.LOGGER.error("Unequal ore/underlying texture dimensions - using missing texture instead");
            throw new RuntimeException();
        }
        BufferedImage oreLayer = maxOreLayerDiff < 0 ?
                findOreLayerExactMatch(base, ore, w, h) : findOreLayerAttempt3(base, ore, w, h, maxOreLayerDiff);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        int xOff = Math.max(1, w / 16), yOff = Math.max(1, h / 16);
        // Some mods have ore textures without a background rock, so start by painting the rock
        g.drawImage(base, 0, 0, null);
        // then add the ore on top
        g.drawImage(ore, 0, 0, null);
        g.drawImage(oreLayer, xOff, yOff, null);
        g.drawImage(oreLayer, -xOff, -yOff, null);
        g.drawImage(oreLayer, xOff, 0, null);
        return result;
    }

    private static BufferedImage findOreLayerExactMatch(BufferedImage base, BufferedImage ore, int w, int h) {
        BufferedImage oreLayer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                if(ore.getRGB(x, y) != base.getRGB(x, y)) {
                    oreLayer.setRGB(x, y, ore.getRGB(x, y));
                }
            }
        }
        return oreLayer;
    }

    private static BufferedImage findOreLayerAttempt3(BufferedImage base, BufferedImage ore, int w, int h, int maxDiff) {
        BufferedImage oreLayer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        HashSet<Integer> baseLayerColors = new HashSet<>();
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                baseLayerColors.add(base.getRGB(x, y));
            }
        }
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                int a = ore.getRGB(x, y);
                boolean isRock = false;
                for(int c : baseLayerColors) {
                    int diff = Math.abs(r(a)-r(c))+Math.abs(g(a)-g(c))+Math.abs(b(a)-b(c));
                    if(diff <= maxDiff) {
                        isRock = true;
                        break;
                    }
                }
                if(!isRock) {
                    oreLayer.setRGB(x, y, a);
                }
            }
        }
        return oreLayer;
    }

    private static int r(int rgb) {
        return (rgb / 0x10000) & 0xFF;
    }

    private static int g(int rgb) {
        return (rgb / 0x100) & 0xFF;
    }

    private static int b(int rgb) {
        return rgb & 0xFF;
    }

}
