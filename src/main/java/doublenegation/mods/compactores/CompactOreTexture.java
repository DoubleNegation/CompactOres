package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

public class CompactOreTexture {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<ResourceLocation, TextureInfo> generatedTextureCache = new HashMap<>();
    private static Map<ResourceLocation, TextureInfo> baseTextureCache = new HashMap<>();
    private static int numTexturesGenerated = 0;
    private static long textureGenerationTime = 0L;

    public static TextureInfo generate(ResourceLocation baseBlock, ResourceLocation baseTexture,
                                       ResourceLocation oreBlock, ResourceLocation oreTexture, int maxOreLayerDiff) {
        if(generatedTextureCache.containsKey(oreBlock)) {
            return generatedTextureCache.get(oreBlock);
        }
        try {
            long generationStart = System.currentTimeMillis();
            TextureInfo base;
            if(baseTextureCache.containsKey(baseTexture)) {
                base = baseTextureCache.get(baseTexture);
            } else {
                base = TextureInfo.generate(baseBlock, baseTexture);
                baseTextureCache.put(baseTexture, base);
            }
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
            TextureInfo result = generateCompactTexture(base, ore, maxOreLayerDiff);
            generatedTextureCache.put(oreBlock, result);
            numTexturesGenerated++;
            textureGenerationTime += (System.currentTimeMillis() - generationStart);
            return result;
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
        ResourceLocation finalTextureOwner = new ResourceLocation(CompactOres.MODID,
                "compactore__" + ore.getTextureOwner().getNamespace() + "__" + ore.getTextureOwner().getPath());
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
        BufferedImage oreLayer;
        if(maxOreLayerDiff < -1) {
            oreLayer = findOreLayerAutoRGBChange(base, ore, w, h);
        } else if(maxOreLayerDiff < 0) {
            oreLayer = findOreLayerExactMatch(base, ore, w, h);
        } else if(maxOreLayerDiff < 1000) {
            oreLayer = findOreLayerAttempt3(base, ore, w, h, maxOreLayerDiff);
        } else {
            oreLayer = findOreLayerRGBChange(base, ore, w, h, maxOreLayerDiff);
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        int xOff = Math.max(1, w / 16), yOff = Math.max(1, h / 16);
        // Start with the background rock
        g.drawImage(base, 0, 0, null);
        // Then add the ore on top
        g.drawImage(oreLayer, 0, 0, null);
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

    private static BufferedImage findOreLayerRGBChange(BufferedImage base, BufferedImage ore, int w, int h, int maxDiff) {
        BufferedImage oreLayer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int rMin = Integer.MAX_VALUE, rMax = Integer.MIN_VALUE,
                gMin = Integer.MAX_VALUE, gMax = Integer.MIN_VALUE,
                bMin = Integer.MAX_VALUE, bMax = Integer.MIN_VALUE;
        int rAvg = 0, gAvg = 0, bAvg = 0, numAvgSamples = 0;
        // find color change ranges and average color in base texture
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                int baseRgb = base.getRGB(x, y);
                int baseR = r(baseRgb), baseG = g(baseRgb), baseB = b(baseRgb);
                int[][] coords = new int[][]{{(x + 1) % w, y}, {x, (y + 1) % h}};
                for(int[] arr : coords) {
                    int rgb = base.getRGB(arr[0], arr[1]);
                    int r = r(rgb), g = g(rgb), b = b(rgb);
                    int dr = Math.abs(baseR - r), dg = Math.abs(baseG - g), db = Math.abs(baseB - b);
                    if(dr < rMin) rMin = dr;
                    if(dr > rMax) rMax = dr;
                    if(dg < gMin) gMin = dg;
                    if(dg > gMax) gMax = dg;
                    if(db < bMin) bMin = db;
                    if(db > bMax) bMax = db;
                }
                rAvg += baseR;
                gAvg += baseG;
                bAvg += baseB;
                numAvgSamples++;
            }
        }
        maxDiff -= 1050;
        rMin -= maxDiff;
        rMax += maxDiff;
        gMin -= maxDiff;
        gMax += maxDiff;
        bMin -= maxDiff;
        bMax += maxDiff;
        rAvg /= numAvgSamples;
        gAvg /= numAvgSamples;
        bAvg /= numAvgSamples;
        // find pixel most similar to average color in ore texture
        int startX = 0, startY = 0, startDiff = Integer.MAX_VALUE;
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                int rgb = ore.getRGB(x, y);
                int r = r(rgb), g = g(rgb), b = b(rgb);
                int diff = Math.abs(rAvg - r) + Math.abs(gAvg - g) + Math.abs(bAvg - b);
                if(diff < startDiff) {
                    startX = x;
                    startY = y;
                }
            }
        }
        // find all connected pixels within the threshold
        boolean[][] baseMap = new boolean[w][h];
        baseMap[startX][startY] = true;
        boolean found = true;
        while(found) {
            found = false;
            for(int x = 0; x < w; x++) {
                for(int y = 0; y < h; y++) {
                    if(baseMap[x][y]) {
                        int baseRgb = ore.getRGB(x, y);
                        int baseR = r(baseRgb), baseG = g(baseRgb), baseB = b(baseRgb);
                        int[][] neighbors = new int[][]{{(x + 1) % w, y}, {x, (y + 1) % h}, {(x - 1 + w) % w, y}, {x, (y - 1 + h) % h}};
                        for(int[] n : neighbors) {
                            if(baseMap[n[0]][n[1]]) continue;
                            int rgb = ore.getRGB(n[0], n[1]);
                            int r = r(rgb), g = g(rgb), b = b(rgb);
                            int dr = Math.abs(baseR - r), dg = Math.abs(baseG - g), db = Math.abs(baseB - b);
                            if(dr >= rMin && dr <= rMax && dg >= gMin && dg <= gMax && db >= bMin && db <= bMax) {
                                baseMap[n[0]][n[1]] = true;
                                found = true;
                            }
                        }
                    }
                }
            }
        }
        // all unconnected pixels are the ore layer
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                if(!baseMap[x][y]) {
                    oreLayer.setRGB(x, y, ore.getRGB(x, y));
                }
            }
        }
        return oreLayer;
    }

    private static BufferedImage findOreLayerAutoRGBChange(BufferedImage base, BufferedImage ore, int w, int h) {
        final double threshold = .5D;
        final int min = 1000, max = 1100;
        IntToDoubleFunction valueForDiff = maxDiff -> {
            BufferedImage oreLayer = findOreLayerRGBChange(base, ore, w, h, maxDiff);
            int px = 0;
            for(int x = 0; x < w; x++) {
                for(int y = 0; y < h; y++) {
                    if((oreLayer.getRGB(x, y) & 0xFF000000) != 0) {
                        px++;
                    }
                }
            }
            return (double)px / (double)(w * h);
        };
        int below = min, above = max;
        // perform a binary search to find the lowest value smaller than threshold
        while(below + 1 < above) {
            int middle = below + (above - below) / 2;
            if(valueForDiff.applyAsDouble(middle) > threshold) {
                below = middle;
            } else {
                above = middle;
            }
        }
        return findOreLayerRGBChange(base, ore, w, h, above);
    }

    private static int r(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private static int g(int rgb) {
        return (rgb >> 8) & 0xFF;
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
            BufferedImage tex = Utils.loadImage(texRes.getInputStream());
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
                    frametimes.remove(0);
                    frametime = animation.get("frametime").getAsInt();
                    frametimes.add(frametime);
                }
                if(animation.has("frames") && animation.get("frames").isJsonArray()) {
                    JsonArray frames;
                    int frameWidth, frameHeight;
                    frames = animation.getAsJsonArray("frames");
                    textures.clear();
                    frametimes.remove(0);
                    frameWidth = tex.getWidth();
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
                        textures.add(tex.getSubimage(0, y, frameWidth, frameHeight));
                    }
                } else {
                    textures.clear();
                    frametimes.clear();
                    int frameWidth = tex.getWidth();
                    int frameHeight = (int) Math.round(frameWidth * height.doubleValue() / width.doubleValue());
                    for(int i = 0; i * frameHeight < tex.getHeight(); i++) {
                        textures.add(tex.getSubimage(0, i * frameHeight, frameWidth, frameHeight));
                        frametimes.add(frametime);
                    }
                }
                return new TextureInfo(textureOwner, textures, frametimes, interpolate);
            } catch(IOException | ClassCastException e) {
                return new TextureInfo(textureOwner, textures, frametimes, interpolate);
            }
        }
        public static TextureInfo generateForEditorRendering(ResourceLocation textureLocation, int maxOreLayerColorDiff) {
            TextureInfo textureInfo;
            if(textureLocation.getNamespace().equals(CompactOres.MODID)) {
                CompactOre ore = CompactOres.getForResourceName(textureLocation.getPath().substring("compactore__".length()));
                if(ore == null) return null;
                try {
                    textureInfo = CompactOreTexture.generate(null, ore.getBaseUnderlyingTexture(),
                            ore.getBaseBlockRegistryName(), ore.getBaseOreTexture(), maxOreLayerColorDiff);
                } catch(RuntimeException e) {
                    LOGGER.warn("Failed to prepare texture for texture editor", e);
                    return null;
                }
                // invalidate caches right away - the next call will almost certainly be with a different diff,
                // and the cache would prevent that from working
                baseTextureCache.clear();
                generatedTextureCache.clear();
            } else {
                try {
                    textureInfo = generate(null, textureLocation);
                } catch(IOException e) {
                    LOGGER.warn("Failed to prepare texture for texture editor", e);
                    return null;
                }
            }
            if(textureInfo.getWidth() < 32) {
                textureInfo = scale(textureInfo, 32 / textureInfo.getWidth());
            }
            if(textureInfo.isInterpolate()) {
                textureInfo = interpolateManually(textureInfo);
            }
            return textureInfo;
        }
    }

    public static void registerCacheInvalidator() {
        ((IReloadableResourceManager)Minecraft.getInstance().getResourceManager()).addReloadListener(
                (ISelectiveResourceReloadListener) (resourceManager, resourcePredicate) -> {
                    if(resourcePredicate.test(VanillaResourceType.TEXTURES)) {
                        // All texture caches are invalidated here immediately AFTER resource loading has COMPLETED.
                        baseTextureCache.clear();
                        generatedTextureCache.clear();
                        LOGGER.info("Generating {} compact ore textures took {} seconds", numTexturesGenerated, (int)Math.round(textureGenerationTime / 1000.));
                        numTexturesGenerated = 0;
                        textureGenerationTime = 0L;
                    }
                }
        );
    }

}
