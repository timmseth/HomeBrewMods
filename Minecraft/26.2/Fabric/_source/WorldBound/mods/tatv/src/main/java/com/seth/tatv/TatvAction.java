package com.seth.tatv;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Rider -> server: 0 = next mode, 1 = toggle anchor. */
public record TatvAction(int entityId, int action) implements CustomPacketPayload {
    public static final Type<TatvAction> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tatv", "action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TatvAction> CODEC = CustomPacketPayload.codec(TatvAction::write, TatvAction::read);

    private void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(entityId);
        b.writeVarInt(action);
    }

    private static TatvAction read(RegistryFriendlyByteBuf b) {
        return new TatvAction(b.readVarInt(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
