package doublenegation.mods.compactores;

import doublenegation.mods.compactores.config.ConfigLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Mod(CompactOres.MODID)
public class CompactOres
{
    public static final String MODID = "compactores";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS  = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, MODID);
    private static final DeferredRegister<Placement<?>> DECORATORS = new DeferredRegister<>(ForgeRegistries.DECORATORS, MODID);

    public static final RegistryObject<CompactOreBlock> COMPACT_ORE = BLOCKS.register("compact_ore", CompactOreBlock::new);

    public static final RegistryObject<CompactOreBlockItem> COMPACT_ORE_ITEM = ITEMS.register(
            "compact_ore", () -> new CompactOreBlockItem(COMPACT_ORE.get()));

    public static final RegistryObject<TileEntityType<CompactOreTileEntity>> COMPACT_ORE_TE = TILE_ENTITIES.register(
            "compact_ore", () -> TileEntityType.Builder.create(CompactOreTileEntity::new).build(null));

    public static final RegistryObject<CompactOreWorldGen.AllWithProbability> ALL_WITH_PROBABILITY = DECORATORS.register(
            "all_with_probability", () -> new CompactOreWorldGen.AllWithProbability(CompactOreWorldGen.ProbabilityConfig::deserialize));

    private static List<CompactOre> compactOres;
    private static CompactOresResourcePack resourcePack;

    private static Utils.ReturningScreen loadFinishScreen;
    private static Timer loadFinishTimer;

    public CompactOres() {
        // Register all event listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModConfigLoading);
        MinecraftForge.EVENT_BUS.addListener(this::startServer);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBroken);

        // Load the config
        compactOres = ConfigLoader.loadOres();

        // Prepare the "missing" ore
        compactOres.add(0, new CompactOre());

        // Register the DeferredRegisters to the event bus to handle the registry events
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILE_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        DECORATORS.register(FMLJavaModLoadingContext.get().getModEventBus());

        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(CompactOres::compactOres);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackList().addPackFinder(resourcePack);
            CompactOreTexture.registerCacheInvalidator();
        });
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
            if(ore.getName().equals(resourceName)) {
                return ore;
            }
        }
        return null;
    }

    private static ItemGroup itemGroup = new ItemGroup(CompactOres.MODID) {
        @Override public ItemStack createIcon() {
            return COMPACT_ORE_ITEM.get().getStackOfOre(compactOres.get(1), 1);
        }
    };

    private void loadComplete(final FMLLoadCompleteEvent event) {
        // This initialization needs to happen as late as possible to make sure that compact ores are generated
        // after all other ores
        CompactOreWorldGen.init(compactOres);
    }

    public static ItemGroup getItemGroup() {
        return itemGroup;
    }

    public void startServer(final FMLServerAboutToStartEvent event) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        event.getServer().getResourcePacks().addPackFinder(resourcePack);
    }

    // global block break listener that fires multiple events for the base block when a compact ore is broken
    public void onBlockBroken(final BlockEvent.BreakEvent breakEvent) {
        BlockState state = breakEvent.getState();
        if(!state.getBlock().equals(COMPACT_ORE.get())) return;
        CompactOre ore = state.get(CompactOreBlock.ORE_PROPERTY);
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
