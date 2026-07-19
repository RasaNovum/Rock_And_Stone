package net.rasanovum.rockandstone.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
//? if <1.21 {
/*import net.minecraft.world.level.chunk.ChunkStatus;
*///?} else {
import net.minecraft.world.level.chunk.status.ChunkStatus;
//?}
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.rasanovum.rockandstone.RockAndStoneConfig;

import java.util.HashMap;
import java.util.Map;

public class OreScanner {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("scanOresCurrentChunk")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerLevel world = source.getLevel();
                    BlockPos playerPos = BlockPos.containing(source.getPosition());

                    NoiseRouter noiseRouter = world.getChunkSource().randomState().router();
                    DensityFunction.SinglePointContext contextPoint =
                            new DensityFunction.SinglePointContext(playerPos.getX(), playerPos.getY(), playerPos.getZ());

                    if (RockAndStoneConfig.doRockAndStoneDebug) {
                        double currentTemp = noiseRouter.temperature().compute(contextPoint);
                        double currentHumidity = noiseRouter.vegetation().compute(contextPoint);
                        double currentContinentalness = noiseRouter.continents().compute(contextPoint);
                        double currentErosion = noiseRouter.erosion().compute(contextPoint);
                        double weirdness = noiseRouter.ridges().compute(contextPoint);
                        double currentPv = 1.0 - Math.abs(3.0 * Math.abs(weirdness) - 2.0);

                        source.sendSystemMessage(Component.literal("§6--- NOISE ROUTER VALUES ---"));
                        source.sendSystemMessage(Component.literal(String.format(
                                "§aCoords: §f[%d, %d, %d] §7| §aTemp: §f%.3f §7| §aHumid: §f%.3f",
                                playerPos.getX(), playerPos.getY(), playerPos.getZ(), currentTemp, currentHumidity
                        )));
                        source.sendSystemMessage(Component.literal(String.format(
                                "§aErosion: §f%.3f §7| §aContinentalness: §f%.3f §7| §aPV: §f%.3f",
                                currentErosion, currentContinentalness, currentPv
                        )));
                        source.sendSystemMessage(Component.literal("§6---------------------------------------"));
                    }

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

                    printResults(source, oreCounts);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("scanOresChunkRadius")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("radiusInChunks", IntegerArgumentType.integer(1, 32))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerLevel world = source.getLevel();
                            int chunkRadius = IntegerArgumentType.getInteger(context, "radiusInChunks");

                            BlockPos centerPos = BlockPos.containing(source.getPosition());
                            ChunkPos centerChunk = new ChunkPos(centerPos);

                            Map<Block, Integer> oreCounts = new HashMap<>();
                            Component radiusMsg = Component.translatable("commands.rockandstone.radius_scan_message");
                            source.sendSystemMessage(Component.literal(radiusMsg.getString() + chunkRadius));

                            for (int xOffset = -chunkRadius; xOffset <= chunkRadius; xOffset++) {
                                for (int zOffset = -chunkRadius; zOffset <= chunkRadius; zOffset++) {
                                    int targetChunkX = centerChunk.x + xOffset;
                                    int targetChunkZ = centerChunk.z + zOffset;

                                    ChunkAccess chunk = world.getChunkSource().getChunk(targetChunkX, targetChunkZ, ChunkStatus.FULL, false);
                                    if (chunk == null) continue;

                                    for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight(); y++) {
                                        for (int x = 0; x < 16; x++) {
                                            for (int z = 0; z < 16; z++) {
                                                BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
                                                Block block = state.getBlock();

                                                if (isOre(block)) {
                                                    oreCounts.put(block, oreCounts.getOrDefault(block, 0) + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            printResults(source, oreCounts);
                            return 1;
                        })
                )
        );
    }

    private static boolean isOre(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return name.contains("_ore");
    }

    private static void printResults(CommandSourceStack source, Map<Block, Integer> oreCounts) {
        if (oreCounts.isEmpty()) {
            source.sendSystemMessage(Component.translatable("commands.rockandstone.scan_no_results"));
        } else {
            source.sendSystemMessage(Component.translatable("commands.rockandstone.scan_results"));
            oreCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> {
                        source.sendSystemMessage(Component.literal("§e" + entry.getKey().getName().getString() + ": §f" + entry.getValue()));
                    });
        }
    }
}
