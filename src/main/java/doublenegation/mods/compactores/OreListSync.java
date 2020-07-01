package doublenegation.mods.compactores;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OreListSync {

    private static final String PROTOCOL_VERSION = "1";
    private static final String CHANNEL_NAME = "oresync";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CompactOres.MODID, CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        INSTANCE.registerMessage(0, OreListMessage.class, OreListMessage::encode, OreListMessage::decode, OreListSync::handle);
    }

    public static void sendListToClient(ServerPlayerEntity player, List<CompactOre> oreList) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new OreListMessage(oreList));
    }

    public static void handle(OreListMessage msg, Supplier<NetworkEvent.Context> ctx) {
        // this should only be executed on the client side, but just in case, use DistExecutor anyway
        // (clients can have bad intentions and we don't want to allow them to crash the server too easily)
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            ctx.get().enqueueWork(() -> {
                if(!msg.matchesLocalList()) {
                    Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("chat." + CompactOres.MODID + ".desync_warning"), null);
                }
            });
            ctx.get().setPacketHandled(true);
        });
    }

    public static class OreListMessage {
        private List<String> oreList;
        public OreListMessage(List<CompactOre> ores) {
            oreList = ores.stream().filter(CompactOre::isReal).map(ore -> CompactOreBlock.ORE_PROPERTY.getName(ore)).collect(Collectors.toList());
        }
        private OreListMessage(String oreList) {
            this.oreList = Arrays.asList(oreList.split(","));
        }
        public boolean matchesLocalList() {
            List<String> localOreList = new OreListMessage(CompactOres.compactOres()).oreList;
            return localOreList.equals(oreList);
        }
        public void encode(PacketBuffer buf) {
            buf.writeString(String.join(",", oreList));
        }
        public static OreListMessage decode(PacketBuffer buf) {
            return new OreListMessage(buf.readString());
        }
    }

}
