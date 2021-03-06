package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import doublenegation.mods.compactores.config.ConfigLoader;
import doublenegation.mods.compactores.debug.CompactOresDebugging;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableManager;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.TableLootEntry;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.tags.ITag;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mod(CompactOres.MODID)
public class CompactOres
{
    public static final String MODID = "compactores";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<Placement<?>> DECORATORS = DeferredRegister.create(ForgeRegistries.DECORATORS, MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);

    public static final RegistryObject<CompactOreWorldGen.AllWithProbability> ALL_WITH_PROBABILITY = DECORATORS.register(
            "all_with_probability", () -> new CompactOreWorldGen.AllWithProbability(CompactOreWorldGen.ProbabilityConfig.codec));

    public static final RegistryObject<CompactOreWorldGen.MultiReplaceBlockFeature> MULTI_REPLACE_BLOCK = FEATURES.register(
            "multi_replace_block", () -> new CompactOreWorldGen.MultiReplaceBlockFeature(CompactOreWorldGen.MultiReplaceBlockConfig.codec));

    private static List<CompactOre> compactOres;
    private static CompactOresResourcePack resourcePack;

    private static Utils.ReturningScreen loadFinishScreen;
    private static Timer loadFinishTimer;

    public CompactOres() {
        // Register all event listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::startServer);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBroken);
        // lowest priority means the features are registered and therefore generated after (most) others of the stage
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onBiomeLoading);

        // Load the config
        compactOres = ConfigLoader.loadOres();

        // Register the DeferredRegisters to the event bus to handle the registry events
        DECORATORS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FEATURES.register(FMLJavaModLoadingContext.get().getModEventBus());

        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(CompactOres::compactOres);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackList().addPackFinder(resourcePack);
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
            if(ore.getString().equals(resourceName)) {
                return ore;
            }
        }
        return null;
    }
    
    public static InputStream getGeneratedResource(ResourcePackType type, ResourceLocation location) throws IOException {
        return resourcePack.getPack().getResourceStream(type, location);
    }

    private static ItemGroup itemGroup = new ItemGroup(CompactOres.MODID) {
        @Override public ItemStack createIcon() {
            return new ItemStack(compactOres.size() > 0 ? compactOres.get(0).getCompactOreBlockItem() : Items.STONE, 1);
        }
    };
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // prepare world gen features. registered later in #onBiomeLoading
        CompactOreWorldGen.init(compactOres);
    }
    
    private void onBiomeLoading(final BiomeLoadingEvent event) {
        CompactOreWorldGen.register(event);
    }

    public static ItemGroup getItemGroup() {
        return itemGroup;
    }

    public void startServer(final FMLServerAboutToStartEvent event) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        event.getServer().getResourcePacks().addPackFinder(resourcePack);
        // when the server initially starts, it has already loaded its data.
        // the additional compact ores data is injected here.
        // if the server's resources are reloaded, the loot tables and tags are then re-obtained from the CompactOresResourcePack.
        LOGGER.info("Injecting post-first-load CompactOres server data into the Minecraft Server");
        LootTableManager ltm = event.getServer().getDataPackRegistries().getLootTableManager();
        Map<ResourceLocation, LootTable> tables = 
                ltm.getLootTableKeys().stream().collect(Collectors.toMap(Function.identity(), ltm::getLootTableFromLocation));
        for(CompactOre ore : compactOres) {
            LootTable table = LootTable.builder().addLootPool(
                    LootPool.builder()
                            .rolls(new RandomValueRange(ore.getMinRolls(), ore.getMaxRolls()))
                            .addEntry(TableLootEntry.builder(ore.getBaseBlock().getLootTable()))).build();
            tables.put(ore.getCompactOreBlock().getLootTable(), table);
        }
        ObfuscationReflectionHelper.setPrivateValue(LootTableManager.class, ltm, ImmutableMap.copyOf(tables), "field_186527_c");
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
        }
    }
    
    private <T> Tag<T> findBaseTag(ITag<T> tag) {
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
    }

    // global block break listener that fires multiple events for the base block when a compact ore is broken
    public void onBlockBroken(final BlockEvent.BreakEvent breakEvent) {
        if(!(breakEvent.getState().getBlock() instanceof CompactOreBlock)) return;
        CompactOre ore = ((CompactOreBlock)breakEvent.getState().getBlock()).getOre();
        int numEvents = ore.getMinRolls() + breakEvent.getWorld().getRandom().nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
        for(int i = 0; i < numEvents; i++) {
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                    (World)breakEvent.getWorld(),
                    breakEvent.getPos(),
                    ore.getBaseBlock().getDefaultState(),
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
                    if(Minecraft.getInstance().currentScreen instanceof MainMenuScreen) {
                        LOGGER.info("Main menu entered - switching to Compact Ores config notification screen");
                        loadFinishTimer.cancel();
                        Minecraft.getInstance().enqueue(() -> {
                            Screen returnScreen = Minecraft.getInstance().currentScreen;
                            loadFinishScreen.setReturnTarget(returnScreen);
                            Minecraft.getInstance().displayGuiScreen(loadFinishScreen);
                        });
                    }
                }
            }, 100, 100);
        }
    }

}
