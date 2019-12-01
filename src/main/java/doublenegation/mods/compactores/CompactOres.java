package doublenegation.mods.compactores;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
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

        // Initialize the compact ore types here for now
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_coal_ore"),
                new CompactOre(Blocks.COAL_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/coal_ore"),
                    new ResourceLocation("minecraft", "textures/block/coal_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_default.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_iron_ore"),
                new CompactOre(Blocks.IRON_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/iron_ore"),
                        new ResourceLocation("minecraft", "textures/block/iron_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_default.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_gold_ore"),
                new CompactOre(Blocks.GOLD_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/gold_ore"),
                        new ResourceLocation("minecraft", "textures/block/gold_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_default.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_redstone_ore"),
                new CompactOre(Blocks.REDSTONE_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/redstone_ore"),
                        new ResourceLocation("minecraft", "textures/block/redstone_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_default.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_lapis_ore"),
                new CompactOre(Blocks.LAPIS_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/lapis_ore"),
                        new ResourceLocation("minecraft", "textures/block/lapis_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_lapis.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_diamond_ore"),
                new CompactOre(Blocks.DIAMOND_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/diamond_ore"),
                        new ResourceLocation("minecraft", "textures/block/diamond_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_default.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_emerald_ore"),
                new CompactOre(Blocks.EMERALD_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/emerald_ore"),
                        new ResourceLocation("minecraft", "textures/block/emerald_ore.png"), new ResourceLocation("compactores", "textures/ore_underlay_minecraft_emerald.png")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_nether_quartz_ore"),
                new CompactOre(Blocks.NETHER_QUARTZ_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/nether_quartz_ore"),
                        new ResourceLocation("minecraft", "textures/block/nether_quartz_ore.png"), new ResourceLocation("minecraft", "textures/block/netherrack.png")));
        // create the resource pack finder as early as possible, and register it to the client immediately
        // the resource pack will be created only when the game attempts to load it for the first time
        // this makes sure that it will exist by the time the resources are loaded for the first time on the client
        resourcePack = new CompactOresResourcePack(this::getOreList);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("Attaching CompactOre resources to the Minecraft client");
            Minecraft.getInstance().getResourcePackList().addPackFinder(resourcePack);
        });
    }

    private Map<ResourceLocation, CompactOre> getOreList() {
        return compactOres;
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void setupClient(final FMLClientSetupEvent event) {
    }

    public static CompactOre getFor(ResourceLocation loc) {
        return compactOres.get(loc);
    }

    @SubscribeEvent
    public void startServer(final FMLServerAboutToStartEvent event) {
        LOGGER.info("Attaching CompactOre resources to the Minecraft server");
        event.getServer().getResourcePacks().addPackFinder(resourcePack);
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            //blockRegistryEvent.getRegistry().register(new CompactOreBlock(new ResourceLocation("minecraft", "coal_ore")).setRegistryName("dense_minecraft_coal_ore"));
            LOGGER.debug("BLOCK REGISTRY EVENT!!!!");
            for(CompactOre ore : compactOres.values()) {
                ore.init1_block();
                blockRegistryEvent.getRegistry().register(ore.getBlock());
            }
        }
        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            //itemRegistryEvent.getRegistry().register(new BlockItem(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("compactores", "dense_minecraft_coal_ore")), new Item.Properties()).setRegistryName("dense_minecraft_coal_ore"));
            for(CompactOre ore : compactOres.values()) {
                ore.init2_item();
                itemRegistryEvent.getRegistry().register(ore.getBlockItem());
            }
        }
    }
}
