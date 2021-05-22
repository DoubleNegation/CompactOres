package doublenegation.mods.compactores.debug;

import com.mojang.brigadier.context.CommandContext;
import doublenegation.mods.compactores.CompactOre;
import doublenegation.mods.compactores.CompactOres;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.stream.Collectors;

public class OreTester {

    public static int executeCommand(CommandContext<CommandSource> ctx) {
        if(!CompactOresDebugging.enabled()) {
            ctx.getSource().sendErrorMessage(new TranslationTextComponent("commands.compactores.debugging_disabled"));
            return 0;
        }
        BlockPos origin = new BlockPos(ctx.getSource().getPos());
        ServerWorld world = ctx.getSource().getWorld();
        List<String> modids = CompactOres.compactOres().stream().map(ore -> ore.getBaseBlockRegistryName().getNamespace()).distinct().sorted().collect(Collectors.toList());
        if(modids.contains("minecraft")) {
            // move minecraft to the start
            modids.remove("minecraft");
            modids.add(0, "minecraft");
        }
        int x = 0;
        for(String modid : modids) {
            base(world, origin.add(x, 0, 0));
            content(world, origin.add(x++, 0, 0), Blocks.BEDROCK.getDefaultState(), Blocks.BEDROCK.getDefaultState());
            for(CompactOre ore : CompactOres.compactOres().stream().filter(ore -> ore.getBaseBlockRegistryName().getNamespace().equals(modid)).sorted().collect(Collectors.toList())) {
                base(world, origin.add(x, 0, 0));
                content(world, origin.add(x++, 0, 0), ore.getCompactOreBlock().getDefaultState(), ore.getBaseBlock().getDefaultState());
            }
        }
        base(world, origin.add(x, 0, 0));
        content(world, origin.add(x, 0, 0), Blocks.BEDROCK.getDefaultState(), Blocks.BEDROCK.getDefaultState());
        ctx.getSource().sendFeedback(new TranslationTextComponent("commands.compactores.genoretester.success", CompactOres.compactOres().size()), true);
        return 0;
    }
    
    private static void base(ServerWorld world, BlockPos pos) {
        for(int i = 0; i <= 8; i++) {
            world.setBlockState(pos.add(0, 0, i), Blocks.BEDROCK.getDefaultState());
        }
        world.setBlockState(pos.add(0, 1, 0), Blocks.BEDROCK.getDefaultState());
        world.setBlockState(pos.add(0, 1, 1), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 1, 2), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 1, 8), Blocks.BEDROCK.getDefaultState());
        world.setBlockState(pos.add(0, 2, 0), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 2, 1), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 2, 2), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 2, 3), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(0, 2, 8), Blocks.BEDROCK.getDefaultState());
        for(int i = 3; i <= 4; i++) {
            for(int j = 0; j <= 7; j++) {
                world.setBlockState(pos.add(0, i, j), Blocks.AIR.getDefaultState());
            }
            world.setBlockState(pos.add(0, i, 8), Blocks.BEDROCK.getDefaultState());
        }
    }
    
    private static void content(ServerWorld world, BlockPos pos, BlockState compact, BlockState normal) {
        for(int i = 3; i <= 7; i++) {
            world.setBlockState(pos.add(0, 1, i), compact);
        }
        for(int i = 4; i <= 7; i++) {
            world.setBlockState(pos.add(0, 2, i), normal);
        }
    }
    
}
