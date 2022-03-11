package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.stream.Collectors;

public class OreTester {

    public static int executeCommand(CommandContext<CommandSourceStack> ctx) {
        if(!CompactOresDebugging.enabled()) {
            ctx.getSource().sendFailure(new TranslatableComponent("commands.compactores.debugging_disabled"));
            return 0;
        }
        BlockPos origin = new BlockPos(ctx.getSource().getPosition());
        ServerLevel world = ctx.getSource().getLevel();
        List<String> modids = CompactOres.compactOres().stream().map(ore -> ore.getBaseBlockRegistryName().getNamespace()).distinct().sorted().collect(Collectors.toList());
        if(modids.contains("minecraft")) {
            // move minecraft to the start
            modids.remove("minecraft");
            modids.add(0, "minecraft");
        }
        int x = 0;
        for(String modid : modids) {
            base(world, origin.offset(x, 0, 0));
            content(world, origin.offset(x++, 0, 0), Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState());
            for(CompactOre ore : CompactOres.compactOres().stream().filter(ore -> ore.getBaseBlockRegistryName().getNamespace().equals(modid)).sorted().collect(Collectors.toList())) {
                base(world, origin.offset(x, 0, 0));
                content(world, origin.offset(x++, 0, 0), ore.getCompactOreBlock().defaultBlockState(), ore.getBaseBlock().defaultBlockState());
            }
        }
        base(world, origin.offset(x, 0, 0));
        content(world, origin.offset(x, 0, 0), Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState());
        ctx.getSource().sendSuccess(new TranslatableComponent("commands.compactores.genoretester.success", CompactOres.compactOres().size()), true);
        return 0;
    }
    
    private static void base(ServerLevel world, BlockPos pos) {
        for(int i = 0; i <= 8; i++) {
            world.setBlock(pos.offset(0, 0, i), Blocks.BEDROCK.defaultBlockState(), 0);
        }
        world.setBlock(pos.offset(0, 1, 0), Blocks.BEDROCK.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 1, 1), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 1, 2), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 1, 8), Blocks.BEDROCK.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 2, 0), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 2, 1), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 2, 2), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 2, 3), Blocks.AIR.defaultBlockState(), 0);
        world.setBlock(pos.offset(0, 2, 8), Blocks.BEDROCK.defaultBlockState(), 0);
        for(int i = 3; i <= 4; i++) {
            for(int j = 0; j <= 7; j++) {
                world.setBlock(pos.offset(0, i, j), Blocks.AIR.defaultBlockState(), 0);
            }
            world.setBlock(pos.offset(0, i, 8), Blocks.BEDROCK.defaultBlockState(), 0);
        }
    }
    
    private static void content(ServerLevel world, BlockPos pos, BlockState compact, BlockState normal) {
        for(int i = 3; i <= 7; i++) {
            world.setBlock(pos.offset(0, 1, i), compact, 0);
        }
        for(int i = 4; i <= 7; i++) {
            world.setBlock(pos.offset(0, 2, i), normal, 0);
        }
    }
    
}
