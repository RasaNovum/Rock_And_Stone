package net.rasanovum.rockandstone.loaders.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.rasanovum.rockandstone.RockAndStone;
import net.rasanovum.rockandstone.RockAndStoneConfig;
import net.rasanovum.rockandstone.util.AdvancementTrigger;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import net.rasanovum.rockandstone.util.OreScanner;
import net.rasanovum.rockandstone.util.VersionUtils;
import net.rasanovum.rockandstone.worldgen.NoiseFilterPlacementModifier;

@Mod(RockAndStone.MOD_ID)
public final class NeoForgeMain {
    public NeoForgeMain(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeMain::register);
        modEventBus.addListener(NeoForgeMain::addPackFinders);

        NeoForge.EVENT_BUS.addListener(NeoForgeMain::addReloadListener);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::registerCommands);
        NeoForge.EVENT_BUS.addListener(NeoForgeMain::onServerTick);

        RockAndStone.initialize();
    }

    private static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.TRIGGER_TYPES.key())) {
            event.register(BuiltInRegistries.TRIGGER_TYPES.key(),
                    VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "geological_hotspot"),
                    () -> {
                        AdvancementTrigger trigger = new AdvancementTrigger();
                        RockAndStone.HOTSPOT_TRIGGER = trigger;
                        return trigger;
                    });
        }

        if (event.getRegistryKey().equals(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.key())) {
            event.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.key(),
                    VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "noise_filter"),
                    () -> {
                        PlacementModifierType<NoiseFilterPlacementModifier> type =
                                NoiseFilterPlacementModifier.createType();
                        RockAndStone.NOISE_FILTER_TYPE = type;
                        return type;
                    });
        }
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        if (!RockAndStoneConfig.doSilentAdvancements || event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        event.addPackFinders(
                VersionUtils.fromNamespaceAndPath(RockAndStone.MOD_ID, "datapacks/silent_advancements"),
                PackType.SERVER_DATA,
                Component.translatable("pack.rockandstone.silent_advancements"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
        );
        RockAndStone.LOGGER.info("Silent advancements are enabled; registering the built-in silent advancements datapack.");
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(DynamicOreRequirements.createReloadListener());
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        OreScanner.register(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        RockAndStone.onServerTick(event.getServer());
    }
}
