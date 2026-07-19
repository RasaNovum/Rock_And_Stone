package net.rasanovum.rockandstone.loaders.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.util.OreScanner;
import net.rasanovum.rockandstone.util.VersionUtils;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        RockAndStone.NOISE_FILTER_TYPE = Registry.register(
                BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
                VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "noise_filter"),
                NoiseFilterPlacementModifier.createType()
        );

        RockAndStone.initialize();
        RockAndStone.registerAdvancementTrigger();
        registerDataPackListener();
        registerSilentAdvancementsPack();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                OreScanner.register(dispatcher));

        BiomeModifications.create(VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "dynamic_ore_removal"))
                .add(ModificationPhase.POST_PROCESSING,
                        BiomeSelectors.foundInOverworld(),
                        context -> {
                            if (!RockAndStoneConfig.doOreReplacement) {
                                return;
                            }

                            for (String customOrePath : DynamicOreRequirements.activeFilters().keySet()) {
                                ResourceLocation targetLocation = DynamicOreRequirements.targetFeatureId(customOrePath)
                                        .orElseThrow();
                                ResourceKey<PlacedFeature> targetKey =
                                        ResourceKey.create(Registries.PLACED_FEATURE, targetLocation);

                                boolean removed = context.getGenerationSettings().removeFeature(
                                        GenerationStep.Decoration.UNDERGROUND_ORES,
                                        targetKey
                                );
                                if (removed) {
                                    RockAndStone.LOGGER.debug("Removed {} and replaced with {}",
                                            targetLocation, customOrePath);
                                }
                            }
                        }
                );

        BiomeModifications.create(VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "dynamic_ore_injector"))
                .add(ModificationPhase.ADDITIONS,
                        BiomeSelectors.foundInOverworld(),
                        context -> {
                            if (!RockAndStoneConfig.doOreReplacement) {
                                return;
                            }

                            for (String customOrePath : DynamicOreRequirements.activeFilters().keySet()) {
                                ResourceLocation customLocation =
                                        VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, customOrePath);
                                ResourceKey<PlacedFeature> customKey =
                                        ResourceKey.create(Registries.PLACED_FEATURE, customLocation);

                                context.getGenerationSettings().addFeature(
                                        GenerationStep.Decoration.UNDERGROUND_ORES,
                                        customKey
                                );
                            }
                        }
                );

        ServerTickEvents.END_SERVER_TICK.register(RockAndStone::onServerTick);
    }

    private static void registerDataPackListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            private final PreparableReloadListener delegate = DynamicOreRequirements.createReloadListener();

            @Override
            public ResourceLocation getFabricId() {
                return VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "dynamic_ore_requirements");
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
                return delegate.reload(preparationBarrier, resourceManager, preparationsProfiler,
                        reloadProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }

    private static void registerSilentAdvancementsPack() {
        if (!RockAndStoneConfig.doSilentAdvancements) {
            return;
        }

        FabricLoader.getInstance().getModContainer(RockAndStone.MOD_ID).ifPresentOrElse(modContainer -> {
            boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
                    VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "silent_advancements"),
                    modContainer,
                    ResourcePackActivationType.ALWAYS_ENABLED
            );
            if (registered) {
                RockAndStone.LOGGER.info(
                        "Silent advancements are enabled; registering the built-in silent advancements datapack.");
            } else {
                RockAndStone.LOGGER.error(
                        "Silent advancements are enabled, but the built-in datapack could not be registered.");
            }
        }, () -> RockAndStone.LOGGER.error(
                "Silent advancements are enabled, but the {} mod container could not be found.",
                RockAndStone.MOD_ID));
    }
}
