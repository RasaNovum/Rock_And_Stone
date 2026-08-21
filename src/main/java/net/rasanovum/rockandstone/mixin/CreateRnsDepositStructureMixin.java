package net.rasanovum.rockandstone.mixin;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.rasanovum.rockandstone.compat.CreateRnsTerrainFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "com.bmaster.createrns.content.deposit.worldgen.DepositStructure", remap = false)
public abstract class CreateRnsDepositStructureMixin {
    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void rockandstone$filterTerrain(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback
    ) {
        Optional<Structure.GenerationStub> result = callback.getReturnValue();
        if (result.isEmpty()) {
            return;
        }

        Structure structure = (Structure) (Object) this;
        if (!CreateRnsTerrainFilter.accepts(structure, context, result.get().position())) {
            callback.setReturnValue(Optional.empty());
        }
    }
}
