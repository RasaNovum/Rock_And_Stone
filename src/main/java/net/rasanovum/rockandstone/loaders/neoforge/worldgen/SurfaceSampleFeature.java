package net.rasanovum.rockandstone.loaders.neoforge.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.util.OreNameNormalizer;
import net.rasanovum.rockandstone.util.VersionUtils;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SurfaceSampleFeature extends Feature<NoneFeatureConfiguration> {
    private static final String SURFACE_SAMPLES_NAMESPACE = "surfacesamples";
    private static final ResourceLocation SURFACE_SAMPLES_MARKER =
            VersionUtils.fromNamespaceAndPath(SURFACE_SAMPLES_NAMESPACE, "coal_ore_sample");

    public SurfaceSampleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        double frequency = Math.max(0.0, Math.min(1.0, RockAndStoneConfig.surfaceSampleFrequency));
        if (!RockAndStoneConfig.doOreReplacement || frequency <= 0.0) {
            return false;
        }
        if (BuiltInRegistries.BLOCK.getOptional(SURFACE_SAMPLES_MARKER).isEmpty()) {
            return false;
        }

        WorldGenLevel level = context.level();
        if (!(level.getChunkSource() instanceof ServerChunkCache chunkCache)) {
            return false;
        }

        BlockPos position = context.origin();
        NoiseRouter noiseRouter = chunkCache.randomState().router();
        Set<Block> matchingSamples = new LinkedHashSet<>();

        DynamicOreRequirements.activeFilters().forEach((filterName, bounds) -> {
            Optional<ResourceLocation> target = DynamicOreRequirements.targetFeatureId(filterName);
            if (target.isEmpty()) {
                return;
            }

            String oreName = OreNameNormalizer.normalizePath(target.get().getPath());
            ResourceLocation sampleId = VersionUtils.fromNamespaceAndPath(
                    SURFACE_SAMPLES_NAMESPACE,
                    samplePath(oreName)
            );
            Optional<Block> sample = BuiltInRegistries.BLOCK.getOptional(sampleId);
            if (sample.isEmpty()) {
                return;
            }

            if (NoiseFilterPlacementModifier.matches(
                    noiseRouter,
                    position,
                    bounds.minTemp(), bounds.maxTemp(),
                    bounds.minHum(), bounds.maxHum(),
                    bounds.minEro(), bounds.maxEro(),
                    bounds.minCon(), bounds.maxCon(),
                    bounds.minRid(), bounds.maxRid()
            )) {
                matchingSamples.add(sample.get());
            }
        });

        if (matchingSamples.isEmpty()) {
            return false;
        }

        RandomSource random = context.random();
        if (frequency < 1.0 && random.nextDouble() >= frequency) {
            return false;
        }

        List<Block> samples = new ArrayList<>(matchingSamples);
        Block sample = samples.get(random.nextInt(samples.size()));
        BlockState existingState = level.getBlockState(position);
        boolean waterlogged = existingState.getFluidState().is(FluidTags.WATER);
        if (!existingState.canBeReplaced() && !waterlogged) {
            return false;
        }

        BlockState sampleState = withStringProperty(sample.defaultBlockState(), "flavour", "unknown");
        if (sampleState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            sampleState = sampleState.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        }
        if (!sampleState.canSurvive(level, position)) {
            return false;
        }

        boolean placed = level.setBlock(position, sampleState, Block.UPDATE_CLIENTS);
        if (placed && RockAndStoneConfig.doRockAndStoneDebug) {
            RockAndStone.LOGGER.debug(
                    "Placed Surface Sample {} at {}",
                    BuiltInRegistries.BLOCK.getKey(sample),
                    position
            );
        }
        return placed;
    }

    private static String samplePath(String oreName) {
        return switch (oreName) {
            case "ancient_debris", "andesite" -> oreName + "_sample";
            default -> oreName + "_ore_sample";
        };
    }

    private static BlockState withStringProperty(BlockState state, String propertyName, String value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return withStringProperty(state, property, value);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withStringProperty(
            BlockState state,
            Property<T> property,
            String value
    ) {
        return property.getValue(value)
                .map(parsed -> state.setValue(property, parsed))
                .orElse(state);
    }
}
