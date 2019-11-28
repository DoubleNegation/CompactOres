package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Mod("compactores")
public class CompactOres
{
    public static final Logger LOGGER = LogManager.getLogger();

    private static final Map<ResourceLocation, CompactOre> compactOres = new HashMap<>();

    public CompactOres() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Initialize the compact ore types here for now
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_coal_ore"),
                new CompactOre(Blocks.COAL_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/coal_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_iron_ore"),
                new CompactOre(Blocks.IRON_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/iron_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_gold_ore"),
                new CompactOre(Blocks.GOLD_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/gold_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_redstone_ore"),
                new CompactOre(Blocks.REDSTONE_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/redstone_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_lapis_ore"),
                new CompactOre(Blocks.LAPIS_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/lapis_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_diamond_ore"),
                new CompactOre(Blocks.DIAMOND_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/diamond_ore")));
        compactOres.put(new ResourceLocation("compactores", "dense_minecraft_emerald_ore"),
                new CompactOre(Blocks.EMERALD_ORE, 3, 5, new ResourceLocation("minecraft", "blocks/emerald_ore")));
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
    }

    public static CompactOre getFor(ResourceLocation loc) {
        return compactOres.get(loc);
    }

    @SubscribeEvent
    public void startServer(final FMLServerAboutToStartEvent event) {
        ImmutableMap.Builder<ResourceLocation, LootTable> mapBuilder = ImmutableMap.builder();
        for(CompactOre ore : compactOres.values()) {
            mapBuilder.put(ore.getLootTableLocation(), ore.getLootTable());
        }
        MinecraftServer server = event.getServer();
        Field f = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_200256_aj");
        try {
            f.set(server, new InjectingLootTableManager((LootTableManager) f.get(server), mapBuilder.build()));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void loadComplete(final FMLLoadCompleteEvent event) {
        LOGGER.debug("LOAD COMPLETE!  " + event);
        for(CompactOre ore : compactOres.values()) {
            ore.init3_final();
        }
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
