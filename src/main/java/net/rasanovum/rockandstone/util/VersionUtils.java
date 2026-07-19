package net.rasanovum.rockandstone.util;

import net.minecraft.resources.ResourceLocation;

public final class VersionUtils {
    private VersionUtils() {
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        //? if <1.21 {
        /*return new ResourceLocation(namespace, path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?}
    }
}
