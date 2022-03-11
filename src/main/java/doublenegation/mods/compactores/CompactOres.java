package doublenegation.mods.compactores;

import doublenegation.mods.compactores.config.ConfigLoader;
import doublenegation.mods.compactores.debug.CompactOresDebugging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Mod(CompactOres.MODID)
public class CompactOres
{
    public static final String MODID = "compactores";
    public static final Logger LOGGER = LogManager.getLogger();

    private static List<CompactOre> compactOres;
    private static CompactOresResourcePack resourcePack;

    private static Utils.ReturningScreen loadFinishScreen;
    private static Timer loadFinishTimer;

    public CompactOres() {
        // Register all event listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::startServer);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBroken);

        // Load the config
        compactOres = ConfigLoader.loadOres();

        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(CompactOres::compactOres);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackRepository().addPackFinder(resourcePack);
            CompactOreTexture.registerCacheInvalidator();
        });

        // Initialize ore excavation integration without risking classloading if ore excavation is not present
        if(ModList.get().isLoaded("oreexcavation")) {
            try {
                Class<?> clazz = Class.forName("doublenegation.mods.compactores.compat.OreExcavationIntegration");
                clazz.getDeclaredMethod("init").invoke(null);
            } catch(Exception e) {
                LOGGER.error("Failed to initialize OreExcavation integration", e);
            }
        }
        
        // initialize debugging (if it is enabled)
        CompactOresDebugging.init();

    }

    public static List<CompactOre> compactOres() {
        return compactOres;
    }

    public static CompactOre getFor(ResourceLocation baseBlockLoc) {
        for(CompactOre ore : compactOres) {
            if(ore.getBaseBlockRegistryName().equals(baseBlockLoc)) {
                return ore;
            }
        }
        return null;
    }

    public static CompactOre getForResourceName(String resourceName) {
        for(CompactOre ore : compactOres) {
            if(ore.getSerializedName().equals(resourceName)) {
                return ore;
            }
        }
        return null;
    }
    
    public static InputStream getGeneratedResource(PackType type, ResourceLocation location) throws IOException {
        return resourcePack.getPack().getResource(type, location);
    }

    private static CreativeModeTab itemGroup = new CreativeModeTab(CompactOres.MODID) {
        @Override public ItemStack makeIcon() {
            return new ItemStack(compactOres.size() > 0 ? compactOres.get(0).getCompactOreBlockItem() : Items.STONE, 1);
        }
    };
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // prepare world gen features. registered later in #onBiomeLoading
        CompactOreWorldGen.init(compactOres);
    }
    
    public static CreativeModeTab getItemGroup() {
        return itemGroup;
    }

    public void startServer(final ServerAboutToStartEvent event) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        event.getServer().getPackRepository().addPackFinder(resourcePack);
        // when the server initially starts, it has already loaded its data.
        // the additional compact ores data is injected here.
        // if the server's resources are reloaded, the loot tables and tags are then re-obtained from the CompactOresResourcePack.
        /*LOGGER.info("Injecting post-first-load CompactOres server data into the Minecraft Server");
        LootTables ltm = event.getServer().getLootTables();
        Map<ResourceLocation, LootTable> tables =
                ltm.getIds().stream().collect(Collectors.toMap(Function.identity(), ltm::get));
        for(CompactOre ore : compactOres) {
            LootTable table = LootTable.lootTable().withPool(
                    LootPool.lootPool()
                            .setRolls(UniformGenerator.between(ore.getMinRolls(), ore.getMaxRolls()))
                            //.addEntry(TableLootEntry.builder(ore.getBaseBlock().getLootTable()))).build(); // I HAVE NO IDEA HOW TO GET THIS LINE WORKING RN
            tables.put(ore.getCompactOreBlock().getLootTable(), table);
        }
        ObfuscationReflectionHelper.setPrivateValue(LootTables.class, ltm, ImmutableMap.copyOf(tables), "field_186527_c");
        // tags
        // (accesstransformers are involved here)
        Tag<Block> oreBlockTag = findBaseTag(ForgeTagHandler.makeWrapperTag(ForgeRegistries.BLOCKS, new ResourceLocation("forge", "ores")));
        if(oreBlockTag != null) {
            Set<Block> oreBlockTagContents = new HashSet<>(oreBlockTag.contents);
            oreBlockTagContents.addAll(compactOres.stream().map(CompactOre::getCompactOreBlock).collect(Collectors.toSet()));
            oreBlockTag.contents = oreBlockTagContents;
            List<Block> oreBlockTagImmutableContents = new ArrayList<>(oreBlockTag.immutableContents);
            oreBlockTagImmutableContents.addAll(compactOres.stream().map(CompactOre::getCompactOreBlock).collect(Collectors.toList()));
            oreBlockTag.immutableContents = ImmutableList.copyOf(oreBlockTagImmutableContents);
        }
        Tag<Item> oreItemTag = findBaseTag(ForgeTagHandler.makeWrapperTag(ForgeRegistries.ITEMS, new ResourceLocation("forge", "ores")));
        if(oreItemTag != null) {
            Set<Item> oreItemTagContents = new HashSet<>(oreItemTag.contents);
            oreItemTagContents.addAll(compactOres.stream().map(CompactOre::getCompactOreBlockItem).collect(Collectors.toSet()));
            oreItemTag.contents = oreItemTagContents;
            List<Item> oreItemTagImmutableContents = new ArrayList<>(oreItemTag.immutableContents);
            oreItemTagImmutableContents.addAll(compactOres.stream().map(CompactOre::getCompactOreBlockItem).collect(Collectors.toList()));
            oreItemTag.immutableContents = ImmutableList.copyOf(oreItemTagImmutableContents);
        }*/
    }
    
    /*private <T> Tag<T> findBaseTag(ITag<T> tag) {
        try {
            @SuppressWarnings("unchecked")
            Class<ITag<?>> namedTagClass = (Class<ITag<?>>) Class.forName("net.minecraft.tags.TagRegistry$NamedTag");
            if (tag instanceof Tag) {
                return (Tag<T>) tag;
            } else if (namedTagClass.isAssignableFrom(tag.getClass())) {
                Field f = ObfuscationReflectionHelper.findField(namedTagClass, "field_232942_b_");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                ITag<T> baseTag = (ITag<T>)f.get(tag);
                return findBaseTag(baseTag);
            } else {
                LOGGER.error("Unexpected problem encountered when trying to inject compact ore tags - compact ores wil not be tagged");
                return null;
            }
        } catch(ClassNotFoundException | ObfuscationReflectionHelper.UnableToFindFieldException | IllegalAccessException | ClassCastException e) {
            LOGGER.error("Unexpected problem encountered when trying to inject compact ore tags - compact ores will not be tagged", e);
            return null;
        }
    }*/

    // global block break listener that fires multiple events for the base block when a compact ore is broken
    public void onBlockBroken(final BlockEvent.BreakEvent breakEvent) {
        if(!(breakEvent.getState().getBlock() instanceof CompactOreBlock)) return;
        CompactOre ore = ((CompactOreBlock)breakEvent.getState().getBlock()).getOre();
        int numEvents = ore.getMinRolls() + breakEvent.getWorld().getRandom().nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
        for(int i = 0; i < numEvents; i++) {
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                    (Level)breakEvent.getWorld(),
                    breakEvent.getPos(),
                    ore.getBaseBlock().defaultBlockState(),
                    breakEvent.getPlayer()));
        }
    }

    // The parameter type can not be Utils.ReturningScreen
    // because the JVM would then try to load that class,
    // so it would also have to load Screen, and that crashes on the dedicated server.
    // Supplier<Utils.ReturningScreen> or even Supplier<Supplier<Utils.ReturningScreen>> also don't seem to work.
    public static void setLoadFinishScreen(Object screen) {
        if(!(screen instanceof Utils.ReturningScreen)) return;
        loadFinishScreen = (Utils.ReturningScreen) screen;
        if(loadFinishTimer == null) {
            loadFinishTimer = new Timer();
            loadFinishTimer.scheduleAtFixedRate(new TimerTask() {
                @Override public void run() {
                    if(Minecraft.getInstance().screen instanceof TitleScreen) {
                        LOGGER.info("Main menu entered - switching to Compact Ores config notification screen");
                        loadFinishTimer.cancel();
                        Minecraft.getInstance().submitAsync(() -> {
                            Screen returnScreen = Minecraft.getInstance().screen;
                            loadFinishScreen.setReturnTarget(returnScreen);
                            Minecraft.getInstance().setScreen(loadFinishScreen);
                        });
                    }
                }
            }, 100, 100);
        }
    }

}
