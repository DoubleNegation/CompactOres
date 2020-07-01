package doublenegation.mods.compactores;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static ResourceLocation parseResourceLocation(String str) {
        if(!str.contains(":")) return new ResourceLocation("minecraft", str);
        String namespace = str.split(":")[0];
        String path = str.substring(namespace.length() + 1);
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parseResourceLocationExtra(String str, String context) {
        if(str == null) return null;
        if(!str.contains(":")) return new ResourceLocation("minecraft", str);
        String namespace = str.split(":") [0];
        String path = str.substring(namespace.length() + 1);
        if(namespace.length() == 0) return new ResourceLocation(context, path);
        return new ResourceLocation(namespace, path);
    }

    public static BufferedImage loadImage(InputStream is) throws IOException {
        // ImageIO behaves in an unexpected way when reading a grayscale PNG.
        // this method works around that behavior and always loads a correct BufferedImage
        // see https://stackoverflow.com/questions/31312645/java-imageio-grayscale-png-issue
        BufferedImage img = ImageIO.read(is);
        ColorSpace colorSpace = img.getColorModel().getColorSpace();
        if(colorSpace instanceof ICC_ColorSpace) {
            ICC_Profile profile = ((ICC_ColorSpace)colorSpace).getProfile();
            if(profile instanceof ICC_ProfileGray) {
                BufferedImage corr = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for(int x = 0; x < img.getWidth(); x++) {
                    for(int y = 0; y < img.getHeight(); y++) {
                        int val = img.getRaster().getSample(x, y, 0);
                        int alpha = 0xff;
                        if(img.getAlphaRaster() != null) {
                            alpha = img.getAlphaRaster().getSample(x, y, 0);
                        }
                        corr.setRGB(x, y, alpha * 0x1000000 + val * 0x10000 + val * 0x100 + val);
                    }
                }
                return corr;
            }
        }
        // An IndexColorModel also causes problems (at least sometimes), re-draw the image if one is used
        if(img.getColorModel() instanceof IndexColorModel) {
            BufferedImage corr = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr = corr.createGraphics();
            gr.drawImage(img, 0, 0, null);
            return corr;
        }
        return img;
    }

    public static class ReturningScreen extends Screen {

        private Screen returnTarget;

        protected ReturningScreen(ITextComponent titleIn) {
            super(titleIn);
        }

        public void setReturnTarget(Screen target) {
            this.returnTarget = target;
        }

        public void returnToPreviousScreen() {
            if(field_230706_i_ != null)
                field_230706_i_.displayGuiScreen(returnTarget);
        }

    }

}
