package net.rasanovum.rockandstone.mixin;

import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import net.minecraft.core.RegistryAccess;
import net.rasanovum.rockandstone.util.DynamicOreRequirements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeModificationImpl.class)
public abstract class ServerWorldgenMixin {
    @Inject(
            method = "finalizeWorldGen",
            at = @At("HEAD"),
            remap = false
    )
    private void rockAndStone$loadDatapackFiltersBeforeBiomeFinalization(RegistryAccess registryAccess, CallbackInfo ci) {
        DynamicOreRequirements.loadActiveFilters(registryAccess);
    }
}
