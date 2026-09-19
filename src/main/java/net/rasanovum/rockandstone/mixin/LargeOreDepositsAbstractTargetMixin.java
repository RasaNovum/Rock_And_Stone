package net.rasanovum.rockandstone.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.rasanovum.rockandstone.compat.LargeOreDepositsTerrainFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(targets = "com.endertech.minecraft.mods.adlods.target.AbstractTarget", remap = false)
public abstract class LargeOreDepositsAbstractTargetMixin {
    @Inject(
            method = "generateAt(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/core/BlockPos;IZLjava/util/Random;)Lcom/endertech/minecraft/mods/adlods/target/TargetGenResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rockandstone$filterTerrain(
            WorldGenLevel level,
            BlockPos position,
            int size,
            boolean testing,
            Random random,
            CallbackInfoReturnable<Object> callback
    ) {
        if (testing) {
            return;
        }
        LargeOreDepositsTerrainFilter.rejectedResult(this, level, position)
                .ifPresent(callback::setReturnValue);
    }
}
