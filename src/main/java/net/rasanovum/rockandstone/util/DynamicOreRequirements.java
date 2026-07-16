package net.rasanovum.rockandstone.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DynamicOreRequirements {
    private static final String PLACED_FEATURE_DIRECTORY = "worldgen/placed_feature";
    private static final ResourceLocation RELOAD_LISTENER_ID = new ResourceLocation(RockAndStone.MOD_ID, "dynamic_ore_requirements");
    private static volatile Map<String, NoiseBounds> activeFilters = Map.of();

    public static void registerDataPackListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return RELOAD_LISTENER_ID;
            }

            @Override
            public CompletableFuture<Void> reload(
                    PreparationBarrier preparationBarrier,
                    ResourceManager resourceManager,
                    ProfilerFiller preparationsProfiler,
                    ProfilerFiller reloadProfiler,
                    Executor backgroundExecutor,
                    Executor gameExecutor
            ) {
                return CompletableFuture
                        .supplyAsync(() -> readActiveFilters(resourceManager), backgroundExecutor)
                        .thenCompose(preparationBarrier::wait)
                        .thenAcceptAsync(DynamicOreRequirements::setActiveFilters, gameExecutor);
            }
        });
    }

    public static Map<String, NoiseBounds> activeFilters() {
        return activeFilters;
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

    private static Map<String, NoiseBounds> readActiveFilters(ResourceManager resourceManager) {
        Map<String, NoiseBounds> discoveredFilters = new LinkedHashMap<>();

        resourceManager.listResources(
                PLACED_FEATURE_DIRECTORY,
                resourceLocation -> resourceLocation.getNamespace().equals(RockAndStone.MOD_ID)
                        && resourceLocation.getPath().endsWith(".json")
        ).forEach((resourceLocation, resource) -> {
            String featurePath = featurePath(resourceLocation);
            if (featurePath == null || targetFeatureId(featurePath).isEmpty()) {
                return;
            }

            readNoiseBounds(resourceLocation, resource).ifPresent(bounds -> discoveredFilters.put(featurePath, bounds));
        });

        return Collections.unmodifiableMap(discoveredFilters);
    }

    private static String featurePath(ResourceLocation resourceLocation) {
        String prefix = PLACED_FEATURE_DIRECTORY + "/";
        String path = resourceLocation.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }

        String featurePath = path.substring(prefix.length(), path.length() - ".json".length());
        return featurePath.contains("/") ? null : featurePath;
    }

    private static Optional<NoiseBounds> readNoiseBounds(ResourceLocation resourceLocation, Resource resource) {
        try (var reader = resource.openAsReader()) {
            JsonElement placedFeature = JsonParser.parseReader(reader);
            if (!placedFeature.isJsonObject() || !placedFeature.getAsJsonObject().has("placement")) {
                return Optional.empty();
            }

            for (JsonElement placement : placedFeature.getAsJsonObject().getAsJsonArray("placement")) {
                if (!placement.isJsonObject()) {
                    continue;
                }

                JsonElement type = placement.getAsJsonObject().get("type");
                if (type == null || !type.isJsonPrimitive() || !"rockandstone:noise_filter".equals(type.getAsString())) {
                    continue;
                }

                return NoiseFilterPlacementModifier.CODEC
                        .parse(JsonOps.INSTANCE, placement)
                        .resultOrPartial(error -> RockAndStone.LOGGER.warn(
                                "Could not decode noise filter in {}: {}", resourceLocation, error
                        ))
                        .map(DynamicOreRequirements::toNoiseBounds);
            }
        } catch (IOException | RuntimeException exception) {
            RockAndStone.LOGGER.warn("Could not read placed feature {} while loading ore requirements", resourceLocation, exception);
        }

        return Optional.empty();
    }

    private static NoiseBounds toNoiseBounds(NoiseFilterPlacementModifier filter) {
        return new NoiseBounds(
                filter.getMinTemp(), filter.getMaxTemp(),
                filter.getMinHumidity(), filter.getMaxHumidity(),
                filter.getMinErosion(), filter.getMaxErosion(),
                filter.getMinRidges(), filter.getMaxRidges(),
                filter.getMinContinentalness(), filter.getMaxContinentalness()
        );
    }

    private static void setActiveFilters(Map<String, NoiseBounds> filters) {
        activeFilters = filters;
        for (Entry<String, NoiseBounds> filter : filters.entrySet()) {
            targetFeatureId(filter.getKey()).ifPresent(target -> RockAndStone.LOGGER.info(
                    "Discovered filtered feature {} -> {}", filter.getKey(), target
            ));
        }
        RockAndStone.LOGGER.info("Loaded {} filtered ore features from active datapacks", filters.size());
    }

    public record NoiseBounds(double minTemp, double maxTemp, double minHum, double maxHum, double minEro, double maxEro, double minRid, double maxRid, double minCon, double maxCon) {}
}
