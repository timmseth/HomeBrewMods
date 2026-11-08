package com.seth.worldbound.mixin;

import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Saving a chunk packs every section. Earth chunks are 254 sections tall and almost all of them are one block
 * (stone below, air above); vanilla still unpacks 4,096 entries per section to find that one value. Pack those
 * directly: same result (one palette entry, no storage), without the work.
 */
@Mixin(PalettedContainer.class)
public abstract class PackSingleValueMixin<T> {
    @Inject(method = "pack", at = @At("HEAD"), cancellable = true)
    private void worldbound$packSingle(Strategy<T> strategy, CallbackInfoReturnable<PalettedContainerRO.PackedData<T>> cir) {
        PalettedContainer<T> self = (PalettedContainer<T>) (Object) this;
        if (self.bitsPerEntry() != 0) return;
        T v = self.get(0, 0, 0);
        if (v != null) cir.setReturnValue(new PalettedContainerRO.PackedData<>(List.of(v), Optional.empty(), 0));
    }
}
