package net.rasanovum.rockandstone.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class OreScanner {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scanores")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerLevel world = source.getLevel();
                    BlockPos playerPos = BlockPos.containing(source.getPosition());

                    int radius = 16;
                    Map<Block, Integer> oreCounts = new HashMap<>();

                    source.sendSystemMessage(Component.translatable("commands.rockandstone.scan"));

                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight(); y++) {
                                BlockPos scanPos = playerPos.offset(x, y - playerPos.getY(), z);
                                BlockState state = world.getBlockState(scanPos);
                                Block block = state.getBlock();

                                if (isOre(block)) {
                                    oreCounts.put(block, oreCounts.getOrDefault(block, 0) + 1);
                                }
                            }
                        }
                    }

                    if (oreCounts.isEmpty()) {
                        source.sendSystemMessage(Component.translatable("commands.rockandstone.scan_no_results"));
                    } else {
                        oreCounts.forEach((block, count) -> {
                            source.sendSystemMessage(Component.literal(block.getName().getString() + ": " + count));
                        });
                    }

                    return 1;
                })
        );
    }

    private static boolean isOre(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return name.contains("_ore");
    }
}
