package net.rasanovum.rockandstone.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.rasanovum.rockandstone.RockAndStone;

import java.util.stream.Stream;

public class NoiseFilterPlacementModifier extends PlacementModifier {

    private final float minHumidity, maxHumidity;
    private final float minTemp, maxTemp;
    private final float minErosion, maxErosion;
    private final float minContinentalness, maxContinentalness;
    private final float minRidges, maxRidges;
    public static final Codec<NoiseFilterPlacementModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("min_humidity").orElse(-2.0f).forGetter(m -> m.minHumidity),
                    Codec.FLOAT.fieldOf("max_humidity").orElse(2.0f).forGetter(m -> m.maxHumidity),
                    Codec.FLOAT.fieldOf("min_temp").orElse(-2.0f).forGetter(m -> m.minTemp),
                    Codec.FLOAT.fieldOf("max_temp").orElse(2.0f).forGetter(m -> m.maxTemp),
                    Codec.FLOAT.fieldOf("min_erosion").orElse(-2.0f).forGetter(m -> m.minErosion),
                    Codec.FLOAT.fieldOf("max_erosion").orElse(2.0f).forGetter(m -> m.maxErosion),
                    Codec.FLOAT.fieldOf("min_continentalness").orElse(-2.0f).forGetter(m -> m.minContinentalness),
                    Codec.FLOAT.fieldOf("max_continentalness").orElse(2.0f).forGetter(m -> m.maxContinentalness),
                    Codec.FLOAT.fieldOf("min_ridges").orElse(-2.0f).forGetter(m -> m.minRidges),
                    Codec.FLOAT.fieldOf("max_ridges").orElse(2.0f).forGetter(m -> m.maxRidges)
            ).apply(instance, NoiseFilterPlacementModifier::new)
    );

    public NoiseFilterPlacementModifier(float minH, float maxH, float minT, float maxT, float minE, float maxE, float minC, float maxC, float minPV, float maxPV) {
        this.minHumidity = minH;
        this.maxHumidity = maxH;
        this.minTemp = minT;
        this.maxTemp = maxT;
        this.minErosion = minE;
        this.maxErosion = maxE;
        this.minContinentalness = minC;
        this.maxContinentalness = maxC;
        this.minRidges = minPV;
        this.maxRidges = maxPV;
    }

    public double getMinTemp() { return this.minTemp; }
    public double getMaxTemp() { return this.maxTemp; }
    public double getMinHumidity() { return this.minHumidity; }
    public double getMaxHumidity() { return this.maxHumidity; }
    public double getMinErosion() { return this.minErosion; }
    public double getMaxErosion() { return this.maxErosion; }
    public double getMinContinentalness() { return this.minContinentalness; }
    public double getMaxContinentalness() { return this.maxContinentalness; }
    public double getMinRidges() { return this.minRidges; }
    public double getMaxRidges() { return this.maxRidges; }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();

        if (level.getChunkSource() instanceof ServerChunkCache chunkCache) {
            NoiseRouter noiseRouter = chunkCache.randomState().router();
            DensityFunction.SinglePointContext contextPoint = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());

            double temp = noiseRouter.temperature().compute(contextPoint);
            double humid = noiseRouter.vegetation().compute(contextPoint);
            double continental = noiseRouter.continents().compute(contextPoint);
            double erosionVal = noiseRouter.erosion().compute(contextPoint);
            double weirdness = noiseRouter.ridges().compute(contextPoint);
            double pv = 1.0 - Math.abs(3.0 * Math.abs(weirdness) - 2.0);

            boolean isCorrectRegion =
                    temp >= minTemp && temp <= maxTemp &&
                            humid >= minHumidity && humid <= maxHumidity &&
                            erosionVal >= minErosion && erosionVal <= maxErosion &&
                            continental >= minContinentalness && continental <= maxContinentalness &&
                            pv >= minRidges && pv <= maxRidges;

            if (isCorrectRegion) {
                return Stream.of(pos);
            }
        }
        return Stream.empty();
    }


    @Override
    public PlacementModifierType<?> type() {
        return RockAndStone.NOISE_FILTER_TYPE;
    }
}