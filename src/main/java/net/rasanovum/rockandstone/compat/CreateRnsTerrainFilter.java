package net.rasanovum.rockandstone.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CreateRnsTerrainFilter {
    private static final String CREATE_RNS_NAMESPACE = "create_rns";

    private CreateRnsTerrainFilter() {
    }

    public static boolean accepts(Structure structure, GenerationContext context, net.minecraft.core.BlockPos position) {
        if (!RockAndStoneConfig.doCreateRnsTerrainFiltering) {
            return true;
        }

        Optional<ResourceLocation> structureId = context.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getResourceKey(structure)
                .map(key -> key.location());
        if (structureId.isEmpty()) {
            return true;
        }

        Optional<String> resource = resourceName(structureId.get());
        if (resource.isEmpty()) {
            return true;
        }

        Map<String, DynamicOreRequirements.NoiseBounds> filters = DynamicOreRequirements.activeFilters();
        NoiseRouter noiseRouter = context.randomState().router();
        boolean foundFilter = false;
        for (Map.Entry<String, DynamicOreRequirements.NoiseBounds> entry : filters.entrySet()) {
            Optional<ResourceLocation> target = DynamicOreRequirements.targetFeatureId(entry.getKey());
            if (target.isEmpty() || !resource.get().equals(normalizeOreName(target.get().getPath()))) {
                continue;
            }

            foundFilter = true;
            DynamicOreRequirements.NoiseBounds bounds = entry.getValue();
            if (NoiseFilterPlacementModifier.matches(
                    noiseRouter,
                    position,
                    bounds.minTemp(), bounds.maxTemp(),
                    bounds.minHum(), bounds.maxHum(),
                    bounds.minEro(), bounds.maxEro(),
                    bounds.minCon(), bounds.maxCon(),
                    bounds.minRid(), bounds.maxRid()
            )) {
                return true;
            }
        }

        return !foundFilter;
    }

    private static Optional<String> resourceName(ResourceLocation structureId) {
        if (!CREATE_RNS_NAMESPACE.equals(structureId.getNamespace())) {
            return Optional.empty();
        }

        String path = structureId.getPath();
        if (!path.startsWith("deposit_")) {
            return Optional.empty();
        }

        path = path.substring("deposit_".length());
        if (path.startsWith("overworld_")) {
            path = path.substring("overworld_".length());
        } else if (path.startsWith("nether_")) {
            path = path.substring("nether_".length());
        }

        return path.isEmpty() ? Optional.empty() : Optional.of(normalizeOreName(path));
    }

    private static String normalizeOreName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("ore_")) {
            normalized = normalized.substring("ore_".length());
        }
        if (normalized.endsWith("_ore")) {
            normalized = normalized.substring(0, normalized.length() - "_ore".length());
        }
        for (String suffix : new String[]{"_small", "_large", "_buried", "_upper", "_lower", "_middle"}) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }
        return normalized;
    }
}
