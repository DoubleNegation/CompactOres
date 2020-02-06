package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

import java.util.HashMap;
import java.util.Map;

@Mod(CompactOres.MODID)
public class CompactOres
{
    public static final String MODID = "compactores";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS  = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Placement<?>> DECORATORS = new DeferredRegister<>(ForgeRegistries.DECORATORS, MODID);

    public static final RegistryObject<CompactOreWorldGen.AllWithProbability> ALL_WITH_PROBABILITY = DECORATORS.register(
            "all_with_probability", () -> new CompactOreWorldGen.AllWithProbability(CompactOreWorldGen.ProbabilityConfig::deserialize));

    private static final Map<ResourceLocation, CompactOre> compactOres = new HashMap<>();
    private static CompactOresResourcePack resourcePack;

    public CompactOres() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Load the ores from the config
        for(CompactOre ore : CompactOresConfig.loadConfigs()) {
            LOGGER.info("Loaded compact ore " + ore.getRegistryName() + " from configuration!");
            compactOres.put(ore.getRegistryName(), ore);
            BLOCKS.register(ore.getRegistryName().getPath(), ore::init1_block);
            ITEMS.register(ore.getRegistryName().getPath(), ore::init2_item);
        }

        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        DECORATORS.register(FMLJavaModLoadingContext.get().getModEventBus());

        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(this::getOreList);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackList().addPackFinder(resourcePack);
            CompactOreTexture.registerCacheInvalidator();
        });
    }

    private static ItemGroup itemGroup = new ItemGroup("compactores") {
        @Override public ItemStack createIcon() {
            return new ItemStack(compactOres.values().iterator().next().getBlock());
        }
    };

    private Map<ResourceLocation, CompactOre> getOreList() {
        return compactOres;
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        // This initialization needs to happen as late as possible to make sure that compact ores are generated
        // after all other ores
        CompactOreWorldGen.init(compactOres);
    }

    public static CompactOre getFor(ResourceLocation loc) {
        return compactOres.get(loc);
    }

    public static ItemGroup getItemGroup() {
        return itemGroup;
    }

    @SubscribeEvent
    public void startServer(final FMLServerAboutToStartEvent event) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        event.getServer().getResourcePacks().addPackFinder(resourcePack);
    }

    // global block break listener that fires multiple events for the base block when a compact ore is broken
    @SubscribeEvent
    public void onBlockBroken(final BlockEvent.BreakEvent breakEvent) {
        for(CompactOre ore : compactOres.values()) {
            if(ore.getBlock().equals(breakEvent.getState().getBlock())) {
                int numEvents = ore.getMinRolls() + breakEvent.getWorld().getRandom().nextInt(ore.getMaxRolls() - ore.getMinRolls() + 1);
                for(int i = 0; i < numEvents; i++) {
                    MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                            (World) breakEvent.getWorld(),
                            breakEvent.getPos(),
                            ore.getBaseBlock().getDefaultState(),
                            breakEvent.getPlayer()));
                }
                break;
            }
        }
    }
}
