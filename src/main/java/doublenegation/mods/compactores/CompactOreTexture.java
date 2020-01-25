package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CompactOreTexture {

    public static TextureInfo generate(ResourceLocation baseBlock, ResourceLocation baseTexture,
                                       ResourceLocation oreBlock, ResourceLocation oreTexture, int maxOreLayerDiff) {
        try {
            TextureInfo base = TextureInfo.generate(baseBlock, baseTexture);
            TextureInfo ore = TextureInfo.generate(oreBlock, oreTexture);
            // Textures that need to be interpolated by the game don't play well with the texture generation,
            // so interpolate them now
            // (technically wouldn't be necessary if the keyframes are identical (i.e. the frametimes list of both
            //  textures are identical), but I can't be bothered to implement that edge case right now)
            if(base.isInterpolate()) base = interpolateManually(base);
            if(ore.isInterpolate()) ore = interpolateManually(ore);
            // Calculate the factors for scaling the textures to the same resolution (lowest common multiple).
            int baseWidth = base.getWidth();
            int baseHeight = base.getHeight();
            int oreWidth = ore.getWidth();
            int oreHeight = ore.getHeight();
            int commonWidth = lcm(baseWidth, oreWidth);
            int commonHeight = lcm(baseHeight, oreHeight);
            int baseCommonWidthFactor = commonWidth / baseWidth;
            int baseCommonHeightFactor = commonHeight / baseHeight;
            int baseFactor = lcm(baseCommonWidthFactor, baseCommonHeightFactor);
            int oreFactor = baseFactor * baseWidth / oreWidth;
            // Verify that the aspect ratios of the images actually match
            // It wouldn't make a lot of sense to try combining them otherwise
            if(baseFactor * baseHeight != oreFactor * oreHeight) {
                throw new RuntimeException("Aspect ratio mismatch (oreFactor=" + oreFactor + ", baseFactor=" + baseFactor +
                        ", baseDimensions=" + baseWidth + "x" + baseHeight + ", oreDimensions=" + oreWidth + "x" + oreHeight + ")");
            }
            // Scale the textures
            base = scale(base, baseFactor);
            ore = scale(ore, oreFactor);
            // Animation times might not match up - repeat the animations so they match properly
            int animBase = base.getTotalAnimationTime();
            int animOre = ore.getTotalAnimationTime();
            if(animBase != 0 && animOre != 0) {
                int animCommon = lcm(animBase, animOre);
                base = repeatAnimation(base, animCommon / animBase);
                ore = repeatAnimation(ore, animCommon / animOre);
            }
            // Finally generate the new texture
            return generateCompactTexture(base, ore, maxOreLayerDiff);
        } catch(Exception e) {
            throw new RuntimeException("Unable to generate compact ore texture (baseBlock=" + baseBlock +
                    ", oreBlock=" + oreBlock + ", baseTexture=" + baseTexture + ", oreTexture=" + oreTexture + ")", e);
        }
    }

    private static TextureInfo interpolateManually(TextureInfo texture) {
        List<BufferedImage> newImages = new ArrayList<>();
        int currentMasterIndex = 0;
        int currentMasterTime = texture.getFrametimes().get(0);
        int currentMasterPreviousTime = 0;
        int targetFrames = texture.getTotalAnimationTime();
        for(int currentFrame = 0; currentFrame < targetFrames; currentFrame++) {
            if(currentMasterTime + currentMasterPreviousTime == currentFrame) {
                currentMasterPreviousTime += currentMasterTime;
                currentMasterTime = texture.getFrametimes().get(++currentMasterIndex);
            }
            BufferedImage beforeTexture = texture.getTextures().get(currentMasterIndex);
            BufferedImage afterTexture = texture.getTextures()
                    .get(currentMasterIndex + 1 < texture.getTextures().size() ? currentMasterIndex + 1 : 0);
            double factor = (double)(currentFrame - currentMasterPreviousTime) / currentMasterTime;
            newImages.add(makeInterpolatedImage(factor, beforeTexture, afterTexture));
        }
        Integer[] newFrametimesArray = new Integer[newImages.size()];
        Arrays.fill(newFrametimesArray, 1);
        List<Integer> newFrametimes = Arrays.asList(newFrametimesArray);
        return new TextureInfo(texture.getTextureOwner(), newImages, newFrametimes, false);
    }

    private static BufferedImage makeInterpolatedImage(double factor, BufferedImage from, BufferedImage to) {
        int w = from.getWidth(), h = from.getHeight();
        WritableRaster fromRaster = from.getRaster(), toRaster = to.getRaster();
        int[] dataBufferFrom = new int[fromRaster.getNumBands()];
        int[] dataBufferTo = new int[toRaster.getNumBands()];
        BufferedImage result = new BufferedImage(w, h, from.getType());
        WritableRaster resultRaster = result.getRaster();
        int[] dataBufferResult = new int[resultRaster.getNumBands()];
        if(dataBufferResult.length != dataBufferFrom.length || dataBufferFrom.length != dataBufferTo.length)
            throw new RuntimeException("When interpolating: Buffer array size mismatch");
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                fromRaster.getPixel(x, y, dataBufferFrom);
                toRaster.getPixel(x, y, dataBufferTo);
                for(int i = 0; i < dataBufferResult.length; i++) {
                    dataBufferResult[i] = interpolateColor(factor, dataBufferTo[i], dataBufferFrom[i]);
                }
                resultRaster.setPixel(x, y, dataBufferResult);
            }
        }
        return result;
    }

    /* Copied from net.minecraft.client.renderer.TextureAtlasSprite */
    private static int interpolateColor(double factor, int to, int from) {
        return (int)(factor * (double)to + (1.0D - factor) * (double)from);
    }

    private static TextureInfo scale(TextureInfo texture, int factor) {
        if(factor == 1) return texture;
        List<BufferedImage> scaledImages = new ArrayList<>(texture.getTextures().size());
        for(BufferedImage image : texture.getTextures()) {
            int w = image.getWidth(), h = image.getHeight();
            BufferedImage newImage = new BufferedImage(w * factor, h * factor, image.getType());
            WritableRaster oldRaster = image.getRaster();
            WritableRaster newRaster = newImage.getRaster();
            int[] pixelData = new int[oldRaster.getNumBands()];
            for(int x = 0; x < w; x++) {
                for(int y = 0; y < h; y++) {
                    oldRaster.getPixel(x, y, pixelData);
                    for(int fx = 0; fx < factor; fx++) {
                        for(int fy = 0; fy < factor; fy++) {
                            newRaster.setPixel(x * factor + fx, y * factor + fy, pixelData);
                        }
                    }
                }
            }
            scaledImages.add(newImage);
        }
        return new TextureInfo(texture.getTextureOwner(), scaledImages, texture.getFrametimes(), texture.isInterpolate());
    }

    private static TextureInfo repeatAnimation(TextureInfo texture, int numRepeats) {
        if(numRepeats == 1) return texture;
        List<BufferedImage> oldTextures = texture.getTextures();
        List<Integer> oldFrametimes = texture.getFrametimes();
        List<BufferedImage> newTextures = new ArrayList<>();
        List<Integer> newFrametimes = new ArrayList<>();
        for(int i = 0; i < numRepeats; i++) {
            newTextures.addAll(oldTextures);
            newFrametimes.addAll(oldFrametimes);
        }
        return new TextureInfo(texture.getTextureOwner(), newTextures, newFrametimes, texture.isInterpolate());
    }

    private static TextureInfo generateCompactTexture(TextureInfo base, TextureInfo ore, int maxOreLayerDiff) {
        int animBase = base.getTotalAnimationTime();
        int animOre = ore.getTotalAnimationTime();
        ResourceLocation finalTextureOwner = new ResourceLocation("compactores",
                "compact_" + ore.getTextureOwner().getNamespace() + "_" + ore.getTextureOwner().getPath());
        if(animBase == 0 && animOre == 0) {
            return new TextureInfo(finalTextureOwner,
                    Collections.singletonList(actuallyFinallyMakeTheTexture(base.getTextures().get(0), ore.getTextures().get(0), maxOreLayerDiff)),
                    Collections.singletonList(0), false);
        }
        int numFrames = Math.max(animBase, animOre);
        int baseFrame = 0;
        int oreFrame = 0;
        int baseFramePrev = -1;
        int oreFramePrev = -1;
        int baseFrametime = animBase == 0 ? Integer.MAX_VALUE : base.getFrametimes().get(0);
        int oreFrametime = animOre == 0 ? Integer.MAX_VALUE : ore.getFrametimes().get(0);
        int frametimeCounter = 0;
        List<BufferedImage> finalTextures = new ArrayList<>(numFrames);
        List<Integer> finalFrametimes = new ArrayList<>();
        for(int i = 0; i < numFrames; i++) {
            if(baseFrame != baseFramePrev || oreFrame != oreFramePrev) {
                finalTextures.add(actuallyFinallyMakeTheTexture(base.getTextures().get(baseFrame), ore.getTextures().get(oreFrame), maxOreLayerDiff));
                baseFramePrev = baseFrame;
                oreFramePrev = oreFrame;
            }
            frametimeCounter++;
            if(--baseFrametime == 0 && i + 1 < numFrames) {
                baseFrame++;
                baseFrametime = base.getFrametimes().get(baseFrame);
                if(frametimeCounter > 0) {
                    finalFrametimes.add(frametimeCounter);
                    frametimeCounter = 0;
                }
            }
            if(--oreFrametime == 0 && i + 1 < numFrames) {
                oreFrame++;
                oreFrametime = ore.getFrametimes().get(oreFrame);
                if(frametimeCounter > 0) {
                    finalFrametimes.add(frametimeCounter);
                    frametimeCounter = 0;
                }
            }
        }
        finalFrametimes.add(frametimeCounter);
        return new TextureInfo(finalTextureOwner, finalTextures, finalFrametimes, false);
    }

    private static BufferedImage actuallyFinallyMakeTheTexture(BufferedImage base, BufferedImage ore, int maxOreLayerDiff) {
        int w = base.getWidth(), h = base.getHeight();
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

    private static int gcd(int a, int b) {
        if(a == b) return a;
        else if(a > b) return gcd(a - b, b);
        else return gcd(a, b - a);
    }

    private static int lcm(int a, int b) {
        return a * b / gcd(a, b);
    }

    public static class TextureInfo {
        private ResourceLocation textureOwner;
        private List<BufferedImage> textures;
        private List<Integer> frametimes;
        private boolean interpolate;
        public TextureInfo(ResourceLocation textureOwner, List<BufferedImage> textures,
                           List<Integer> frametimes, boolean interpolate) {
            CompactOres.LOGGER.debug("Creating texture for " + textureOwner + " with " + textures.size() + " textures and " + frametimes.size() + " frametimes.");
            this.textureOwner = textureOwner;
            this.textures = textures;
            this.frametimes = frametimes;
            this.interpolate = interpolate;
        }
        public ResourceLocation getTextureOwner() {
            return textureOwner;
        }
        public List<BufferedImage> getTextures() {
            return textures;
        }
        public List<Integer> getFrametimes() {
            return frametimes;
        }
        public boolean isInterpolate() {
            return interpolate;
        }
        public int getTotalAnimationTime() {
            return frametimes.stream().mapToInt(t -> t).sum();
        }
        public int getWidth() {
            return textures.get(0).getWidth();
        }
        public int getHeight() {
            return textures.get(0).getHeight();
        }
        public BufferedImage generateImage() {
            int imgHeight = getHeight();
            BufferedImage img = new BufferedImage(getWidth(), imgHeight * textures.size(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            for(int i = 0; i < textures.size(); i++) {
                g.drawImage(textures.get(i), 0, i * imgHeight, null);
            }
            return img;
        }
        public JsonObject generateMeta() {
            JsonObject meta = new JsonObject();
            JsonObject animation = new JsonObject();
            animation.addProperty("interpolate", interpolate);
            animation.addProperty("width", getWidth());
            animation.addProperty("height", getHeight());
            JsonArray frames = new JsonArray();
            for(int i = 0; i < frametimes.size(); i++) {
                JsonObject frame = new JsonObject();
                frame.addProperty("index", i);
                frame.addProperty("time", frametimes.get(i));
                frames.add(frame);
            }
            animation.add("frames", frames);
            meta.add("animation", animation);
            return meta;
        }
        public static TextureInfo generate(ResourceLocation textureOwner, ResourceLocation texture) throws IOException {
            ResourceLocation metaLocation = new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta");
            IResourceManager rm = Minecraft.getInstance().getResourceManager();
            IResource texRes = rm.getResource(texture);
            BufferedImage tex = ImageIO.read(texRes.getInputStream());
            ArrayList<BufferedImage> textures = new ArrayList<>(1);
            textures.add(tex);
            ArrayList<Integer> frametimes = new ArrayList<>(1);
            frametimes.add(0);
            boolean interpolate = false;
            Number width = 1, height = 1;
            int frametime = 1;
            try {
                JsonObject meta = new JsonParser().parse(new InputStreamReader(rm.getResource(metaLocation).getInputStream())).getAsJsonObject();
                if(!meta.has("animation") || !(meta.get("animation") instanceof JsonObject)) throw new ClassCastException();
                JsonObject animation = meta.getAsJsonObject("animation");
                if(animation.has("interpolate") && animation.get("interpolate").isJsonPrimitive() &&
                        animation.get("interpolate").getAsJsonPrimitive().isBoolean()) {
                    interpolate = animation.get("interpolate").getAsBoolean();
                }
                if(animation.has("width") && animation.get("width").isJsonPrimitive() &&
                        animation.get("width").getAsJsonPrimitive().isNumber() &&
                        animation.has("height") && animation.get("height").isJsonPrimitive() &&
                        animation.get("height").getAsJsonPrimitive().isNumber()) {
                    width = animation.get("width").getAsNumber();
                    height = animation.get("height").getAsNumber();
                }
                if(animation.has("frametime") && animation.get("frametime").isJsonPrimitive() &&
                        animation.get("frametime").getAsJsonPrimitive().isNumber()) {
                    frametime = animation.get("frametime").getAsInt();
                }
                if(animation.has("frames") && animation.get("frames").isJsonArray()) {
                    JsonArray frames;
                    BufferedImage originalImage;
                    int frameWidth, frameHeight;
                    frames = animation.getAsJsonArray("frames");
                    originalImage = textures.remove(0);
                    frametimes.remove(0);
                    frameWidth = originalImage.getWidth();
                    frameHeight = (int) Math.round(frameWidth * height.doubleValue() / width.doubleValue());
                    for(JsonElement entry : frames) {
                        int index = 0, time = frametime;
                        if(entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isNumber()) {
                            index = entry.getAsInt();
                        } else if(entry.isJsonObject()) {
                            JsonObject frameInfo = entry.getAsJsonObject();
                            if(frameInfo.has("index") && frameInfo.get("index").isJsonPrimitive() &&
                                    frameInfo.getAsJsonPrimitive("index").isNumber()) {
                                index = frameInfo.get("index").getAsInt();
                            }
                            if(frameInfo.has("time") && frameInfo.get("time").isJsonPrimitive() &&
                                    frameInfo.getAsJsonPrimitive("time").isNumber()) {
                                time = frameInfo.get("time").getAsInt();
                            }
                        }
                        frametimes.add(time);
                        int y = index * frameHeight;
                        textures.add(originalImage.getSubimage(0, y, frameWidth, frameHeight));
                    }
                }
                return new TextureInfo(textureOwner, textures, frametimes, interpolate);
            } catch(IOException | ClassCastException e) {
                return new TextureInfo(textureOwner, textures, frametimes, interpolate);
            }
        }
    }

}
