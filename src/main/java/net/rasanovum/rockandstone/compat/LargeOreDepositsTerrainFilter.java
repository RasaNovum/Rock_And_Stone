package net.rasanovum.rockandstone.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LargeOreDepositsTerrainFilter {
    private static final String DEPOSIT_CLASS = "com.endertech.minecraft.mods.adlods.deposit.Deposit";
    private static final String TARGET_RESULT_CLASS = "com.endertech.minecraft.mods.adlods.target.TargetGenResult";
    private static final AtomicBoolean REFLECTION_WARNING_LOGGED = new AtomicBoolean();

    private LargeOreDepositsTerrainFilter() {
    }

    public static Optional<Object> rejectedResult(Object target, WorldGenLevel level, BlockPos position) {
        if (!RockAndStoneConfig.doLargeOreDepositsTerrainFiltering || !isDeposit(target)) {
            return Optional.empty();
        }
        if (!(level.getChunkSource() instanceof ServerChunkCache chunkCache)) {
            return Optional.empty();
        }

        Set<String> depositOres = depositOreNames(target);
        if (depositOres.isEmpty()) {
            return Optional.empty();
        }

        NoiseRouter noiseRouter = chunkCache.randomState().router();
        Map<String, DynamicOreRequirements.NoiseBounds> filters = DynamicOreRequirements.activeFilters();
        for (String depositOre : depositOres) {
            boolean foundFilter = false;
            boolean accepted = false;

            for (Map.Entry<String, DynamicOreRequirements.NoiseBounds> entry : filters.entrySet()) {
                Optional<ResourceLocation> targetFeature = DynamicOreRequirements.targetFeatureId(entry.getKey());
                if (targetFeature.isEmpty() || !depositOre.equals(normalizeOreId(targetFeature.get()))) {
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
                    accepted = true;
                    break;
                }
            }

            if (foundFilter && !accepted) {
                return emptyResult(target, position);
            }
        }

        return Optional.empty();
    }

    private static boolean isDeposit(Object target) {
        Class<?> type = target.getClass();
        while (type != null) {
            if (DEPOSIT_CLASS.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Set<String> depositOreNames(Object deposit) {
        Set<String> oreNames = new HashSet<>();
        try {
            Method getPlacements = deposit.getClass().getMethod("getPlacements");
            Object placements = getPlacements.invoke(deposit);
            Method getOreBlocks = placements.getClass().getMethod("getOreBlocks");
            Object oreBlocks = getOreBlocks.invoke(placements);
            if (!(oreBlocks instanceof Iterable<?> blocks)) {
                return oreNames;
            }

            for (Object value : blocks) {
                if (value instanceof Block block) {
                    oreNames.add(normalizeOreId(BuiltInRegistries.BLOCK.getKey(block)));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logReflectionWarning(exception);
        }
        return oreNames;
    }

    private static Optional<Object> emptyResult(Object target, BlockPos position) {
        try {
            String targetType = target.getClass().getSimpleName();
            String targetName = (String) target.getClass().getMethod("getName").invoke(target);
            Class<?> resultClass = Class.forName(TARGET_RESULT_CLASS, false, target.getClass().getClassLoader());
            Method none = resultClass.getMethod("none", String.class, String.class, BlockPos.class);
            Object result = none.invoke(null, targetType, targetName, position);
            if (RockAndStoneConfig.doRockAndStoneDebug) {
                RockAndStone.LOGGER.debug("Rejected Large Ore Deposits target {} at {}", targetName, position);
            }
            return Optional.of(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logReflectionWarning(exception);
            return Optional.empty();
        }
    }

    private static String normalizeOreId(ResourceLocation id) {
        return id.getNamespace() + ":" + normalizeOreName(id.getPath());
    }

    private static String normalizeOreName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("ore_")) {
            normalized = normalized.substring("ore_".length());
        }
        if (normalized.startsWith("deepslate_")) {
            normalized = normalized.substring("deepslate_".length());
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

    private static void logReflectionWarning(Throwable exception) {
        if (REFLECTION_WARNING_LOGGED.compareAndSet(false, true)) {
            RockAndStone.LOGGER.warn(
                    "Could not inspect Large Ore Deposits internals; terrain filtering will fail open",
                    exception
            );
        }
    }
}
