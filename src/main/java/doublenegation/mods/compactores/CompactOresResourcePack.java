package doublenegation.mods.compactores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.util.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CompactOresResourcePack implements IPackFinder {

    private InMemoryResourcePack pack;

    public CompactOresResourcePack(Map<ResourceLocation, CompactOre> ores) {
        Map<String, byte[]> resPack = new HashMap<>();
        // pack.mcmeta start
        JsonObject packmcmeta = new JsonObject();
        JsonObject packmcmetapack = new JsonObject();
        packmcmetapack.addProperty("pack_format", 4);
        packmcmetapack.addProperty("description", "CompactOres dynamic resources");
        packmcmeta.add("pack", packmcmetapack);
        resPack.put("pack.mcmeta", packmcmeta.toString().getBytes(StandardCharsets.UTF_8));
        // pack.mcmeta end
        for(CompactOre ore : ores.values()) {
            makeLootTable(resPack, ore);
        }
        pack = new InMemoryResourcePack(resPack);
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
        resourcePack.put("data/compactores/loot_tables/" + ore.getBlock().getRegistryName().getPath() + ".json", table.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> map, ResourcePackInfo.IFactory<T> iFactory) {
        map.put(pack.getName(), ResourcePackInfo.createResourcePack(pack.getName(), false, () -> pack, iFactory, ResourcePackInfo.Priority.TOP));
    }

}
