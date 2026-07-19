package net.rasanovum.rockandstone.loaders.neoforge.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.util.VersionUtils;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ModifiableBiomeInfo.class)
public abstract class ModifiableBiomeInfoMixin {
    @Shadow
    @Nullable
    private ModifiableBiomeInfo.BiomeInfo modifiedBiomeInfo;

    @Inject(method = "applyBiomeModifiers", at = @At("RETURN"))
    private void rockAndStone$applyOreChanges(
            Holder<Biome> biome,
            List<?> biomeModifiers,
            net.minecraft.core.RegistryAccess registryAccess,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!RockAndStoneConfig.doOreReplacement || !biome.is(BiomeTags.IS_OVERWORLD) || modifiedBiomeInfo == null) {
            return;
        }

        Registry<PlacedFeature> placedFeatures = registryAccess.registryOrThrow(Registries.PLACED_FEATURE);
        Map<String, DynamicOreRequirements.NoiseBounds> filters = discoverFilters(placedFeatures);
        if (filters.isEmpty()) {
            return;
        }

        ModifiableBiomeInfo.BiomeInfo.Builder builder =
                ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(modifiedBiomeInfo);
        List<Holder<PlacedFeature>> undergroundOres = builder.getGenerationSettings()
                .getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);

        for (String customOrePath : filters.keySet()) {
            ResourceLocation targetLocation = DynamicOreRequirements.targetFeatureId(customOrePath).orElseThrow();
            ResourceKey<PlacedFeature> targetKey = ResourceKey.create(Registries.PLACED_FEATURE, targetLocation);
            boolean removed = undergroundOres.removeIf(holder -> holder.unwrapKey().map(targetKey::equals).orElse(false));
            if (removed) {
                RockAndStone.LOGGER.debug("Removed {} and replaced with {}", targetLocation, customOrePath);
            }

            ResourceLocation customLocation = VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, customOrePath);
            placedFeatures.getHolder(customLocation).ifPresent(customHolder -> {
                if (!undergroundOres.contains(customHolder)) {
                    builder.getGenerationSettings().addFeature(
                            GenerationStep.Decoration.UNDERGROUND_ORES,
                            customHolder
                    );
                }
            });
        }

        modifiedBiomeInfo = builder.build();
    }

    private static Map<String, DynamicOreRequirements.NoiseBounds> discoverFilters(Registry<PlacedFeature> placedFeatures) {
        Map<String, DynamicOreRequirements.NoiseBounds> filters = new LinkedHashMap<>();

        for (Map.Entry<ResourceKey<PlacedFeature>, PlacedFeature> entry : placedFeatures.entrySet()) {
            ResourceLocation location = entry.getKey().location();
            if (!location.getNamespace().equals(RockAndStone.MOD_ID)
                    || !location.getPath().startsWith("filtered_")) {
                continue;
            }

            for (PlacementModifier modifier : entry.getValue().placement()) {
                if (modifier instanceof NoiseFilterPlacementModifier filter) {
                    filters.put(location.getPath(), new DynamicOreRequirements.NoiseBounds(
                            filter.getMinTemp(), filter.getMaxTemp(),
                            filter.getMinHumidity(), filter.getMaxHumidity(),
                            filter.getMinErosion(), filter.getMaxErosion(),
                            filter.getMinRidges(), filter.getMaxRidges(),
                            filter.getMinContinentalness(), filter.getMaxContinentalness()
                    ));
                    break;
                }
            }
        }

        return filters;
    }
}
