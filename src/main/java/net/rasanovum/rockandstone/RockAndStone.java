package net.rasanovum.rockandstone;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RockAndStone implements ModInitializer {
	public static final String MOD_ID = "rockandstone";
	public static PlacementModifierType<NoiseFilterPlacementModifier> NOISE_FILTER_TYPE;

	public static final ResourceKey<PlacedFeature> FILTERED_IRON_UPPER = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_iron_upper"));
	public static final ResourceKey<PlacedFeature> FILTERED_IRON_MIDDLE = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_iron_middle"));
	public static final ResourceKey<PlacedFeature> FILTERED_IRON_SMALL = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_iron_small"));

	public static final ResourceKey<PlacedFeature> FILTERED_COAL_UPPER = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_coal_upper"));
	public static final ResourceKey<PlacedFeature> FILTERED_COAL_LOWER = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_coal_lower"));

	public static final ResourceKey<PlacedFeature> FILTERED_COPPER = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_copper"));
	public static final ResourceKey<PlacedFeature> FILTERED_COPPER_LARGE = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_copper_large"));

	public static final ResourceKey<PlacedFeature> FILTERED_GOLD = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_gold"));
	public static final ResourceKey<PlacedFeature> FILTERED_GOLD_LOWER = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_gold_lower"));

	public static final ResourceKey<PlacedFeature> FILTERED_EMERALD = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_emerald"));

	public static final ResourceKey<PlacedFeature> FILTERED_LAPIS = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_lapis"));
	public static final ResourceKey<PlacedFeature> FILTERED_LAPIS_BURIED = ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MOD_ID, "filtered_lapis_buried"));

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		// debug
		if (RockAndStoneConfig.doRockAndStoneDebug = true) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				OreScanner.register(dispatcher);
			});
		}


		NOISE_FILTER_TYPE = Registry.register(
				BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
				new ResourceLocation(MOD_ID, "noise_filter"),
				() -> NoiseFilterPlacementModifier.CODEC
		);

		// iron

		OreModifier.removeOverworldOres(MOD_ID,"iron_removal",
				OrePlacements.ORE_IRON_UPPER,
				OrePlacements.ORE_IRON_MIDDLE,
				OrePlacements.ORE_IRON_SMALL
		);
		OreModifier.addOverworldOre(FILTERED_IRON_SMALL);
		OreModifier.addOverworldOre(FILTERED_IRON_MIDDLE);
		OreModifier.addOverworldOre(FILTERED_IRON_UPPER);


		// coal

		OreModifier.removeOverworldOres(MOD_ID, "coal_removal",
				OrePlacements.ORE_COAL_UPPER,
				OrePlacements.ORE_COAL_LOWER
		);
		OreModifier.addOverworldOre(FILTERED_COAL_UPPER);
		OreModifier.addOverworldOre(FILTERED_COAL_LOWER);

		// copper
		OreModifier.removeOverworldOres(MOD_ID, "copper_removal",
				OrePlacements.ORE_COPPER,
				OrePlacements.ORE_COPPER_LARGE
		);
		OreModifier.addOverworldOre(FILTERED_COPPER);
		OreModifier.addOverworldOre(FILTERED_COPPER_LARGE);

		// gold
		OreModifier.removeOverworldOres(MOD_ID, "gold_removal",
				OrePlacements.ORE_GOLD,
				OrePlacements.ORE_GOLD_LOWER
		);
		OreModifier.addOverworldOre(FILTERED_GOLD);
		OreModifier.addOverworldOre(FILTERED_GOLD_LOWER);

		// emerald
		OreModifier.removeOverworldOres(MOD_ID, "emerald_removal",
				OrePlacements.ORE_EMERALD)
		;
		OreModifier.addOverworldOre(FILTERED_EMERALD);

		// lapis
		OreModifier.removeOverworldOres(MOD_ID, "lapis_removal",
				OrePlacements.ORE_LAPIS,
				OrePlacements.ORE_LAPIS_BURIED
		);
		OreModifier.addOverworldOre(FILTERED_LAPIS);
		OreModifier.addOverworldOre(FILTERED_LAPIS_BURIED);

		LOGGER.info("Hello Fabric world!");
	}
}