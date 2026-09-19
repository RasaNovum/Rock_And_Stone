package net.rasanovum.rockandstone;

import eu.midnightdust.lib.config.MidnightConfig;

public class RockAndStoneConfig extends MidnightConfig {
    @Entry public static boolean doRockAndStoneDebug = false;
    @Entry public static boolean doOreReplacement = true;
    @Entry public static boolean doCreateRnsTerrainFiltering = false;
    @Entry public static boolean doLargeOreDepositsTerrainFiltering = false;
    //? if <1.21 {
    /*@Hidden
    *///?} else {
    @Condition(requiredModId = "surfacesamples")
    //?}
    @Entry(min = 0.0, max = 1.0, isSlider = true)
    public static double surfaceSampleFrequency = 0.05;
    @Entry public static boolean doSilentAdvancements = false;
}
