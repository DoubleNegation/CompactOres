package doublenegation.mods.compactores.mixin;

import doublenegation.mods.compactores.CompactOres;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(at = @At("HEAD"), method = "configurePackRepository(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/DataPackConfig;Z)Lnet/minecraft/world/level/DataPackConfig;")
    private static void configurePackRepository(PackRepository p_129820_, DataPackConfig p_129821_, boolean p_129822_, CallbackInfoReturnable<DataPackConfig> callback) {
        CompactOres.registerServerPackFinder(p_129820_);
    }

}
