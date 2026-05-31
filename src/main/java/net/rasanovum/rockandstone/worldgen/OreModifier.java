package net.rasanovum.rockandstone.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class OreModifier {
    public static void removeOverworldOres(String modId, String name, ResourceKey<PlacedFeature>... features) {
        BiomeModifications.create(new ResourceLocation(modId, name))
                .add(ModificationPhase.REMOVALS,
                        BiomeSelectors.foundInOverworld(),
                        context -> {
                            for (ResourceKey<PlacedFeature> feature : features) {
                                context.getGenerationSettings().removeFeature(
                                        GenerationStep.Decoration.UNDERGROUND_ORES,
                                        feature
                                );
                            }
                        }
                );
    }

    public static void addOverworldOre(ResourceKey<PlacedFeature> featureKey) {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                featureKey
        );
    }
}
