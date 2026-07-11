package net.rasanovum.rockandstone;

import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.rasanovum.rockandstone.util.AdvancementTrigger;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.util.OreScanner;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RockAndStone implements ModInitializer {
	public static final String MOD_ID = "rockandstone";
	public static AdvancementTrigger HOTSPOT_TRIGGER;
	public static PlacementModifierType<NoiseFilterPlacementModifier> NOISE_FILTER_TYPE;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, RockAndStoneConfig.class);
		boolean doDebugConfig = RockAndStoneConfig.doRockAndStoneDebug;
		HOTSPOT_TRIGGER = CriteriaTriggers.register(new AdvancementTrigger());
		// debug
		if (doDebugConfig) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				OreScanner.register(dispatcher);
			});
		}

		NOISE_FILTER_TYPE = Registry.register(
				BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
				new ResourceLocation(MOD_ID, "noise_filter"),
				() -> NoiseFilterPlacementModifier.CODEC
		);

		DynamicOreRequirements.registerDataPackListener();
		LOGGER.info("Datapack filtered-ore support is enabled; filters will be discovered when a world starts.");

		BiomeModifications.create(new ResourceLocation(MOD_ID, "dynamic_ore_removal"))
				.add(ModificationPhase.POST_PROCESSING,
						BiomeSelectors.foundInOverworld(),
						context -> {
							for (String customOrePath : DynamicOreRequirements.ACTIVE_FILTERS.keySet()) {
								ResourceLocation targetLocation = DynamicOreRequirements.targetFeatureId(customOrePath).orElseThrow();
								ResourceKey<PlacedFeature> targetKey = ResourceKey.create(Registries.PLACED_FEATURE, targetLocation);

								boolean removed = context.getGenerationSettings().removeFeature(
										GenerationStep.Decoration.UNDERGROUND_ORES,
										targetKey
								);
								if (removed) {
									LOGGER.debug("Removed {} for filtered replacement {}", targetLocation, customOrePath);
								}
							}
						}
				);

		BiomeModifications.create(new ResourceLocation(MOD_ID, "dynamic_ore_injector"))
				.add(ModificationPhase.ADDITIONS,
						BiomeSelectors.foundInOverworld(),
						context -> {
							for (String customOrePath : DynamicOreRequirements.ACTIVE_FILTERS.keySet()) {
								ResourceLocation customLocation = new ResourceLocation(MOD_ID, customOrePath);
								ResourceKey<PlacedFeature> customKey = ResourceKey.create(Registries.PLACED_FEATURE, customLocation);

								context.getGenerationSettings().addFeature(
										GenerationStep.Decoration.UNDERGROUND_ORES,
										customKey
								);
							}
						}
				);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				BlockPos pos = player.blockPosition();
				ServerLevel world = player.serverLevel();

				NoiseRouter noiseRouter = world.getChunkSource().randomState().router();
				DensityFunction.SinglePointContext contextPoint = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());

				double currentTemp = noiseRouter.temperature().compute(contextPoint);
				double currentHumidity = noiseRouter.vegetation().compute(contextPoint);
				double currentContinentalness = noiseRouter.continents().compute(contextPoint);
				double currentErosion = noiseRouter.erosion().compute(contextPoint);
				double weirdness = noiseRouter.ridges().compute(contextPoint);
				double currentPv = 1.0 - Math.abs(3.0 * Math.abs(weirdness) - 2.0);

				DynamicOreRequirements.ACTIVE_FILTERS.forEach((oreName, bounds) -> {
					boolean match = currentTemp >= bounds.minTemp() && currentTemp <= bounds.maxTemp() &&
							currentHumidity >= bounds.minHum() && currentHumidity <= bounds.maxHum() &&
							currentErosion >= bounds.minEro() && currentErosion <= bounds.maxEro() &&
							currentPv >= bounds.minRid() && currentPv <= bounds.maxRid() &&
							currentContinentalness >= bounds.minCon() && currentContinentalness <= bounds.maxCon();

					if (match && doDebugConfig) {
						System.out.println("DEBUG: Conditions met for " + oreName);
						HOTSPOT_TRIGGER.trigger(player, oreName);
					} else if (doDebugConfig) {
						System.out.printf("DEBUG: No conditions met! Temp: %.2f [%.2f/%.2f] | Ero: %.2f [%.2f/%.2f] | PV: %.2f [%.2f/%.2f]%n",
								currentTemp, bounds.minTemp(), bounds.maxTemp(),
								currentErosion, bounds.minEro(), bounds.maxEro(),
								currentPv, bounds.minRid(), bounds.maxRid()
						);
					} else if (match) {
						HOTSPOT_TRIGGER.trigger(player, oreName);
					}

				});
			}

		});
		LOGGER.info("We're rich!");
	}
}
