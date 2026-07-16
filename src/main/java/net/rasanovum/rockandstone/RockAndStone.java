package net.rasanovum.rockandstone;

import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

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

import java.awt.*;

public class RockAndStone implements ModInitializer {
	public static final String MOD_ID = "rockandstone";
	public static AdvancementTrigger HOTSPOT_TRIGGER;
	public static PlacementModifierType<NoiseFilterPlacementModifier> NOISE_FILTER_TYPE;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, RockAndStoneConfig.class);
		boolean replaceOres = RockAndStoneConfig.doOreReplacement;
		boolean doDebug = RockAndStoneConfig.doRockAndStoneDebug;
		registerSilentAdvancementsPack();
		HOTSPOT_TRIGGER = CriteriaTriggers.register(new AdvancementTrigger());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
					OreScanner.register(dispatcher);
				});

		NOISE_FILTER_TYPE = Registry.register(
				BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
				new ResourceLocation(MOD_ID, "noise_filter"),
				() -> NoiseFilterPlacementModifier.CODEC
		);

		DynamicOreRequirements.registerDataPackListener();
		LOGGER.info("Ore replacement via datapack is {}.", replaceOres ? "enabled" : "disabled");

		BiomeModifications.create(new ResourceLocation(MOD_ID, "dynamic_ore_removal"))
				.add(ModificationPhase.POST_PROCESSING,
						BiomeSelectors.foundInOverworld(),
						context -> {
							if (!replaceOres) {
								return;
							}

							for (String customOrePath : DynamicOreRequirements.activeFilters().keySet()) {
								ResourceLocation targetLocation = DynamicOreRequirements.targetFeatureId(customOrePath).orElseThrow();
								ResourceKey<PlacedFeature> targetKey = ResourceKey.create(Registries.PLACED_FEATURE, targetLocation);

								boolean removed = context.getGenerationSettings().removeFeature(
										GenerationStep.Decoration.UNDERGROUND_ORES,
										targetKey
								);
								if (removed) {
									LOGGER.debug("Removed {} and replaced with {}", targetLocation, customOrePath);
								}
							}
						}
				);

		BiomeModifications.create(new ResourceLocation(MOD_ID, "dynamic_ore_injector"))
				.add(ModificationPhase.ADDITIONS,
						BiomeSelectors.foundInOverworld(),
						context -> {
							if (!replaceOres) {
								return;
							}

							for (String customOrePath : DynamicOreRequirements.activeFilters().keySet()) {
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

				DynamicOreRequirements.activeFilters().forEach((oreName, bounds) -> {
					boolean match = currentTemp >= bounds.minTemp() && currentTemp <= bounds.maxTemp() &&
							currentHumidity >= bounds.minHum() && currentHumidity <= bounds.maxHum() &&
							currentErosion >= bounds.minEro() && currentErosion <= bounds.maxEro() &&
							currentPv >= bounds.minRid() && currentPv <= bounds.maxRid() &&
							currentContinentalness >= bounds.minCon() && currentContinentalness <= bounds.maxCon();

					if (doDebug) {
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

		});
		LOGGER.info("We're rich!");
	}

	private static void registerSilentAdvancementsPack() {
		if (!RockAndStoneConfig.doSilentAdvancements) {
			return;
		}

		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresentOrElse(modContainer -> {
			boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
					new ResourceLocation(MOD_ID, "silent_advancements"),
					modContainer,
					ResourcePackActivationType.ALWAYS_ENABLED
			);
			if (registered) {
				LOGGER.info("Silent advancements are enabled; registering the built-in silent advancements datapack.");
			} else {
				LOGGER.error("Silent advancements are enabled, but the built-in datapack could not be registered.");
			}
		}, () -> LOGGER.error("Silent advancements are enabled, but the {} mod container could not be found.", MOD_ID));
	}
}
