package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CompactOresResourcePack implements IPackFinder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PACK_NAME = "CompactOres dynamic resources";

    private Supplier<List<CompactOre>> oreListSupplier;
    private Map<String, Supplier<byte[]>> packData;
    private InMemoryResourcePack pack;

    public CompactOresResourcePack(Supplier<List<CompactOre>> oreListSupplier) {
        this.oreListSupplier = oreListSupplier;
    }

    private synchronized InMemoryResourcePack getPack() {
        if(pack == null) {
            packData = new HashMap<>();
            generatePack(packData);
            pack = new InMemoryResourcePack(PACK_NAME, packData, path -> {
                if(!path.endsWith(".mcmeta")) return true;
                return DistExecutor.unsafeRunForDist(() -> () -> {
                    // Client only, server is always false
                    String[] split = path.split("/");
                    String filename = split[split.length - 1];
                    String name = filename.substring(0, filename.length() - ".png.mcmeta".length());
                    CompactOre ore = CompactOres.getForResourceName(name);
                    if(ore == null) return false;
                    ResourceLocation baseTexture = ore.getBaseUnderlyingTexture();
                    ResourceLocation oreTexture = ore.getBaseOreTexture();
                    if(!ore.isGenerateTexture() || baseTexture == null || oreTexture == null) return false;
                    ResourceLocation baseMeta = new ResourceLocation(baseTexture.getNamespace(), baseTexture.getPath() + ".mcmeta");
                    ResourceLocation oreMeta = new ResourceLocation(oreTexture.getNamespace(), oreTexture.getPath() + ".mcmeta");
                    IResourceManager rm = Minecraft.getInstance().getResourceManager();
                    return rm.hasResource(baseMeta) || rm.hasResource(oreMeta);
                }, () -> () -> false);
            });
        }
        return pack;
    }

    private void generatePack(Map<String, Supplier<byte[]>> resPack) {
        List<CompactOre> ores = oreListSupplier.get();
        LOGGER.info("Generating CompactOre resources for " + ores.size() + " compact ore blocks");
        // pack.mcmeta start
        JsonObject packmcmeta = new JsonObject();
        JsonObject packmcmetapack = new JsonObject();
        packmcmetapack.addProperty("pack_format", 4);
        packmcmetapack.addProperty("description", PACK_NAME);
        packmcmeta.add("pack", packmcmetapack);
        final byte[] packmcmetaBytes = packmcmeta.toString().getBytes(StandardCharsets.UTF_8);
        resPack.put("pack.mcmeta", () -> packmcmetaBytes);
        // pack.mcmeta end
        // pack.png start - to prevent crash on opening resource packs menu
        BufferedImage packpng = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(packpng, "PNG", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final byte[] packpngBytes = baos.toByteArray();
        resPack.put("pack.png", () -> packpngBytes);
        // pack.png end
        boolean continueLoading;
        try {
            //continueLoading = CompactOres.COMPACT_ORE.isPresent();
            continueLoading = ores.size() > 0 && ores.get(0).getCompactOreBlock() != null;
        } catch(NullPointerException e) {
            continueLoading = false;
        }
        if(continueLoading) {
            // If this condition fails, some mod probably crashed during CONSTRUCT, causing resource loading
            // without registry initialization. Don't attempt to create this resource pack, because this would
            // crash the game. Without this resource pack, the game will make it to the forge error screen and
            // display the actual error. See #3
            makeOreTags(resPack, ores);
            for (CompactOre ore : ores) {
                if(ore.getBaseBlock() == null || ore.getCompactOreBlock() == null) continue;
                makeLootTable(resPack, ore);
                makeBlockstate(resPack, ore);
                makeBlockModel(resPack, ore);
                makeBlockTexture(resPack, ore);
                makeItemModel(resPack, ore);
            }
        }
    }

    private void makeLootTable(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "block");
        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        JsonObject rolls = new JsonObject();
        rolls.addProperty("min", ore.getMinRolls());
        rolls.addProperty("max", ore.getMaxRolls());
        pool.add("rolls", rolls);
        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "loot_table");
        entry.addProperty("name", ore.getBaseBlock().getLootTable().toString());
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        final byte[] bytes = table.toString().getBytes(StandardCharsets.UTF_8);
        ResourceLocation loc = ore.getCompactOreBlock().getLootTable();
        resourcePack.put("data/" + loc.getNamespace() + "/loot_tables/" + loc.getPath() + ".json", () -> bytes);
    }

    private void makeBlockstate(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", ore.name().getNamespace() + ":block/" + ore.name().getPath());
        variants.add("", variant);
        blockstate.add("variants", variants);
        final byte[] bytes = blockstate.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + ore.name().getNamespace() + "/blockstates/" +
                ore.name().getPath() + ".json", () -> bytes);
    }

    private void makeBlockModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", ore.name().getNamespace() + ":" + ore.name().getPath());
        model.add("textures", textures);
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + ore.name().getNamespace() + "/models/block/" + ore.name().getPath() + ".json",
                () -> bytes);
    }

    private void makeItemModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", ore.name().getNamespace() + ":block/" +ore.name().getPath());
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + ore.name().getNamespace() + "/models/item/" +
                ore.name().getPath() + ".json", () -> bytes);
    }

    private void makeBlockTexture(Map<String, Supplier<byte[]>> resourcePack, final CompactOre ore) {
        if(ore.isGenerateTexture()) {
            resourcePack.put("assets/" + ore.name().getNamespace() + "/textures/" +
                    ore.name().getPath() + ".png", () -> {
                try {
                    CompactOreTexture.TextureInfo info = CompactOreTexture.generate(null, ore.getBaseUnderlyingTexture(),
                            ore.getBaseBlockRegistryName(), ore.getBaseOreTexture(), ore.getMaxOreLayerColorDiff());
                    BufferedImage img = info.generateImage();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "PNG", baos);
                    if ("true".equals(System.getProperty("compactores.dumpTextures"))) {
                        TextureDumper.dump(ore, baos.toByteArray());
                    }
                    return baos.toByteArray();
                } catch (Exception e) {
                    LOGGER.error("Failed to generate compact ore texture for " + ore.name().getPath() + ", using missing texture instead.");
                    logExceptionCauseList(e);
                    // missing texture
                    BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, 16, 16);
                    g.setColor(Color.MAGENTA);
                    g.fillRect(0, 8, 8, 8);
                    g.fillRect(8, 0, 8, 8);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(img, "PNG", baos);
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                    return baos.toByteArray();
                }
            });
        }
        resourcePack.put("assets/" + ore.name().getNamespace() + "/textures/" +
                ore.name().getPath() + ".png.mcmeta", () -> {
            try {
                CompactOreTexture.TextureInfo info = CompactOreTexture.generate(null, ore.getBaseUnderlyingTexture(),
                        ore.getBaseBlockRegistryName(), ore.getBaseOreTexture(), ore.getMaxOreLayerColorDiff());
                return info.generateMeta().toString().getBytes(StandardCharsets.UTF_8);
            } catch(Exception e) {
                LOGGER.error("Failed to generate compact ore texture for " + ore.name().getPath() + ", using missing texture instead.");
                logExceptionCauseList(e);
                throw e;
            }
        });
    }

    private void makeOreTags(Map<String, Supplier<byte[]>> resourcePack, List<CompactOre> ores) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray values = new JsonArray();
        for(CompactOre ore : ores) {
            values.add(ore.name().toString());
        }
        tag.add("values", values);
        final byte[] bytes = tag.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("data/forge/tags/blocks/ores.json", () -> bytes);
        resourcePack.put("data/forge/tags/items/ores.json", () -> bytes);
    }

    private void logExceptionCauseList(Throwable th) {
        do {
            LOGGER.error("   Caused by " + th.getClass().getName() + ": " + th.getMessage());
        } while((th = th.getCause()) != null);
    }

    @Override
    public void func_230230_a_(Consumer<ResourcePackInfo> packConsumer, ResourcePackInfo.IFactory iFactory) {
        packConsumer.accept(ResourcePackInfo.createResourcePack(PACK_NAME, true/*isAlwaysEnabled*/,
                this::getPack, iFactory, ResourcePackInfo.Priority.BOTTOM, IPackNameDecorator.BUILTIN));
    }

}
