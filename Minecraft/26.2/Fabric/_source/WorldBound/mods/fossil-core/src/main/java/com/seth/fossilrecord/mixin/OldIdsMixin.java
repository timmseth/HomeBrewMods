package com.seth.fossilrecord.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * The animals were WorldBound's before Fossil Record (worldbound:tyrannosaurus...). Saved worlds still name them
 * that way, so an unknown worldbound: id loads as the fossilrecord: animal of the same name.
 */
@Mixin(EntityType.class)
public abstract class OldIdsMixin {
    @Inject(method = "by(Lnet/minecraft/world/level/storage/ValueInput;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private static void fossilrecord$oldIds(ValueInput in, CallbackInfoReturnable<Optional<EntityType<?>>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        String id = in.getStringOr("id", "");
        if (!id.startsWith("worldbound:")) return;
        Identifier now = Identifier.fromNamespaceAndPath("fossilrecord", id.substring("worldbound:".length()));
        BuiltInRegistries.ENTITY_TYPE.getOptional(now).ifPresent(t -> cir.setReturnValue(Optional.of(t)));
    }
}
