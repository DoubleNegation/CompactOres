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
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod("compactores")
public class CompactOres
{
    public static final Logger LOGGER = LogManager.getLogger();

    private static final Map<ResourceLocation, CompactOre> compactOres = new HashMap<>();
    private static CompactOresResourcePack resourcePack;

    public CompactOres() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Load the ores from the config
        for(CompactOre ore : CompactOresConfig.loadConfigs()) {
            LOGGER.info("Loaded compact ore " + ore.getRegistryName() + " from configuration!");
            compactOres.put(ore.getRegistryName(), ore);
        }

        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(this::getOreList);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackList().addPackFinder(resourcePack);
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

    private void setup(final FMLCommonSetupEvent event) {
        CompactOreWorldGen.init(compactOres);
    }

    private void setupClient(final FMLClientSetupEvent event) {
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

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            for(CompactOre ore : compactOres.values()) {
                ore.init1_block();
                blockRegistryEvent.getRegistry().register(ore.getBlock());
            }
        }
        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            for(CompactOre ore : compactOres.values()) {
                ore.init2_item();
                itemRegistryEvent.getRegistry().register(ore.getBlockItem());
            }
        }
        @SubscribeEvent
        public static void onDecoratorsRegistry(final RegistryEvent.Register<Placement<?>> decoratorRegistryEvent) {
            decoratorRegistryEvent.getRegistry().register(new CompactOreWorldGen.AllWithProbability(CompactOreWorldGen.ProbabilityConfig::deserialize)
                    .setRegistryName(new ResourceLocation("compactores", "all_with_probability")));
        }
    }
}
