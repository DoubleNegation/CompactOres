package doublenegation.mods.compactores.compat;

import doublenegation.mods.compactores.CompactOres;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    public JEIPlugin() {
        LOGGER.info("CompactOres JEI Plugin enabled");
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(CompactOres.MODID, CompactOres.MODID);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(CompactOres.COMPACT_ORE_ITEM.get());
    }

}
