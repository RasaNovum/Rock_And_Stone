package net.rasanovum.rockandstone;

import eu.midnightdust.lib.config.MidnightConfig;

public class RockAndStoneConfig extends MidnightConfig {
    @Entry public static boolean doRockAndStoneDebug = false;
    @Entry public static boolean doOreReplacement = true;
    @Entry public static boolean doCreateRnsTerrainFiltering = false;
    @Entry public static boolean doLargeOreDepositsTerrainFiltering = false;
    @Entry public static boolean doSilentAdvancements = false;
}
