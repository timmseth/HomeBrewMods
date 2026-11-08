package com.seth.worldbound.net;

import com.seth.worldbound.WorldBoundMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: 0 = travel to landmark {@code index}, 1 = open the landmark menu. */
public record LandmarkActionPayload(int action, int index) implements CustomPacketPayload {
    public static final int TRAVEL = 0, OPEN = 1;
    public static final Type<LandmarkActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "landmark_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LandmarkActionPayload> CODEC =
            CustomPacketPayload.codec(LandmarkActionPayload::write, LandmarkActionPayload::read);

    private void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(action);
        b.writeVarInt(index);
    }

    private static LandmarkActionPayload read(RegistryFriendlyByteBuf b) {
        return new LandmarkActionPayload(b.readVarInt(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
