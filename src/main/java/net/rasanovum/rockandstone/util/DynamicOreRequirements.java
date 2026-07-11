package net.rasanovum.rockandstone.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class DynamicOreRequirements {
    public static final Map<String, NoiseBounds> ACTIVE_FILTERS = new HashMap<>();

    static {
        FabricLoader.getInstance().getModContainer("rockandstone").ifPresent(modContainer -> {
            // path to features
            String internalPath = "data/rockandstone/worldgen/placed_feature";

            modContainer.findPath(internalPath).ifPresent(directoryPath -> {
                try (Stream<Path> walk = Files.walk(directoryPath, 1)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".json"))
                            .forEach(filePath -> {
                                String fileName = filePath.getFileName().toString().replace(".json", "");

                                // empty placeholder
                                ACTIVE_FILTERS.put(fileName, new NoiseBounds(0, 0, 0, 0, 0, 0,0,0,0,0));
                            });
                } catch (IOException e) {
                    System.err.println("RockAndStone: Failed to crawl worldgen json files at: " + internalPath);
                    e.printStackTrace();
                }
            });
        });
    }

    public static void registerDataPackListener() {
    }


    public static Optional<ResourceLocation> targetFeatureId(String filteredFeaturePath) {
        if (!filteredFeaturePath.startsWith("filtered_")) {
            return Optional.empty();
        }

        String target = filteredFeaturePath.substring("filtered_".length());
        String namespace = "minecraft";
        String path = target;

        if (target.contains("__")) {
            String[] split = target.split("__", 2);
            namespace = split[0];
            path = split[1];
        }

        return Optional.ofNullable(ResourceLocation.tryBuild(namespace, path));
    }

    public static void loadActiveFilters(RegistryAccess registryAccess) {
        ACTIVE_FILTERS.clear();
        var registry = registryAccess.registryOrThrow(Registries.PLACED_FEATURE);

        registry.entrySet().forEach(entry -> {
            ResourceLocation id = entry.getKey().location();

            if (!id.getNamespace().equals(RockAndStone.MOD_ID)) {
                return;
            }

            Optional<ResourceLocation> targetId = targetFeatureId(id.getPath());
            if (targetId.isEmpty()) {
                return;
            }

            PlacedFeature feature = entry.getValue();
            for (PlacementModifier modifier : feature.placement()) {
                if (modifier instanceof NoiseFilterPlacementModifier filter) {
                    ACTIVE_FILTERS.put(id.getPath(), new NoiseBounds(
                            filter.getMinTemp(), filter.getMaxTemp(),
                            filter.getMinHumidity(), filter.getMaxHumidity(),
                            filter.getMinErosion(), filter.getMaxErosion(),
                            filter.getMinRidges(), filter.getMaxRidges(),
                            filter.getMinContinentalness(), filter.getMaxContinentalness()
                    ));

                    if (registry.containsKey(targetId.get())) {
                        RockAndStone.LOGGER.info("Discovered filtered feature {} -> {}", id, targetId.get());
                    } else {
                        RockAndStone.LOGGER.warn("Filtered feature {} targets {}, but that placed feature is not registered", id, targetId.get());
                    }
                    break;
                }
            }
        });

        RockAndStone.LOGGER.info("Loaded {} filtered ore features from active datapacks", ACTIVE_FILTERS.size());
    }

    public record NoiseBounds(double minTemp, double maxTemp, double minHum, double maxHum, double minEro, double maxEro, double minRid, double maxRid, double minCon, double maxCon) {}
}
