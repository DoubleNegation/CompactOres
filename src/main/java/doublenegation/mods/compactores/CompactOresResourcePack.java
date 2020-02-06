package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.*;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CompactOresResourcePack implements IPackFinder {

    private static final String PACK_NAME = "CompactOres dynamic resources";

    private Supplier<Map<ResourceLocation, CompactOre>> oreListSupplier;
    private InMemoryResourcePack pack;

    public CompactOresResourcePack(Supplier<Map<ResourceLocation, CompactOre>> oreListSupplier) {
        this.oreListSupplier = oreListSupplier;
    }

    private synchronized InMemoryResourcePack getPack() {
        if(pack == null) {
            Map<ResourceLocation, CompactOre> ores = oreListSupplier.get();
            CompactOres.LOGGER.info("Generating CompactOre resources for " + ores.size() + " compact ore blocks");
            Map<String, Supplier<byte[]>> resPack = new HashMap<>();
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
            Graphics2D packpngg = packpng.createGraphics();
            packpngg.setColor(Color.BLACK);
            packpngg.fillRect(0, 0, 16, 16);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(packpng, "PNG", baos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final byte[] packpngBytes = baos.toByteArray();
            resPack.put("pack.png", () -> packpngBytes);
            // pack.png end
            makeTags(resPack, ores.values());
            for (CompactOre ore : ores.values()) {
                if(ore.getBlock() == null) {
                    // Some mod probably crashed during CONSTRUCT, causing resource loading without registry
                    // initialization. Don't attempt to create this resource pack, because this would crash.
                    // Without this resource pack, the game will make it to the forge error screen and display the
                    // actual error. See #3
                    continue;
                }
                makeLootTable(resPack, ore);
                makeBlockstate(resPack, ore);
                makeBlockModel(resPack, ore);
                makeItemModel(resPack, ore);
                makeBlockTexture(resPack, ore);
            }
            pack = new InMemoryResourcePack(PACK_NAME, resPack, path -> {
                if(!path.endsWith(".mcmeta")) return true;
                String[] split = path.split("/");
                String filename = split[split.length - 1];
                String name = filename.substring(0, filename.length() - ".png.mcmeta".length());
                ResourceLocation loc = new ResourceLocation(CompactOres.MODID, name);
                CompactOre ore = CompactOres.getFor(loc);
                ResourceLocation baseTexture = ore.getBaseUnderlyingTexture();
                ResourceLocation oreTexture = ore.getBaseOreTexture();
                ResourceLocation baseMeta = new ResourceLocation(baseTexture.getNamespace(), baseTexture.getPath() + ".mcmeta");
                ResourceLocation oreMeta = new ResourceLocation(oreTexture.getNamespace(), oreTexture.getPath() + ".mcmeta");
                IResourceManager rm = Minecraft.getInstance().getResourceManager();
                return rm.hasResource(baseMeta) || rm.hasResource(oreMeta);
            });
        }
        return pack;
    }

    private void makeTags(Map<String, Supplier<byte[]>> resourcePack, Collection<CompactOre> ores) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray values = new JsonArray();
        for(CompactOre ore : ores) {
            values.add(ore.getRegistryName().toString());
        }
        tag.add("values", values);
        final byte[] bytes = tag.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("data/forge/tags/blocks/ores.json", () -> bytes);
        resourcePack.put("data/forge/tags/items/ores.json", () -> bytes);
    }

    private void makeLootTable(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        final String namespace = ore.getRegistryName().getNamespace();
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
        // This /should/ only happen after the registry events, let's just hope that that is always the case...
        ResourceLocation baseLootTable = ore.getBaseBlock().getLootTable();
        entry.addProperty("name", baseLootTable.toString());
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        final byte[] bytes = table.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("data/" + namespace + "/loot_tables/" + ore.getRegistryName().getPath() + ".json", () -> bytes);
    }

    private void makeBlockstate(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        final String namespace = ore.getRegistryName().getNamespace();
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", namespace + ":block/" + ore.getRegistryName().getPath());
        variants.add("", defaultVariant);
        blockstate.add("variants", variants);
        final byte[] bytes = blockstate.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + namespace + "/blockstates/" + ore.getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeBlockModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        final String namespace = ore.getRegistryName().getNamespace();
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", namespace + ":" + ore.getRegistryName().getPath());
        model.add("textures", textures);
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + namespace + "/models/block/" + ore.getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeItemModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        final String namespace = ore.getRegistryName().getNamespace();
        JsonObject model = new JsonObject();
        model.addProperty("parent", namespace + ":block/" + ore.getRegistryName().getPath());
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/" + namespace + "/models/item/" + ore.getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeBlockTexture(Map<String, Supplier<byte[]>> resourcePack, final CompactOre ore) {
        final String namespace = ore.getRegistryName().getNamespace();
        resourcePack.put("assets/" + namespace + "/textures/" + ore.getRegistryName().getPath() + ".png", () -> {
            try {
                CompactOreTexture.TextureInfo info = CompactOreTexture.generate(null, ore.getBaseUnderlyingTexture(),
                        ore.getBaseBlock().getRegistryName(), ore.getBaseOreTexture(), ore.getMaxOreLayerColorDiff());
                BufferedImage img = info.generateImage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                if("true".equals(System.getProperty("compactores.dumpTextures"))) {
                    TextureDumper.dump(ore, baos.toByteArray());
                }
                return baos.toByteArray();
            } catch(Exception e) {
                CompactOres.LOGGER.error("Failed to generate compact ore texture for " + ore.getRegistryName() + ", using missing texture instead.");
                Throwable ex = e;
                do {
                    CompactOres.LOGGER.error("   Caused by " + ex.getClass().getName() + ": " + ex.getMessage());
                } while((ex = ex.getCause()) != null);
                e.printStackTrace();
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
                } catch(Exception exc) {exc.printStackTrace();}
                return baos.toByteArray();
            }
        });
        resourcePack.put("assets/" + namespace + "/textures/" + ore.getRegistryName().getPath() + ".png.mcmeta", () -> {
            try {
                CompactOreTexture.TextureInfo info = CompactOreTexture.generate(null, ore.getBaseUnderlyingTexture(),
                        ore.getBaseBlock().getRegistryName(), ore.getBaseOreTexture(), ore.getMaxOreLayerColorDiff());
                return info.generateMeta().toString().getBytes(StandardCharsets.UTF_8);
            } catch(Exception e) {
                CompactOres.LOGGER.error("Failed to generate compact ore texture for " + ore.getRegistryName() + ", using missing texture instead.");
                Throwable ex = e;
                do {
                    CompactOres.LOGGER.error("   Caused by " + ex.getClass().getName() + ": " + ex.getMessage());
                } while((ex = ex.getCause()) != null);
                e.printStackTrace();
                throw e;
            }
        });
    }

    @Override
    public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> map, ResourcePackInfo.IFactory<T> iFactory) {
        map.put(PACK_NAME, ResourcePackInfo.createResourcePack(PACK_NAME,
                true/*isAlwaysEnabled*/, this::getPack, iFactory, ResourcePackInfo.Priority.BOTTOM));
    }

}
