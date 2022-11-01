package doublenegation.mods.compactores;

import doublenegation.mods.compactores.config.ConfigLoader;
import doublenegation.mods.compactores.debug.CompactOresDebugging;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBroken);
        MinecraftForge.EVENT_BUS.addListener(this::tagsUpdated);

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

    private void tagsUpdated(final TagsUpdatedEvent event) {
        // copy over harvestability tags from base ore block to compact ore block
        Set<ResourceLocation> copiedTags =
                TierSortingRegistry.getSortedTiers().stream().map(Tier::getTag).filter(Objects::nonNull).map(TagKey::location).collect(Collectors.toSet());
        Registry<Block> blockRegistry = event.getTagManager().registryOrThrow(ForgeRegistries.BLOCKS.getRegistryKey());
        for(CompactOre ore : compactOres()) {
            Holder<Block> compactOreHolder = ForgeRegistries.BLOCKS.getResourceKey(ore.getCompactOreBlock()).flatMap(blockRegistry::getHolder).orElse(null);
            Holder<Block> baseOreHolder = ForgeRegistries.BLOCKS.getResourceKey(ore.getBaseBlock()).flatMap(blockRegistry::getHolder).orElse(null);
            if(baseOreHolder != null && (compactOreHolder instanceof Holder.Reference<Block> refHolder)) {
                refHolder.bindTags(Stream.concat(compactOreHolder.tags(),
                                baseOreHolder.tags().filter(tagKey -> copiedTags.contains(tagKey.location())))
                        .collect(Collectors.toSet()));
            }
        }
        Blocks.rebuildCache();
    }
    
    public static CreativeModeTab getItemGroup() {
        return itemGroup;
    }

    public static void registerServerPackFinder(PackRepository packRepository) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        packRepository.addPackFinder(resourcePack);
    }
    
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
