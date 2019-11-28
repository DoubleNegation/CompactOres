package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class InjectingLootTableManager extends LootTableManager {

    private static final Method m_apply = ObfuscationReflectionHelper.findMethod(ReloadListener.class, "func_212853_a_", Object.class, IResourceManager.class, IProfiler.class);
    private static final Method m_prepare = ObfuscationReflectionHelper.findMethod(ReloadListener.class, "func_212854_a_", IResourceManager.class, IProfiler.class);
    private static final Method m_getPreparedPath;

    static {
        try {
            m_getPreparedPath = JsonReloadListener.class.getDeclaredMethod("getPreparedPath", ResourceLocation.class);
        } catch(Exception e) { throw new RuntimeException(e); }
    }

    private LootTableManager original;
    private Map<ResourceLocation, LootTable> lootTables;

    public InjectingLootTableManager(LootTableManager original, Map<ResourceLocation, LootTable> lootTables) {
        this.original = original;
        this.lootTables = lootTables;
    }

    @Override
    public LootTable getLootTableFromLocation(ResourceLocation p_186521_1_) {
        if(lootTables.containsKey(p_186521_1_)) return lootTables.get(p_186521_1_);
        return original.getLootTableFromLocation(p_186521_1_);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> p_212853_1_, IResourceManager p_212853_2_, IProfiler p_212853_3_) {
        try {
            m_apply.invoke(original, p_212853_1_, p_212853_2_, p_212853_3_);
        } catch(Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public Set<ResourceLocation> getLootTableKeys() {
        ImmutableSet.Builder<ResourceLocation> b = new ImmutableSet.Builder<>();
        b.addAll(lootTables.keySet());
        b.addAll(original.getLootTableKeys());
        return b.build();
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(IResourceManager p_212854_1_, IProfiler p_212854_2_) {
        try {
            return (Map<ResourceLocation, JsonObject>) m_prepare.invoke(original, p_212854_1_, p_212854_2_);
        } catch(Exception e) { throw new RuntimeException(e); }
    }

    @Override
    protected ResourceLocation getPreparedPath(ResourceLocation p_getPreparedPath_1_) {
        try {
            return (ResourceLocation) m_getPreparedPath.invoke(original, p_getPreparedPath_1_);
        } catch(Exception e) { throw new RuntimeException(e); }
    }
}
