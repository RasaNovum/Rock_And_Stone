package net.rasanovum.rockandstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.rasanovum.rockandstone.util.AdvancementTrigger;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;
import eu.midnightdust.lib.config.MidnightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RockAndStone {
    public static final String MOD_ID = "rockandstone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static AdvancementTrigger HOTSPOT_TRIGGER;
    public static PlacementModifierType<NoiseFilterPlacementModifier> NOISE_FILTER_TYPE;

    private RockAndStone() {
    }

    public static void initialize() {
        MidnightConfig.init(MOD_ID, RockAndStoneConfig.class);
        LOGGER.info("Ore replacement via datapack is {}.",
                RockAndStoneConfig.doOreReplacement ? "enabled" : "disabled");
        LOGGER.info("We're rich!");
    }

    public static void registerAdvancementTrigger() {
        HOTSPOT_TRIGGER = AdvancementTrigger.register();
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlockPos pos = player.blockPosition();
            ServerLevel world = player.serverLevel();

            NoiseRouter noiseRouter = world.getChunkSource().randomState().router();
            DensityFunction.SinglePointContext contextPoint =
                    new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());

            double currentTemp = noiseRouter.temperature().compute(contextPoint);
            double currentHumidity = noiseRouter.vegetation().compute(contextPoint);
            double currentContinentalness = noiseRouter.continents().compute(contextPoint);
            double currentErosion = noiseRouter.erosion().compute(contextPoint);
            double weirdness = noiseRouter.ridges().compute(contextPoint);
            double currentPv = 1.0 - Math.abs(3.0 * Math.abs(weirdness) - 2.0);

            DynamicOreRequirements.activeFilters().forEach((oreName, bounds) -> {
                boolean match = currentTemp >= bounds.minTemp() && currentTemp <= bounds.maxTemp() &&
                        currentHumidity >= bounds.minHum() && currentHumidity <= bounds.maxHum() &&
                        currentErosion >= bounds.minEro() && currentErosion <= bounds.maxEro() &&
                        currentPv >= bounds.minRid() && currentPv <= bounds.maxRid() &&
                        currentContinentalness >= bounds.minCon() && currentContinentalness <= bounds.maxCon();

                if (RockAndStoneConfig.doRockAndStoneDebug) {
                    if (match) {
                        LOGGER.info("Conditions met for {}", oreName);
                    } else {
                        LOGGER.info(
                                "No conditions met for {}. Temp: {} [{}/{}] | Ero: {} [{}/{}] | PV: {} [{}/{}]",
                                oreName,
                                currentTemp, bounds.minTemp(), bounds.maxTemp(),
                                currentErosion, bounds.minEro(), bounds.maxEro(),
                                currentPv, bounds.minRid(), bounds.maxRid()
                        );
                    }
                }

                if (match) {
                    HOTSPOT_TRIGGER.trigger(player, oreName);
                }
            });
        }
    }
}
