package net.rasanovum.rockandstone.util;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class OreNameNormalizer {
    private OreNameNormalizer() {
    }

    public static String normalizeId(ResourceLocation id) {
        return id.getNamespace() + ":" + normalizePath(id.getPath());
    }

    public static String normalizePath(String name) {
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
        for (String suffix : new String[]{
                "_small", "_large", "_buried", "_upper", "_lower", "_middle", "_placer"
        }) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }
        return normalized;
    }
}
