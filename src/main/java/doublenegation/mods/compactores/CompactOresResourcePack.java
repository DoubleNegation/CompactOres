package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            Map<String, byte[]> resPack = new HashMap<>();
            // pack.mcmeta start
            JsonObject packmcmeta = new JsonObject();
            JsonObject packmcmetapack = new JsonObject();
            packmcmetapack.addProperty("pack_format", 4);
            packmcmetapack.addProperty("description", "CompactOres dynamic resources");
            packmcmeta.add("pack", packmcmetapack);
            resPack.put("pack.mcmeta", packmcmeta.toString().getBytes(StandardCharsets.UTF_8));
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
            resPack.put("pack.png", baos.toByteArray());
            // pack.png end
            for (CompactOre ore : ores.values()) {
                makeLootTable(resPack, ore);
                makeBlockstate(resPack, ore);
                makeBlockModel(resPack, ore);
                makeItemModel(resPack, ore);
            }
            pack = new InMemoryResourcePack(resPack);
        }
        return pack;
    }

    private void makeLootTable(Map<String, byte[]> resourcePack, CompactOre ore) {
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
        entry.addProperty("name", ore.getBaseLootTable().getNamespace() + ":" + ore.getBaseLootTable().getPath());
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);
        resourcePack.put("data/compactores/loot_tables/" + ore.getBlock().getRegistryName().getPath() + ".json",
                table.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void makeBlockstate(Map<String, byte[]> resourcePack, CompactOre ore) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", "compactores:block/" + ore.getBlock().getRegistryName().getPath());
        variants.add("", defaultVariant);
        blockstate.add("variants", variants);
        resourcePack.put("assets/compactores/blockstates/" + ore.getBlock().getRegistryName().getPath() + ".json",
                blockstate.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void makeBlockModel(Map<String, byte[]> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", "compactores:" + ore.getBlock().getRegistryName().getPath());
        model.add("textures", textures);
        resourcePack.put("assets/compactores/models/block/" + ore.getBlock().getRegistryName().getPath() + ".json",
                model.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void makeItemModel(Map<String, byte[]> resourcePack, CompactOre ore) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "compactores:block/" + ore.getBlock().getRegistryName().getPath());
        resourcePack.put("assets/compactores/models/item/" + ore.getBlock().getRegistryName().getPath() + ".json",
                model.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> map, ResourcePackInfo.IFactory<T> iFactory) {
        map.put("CompactOres dynamic resources", ResourcePackInfo.createResourcePack("CompactOres dynamic resources",
                true/*isAlwaysEnabled*/, this::getPack, iFactory, ResourcePackInfo.Priority.BOTTOM));
    }

}
