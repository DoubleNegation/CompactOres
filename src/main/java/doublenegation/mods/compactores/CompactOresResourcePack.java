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
            packmcmetapack.addProperty("description", "CompactOres dynamic resources");
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
                makeLootTable(resPack, ore);
                makeBlockstate(resPack, ore);
                makeBlockModel(resPack, ore);
                makeItemModel(resPack, ore);
                makeBlockTexture(resPack, ore);
            }
            pack = new InMemoryResourcePack(resPack);
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
        resourcePack.put("data/compactores/loot_tables/" + ore.getBlock().getRegistryName().getPath() + ".json", () -> bytes);
    }

    private void makeBlockstate(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", "compactores:block/" + ore.getBlock().getRegistryName().getPath());
        variants.add("", defaultVariant);
        blockstate.add("variants", variants);
        final byte[] bytes = blockstate.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/compactores/blockstates/" + ore.getBlock().getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeBlockModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", "compactores:" + ore.getBlock().getRegistryName().getPath());
        model.add("textures", textures);
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/compactores/models/block/" + ore.getBlock().getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeItemModel(Map<String, Supplier<byte[]>> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "compactores:block/" + ore.getBlock().getRegistryName().getPath());
        final byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
        resourcePack.put("assets/compactores/models/item/" + ore.getBlock().getRegistryName().getPath() + ".json",
                () -> bytes);
    }

    private void makeBlockTexture(Map<String, Supplier<byte[]>> resourcePack, final CompactOre ore) {
        resourcePack.put("assets/compactores/textures/" + ore.getBlock().getRegistryName().getPath() + ".png", () -> {
            try {
                IResourceManager rm = Minecraft.getInstance().getResourceManager();
                IResource baseOreTexture = rm.getResource(ore.getBaseOreTexture());
                IResource baseUnderlyingTexture = rm.getResource(ore.getBaseUnderlyingTexture());
                BufferedImage baseOre = ImageIO.read(baseOreTexture.getInputStream());
                BufferedImage baseRock = ImageIO.read(baseUnderlyingTexture.getInputStream());
                BufferedImage result = CompactOreTexture.generate(baseRock, baseOre, ore.getMaxOreLayerColorDiff());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(result, "PNG", baos);
                } catch(Exception e){e.printStackTrace();}
                if("true".equals(System.getProperty("compactores.dumpTextures"))) {
                    TextureDumper.dump(ore, baos.toByteArray());
                }
                return baos.toByteArray();
            } catch(Exception e) {
                CompactOres.LOGGER.error("Failed to generate texture for " + ore.getBlock().getRegistryName() +
                        ", using missing texture instead: " + e.getClass().getName() + ": " + e.getMessage());
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
                } catch(Exception ex) {ex.printStackTrace();}
                return baos.toByteArray();
            }
        });
    }

    @Override
    public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> map, ResourcePackInfo.IFactory<T> iFactory) {
        map.put("CompactOres dynamic resources", ResourcePackInfo.createResourcePack("CompactOres dynamic resources",
                true/*isAlwaysEnabled*/, this::getPack, iFactory, ResourcePackInfo.Priority.BOTTOM));
    }

}
