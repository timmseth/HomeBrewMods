package com.seth.worldbound.net;

import com.seth.worldbound.WorldBoundMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: landmark obelisk state for this player. Indices are positions in the landmark list
 * (worldbound/landmarks.tsv, identical on both sides). {@code open} asks the client to show the menu;
 * {@code at} is the obelisk the player is standing at (-1 if none), the only place travel is allowed from.
 */
public record LandmarkSyncPayload(boolean enabled, boolean open, int at, int[] discovered, int[] placed, int[] placedX, int[] placedZ)
        implements CustomPacketPayload {
    public static final Type<LandmarkSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "landmarks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LandmarkSyncPayload> CODEC =
            CustomPacketPayload.codec(LandmarkSyncPayload::write, LandmarkSyncPayload::read);

    private static void ints(RegistryFriendlyByteBuf b, int[] a) {
        b.writeVarInt(a.length);
        for (int v : a) b.writeInt(v);
    }

    private static int[] ints(RegistryFriendlyByteBuf b) {
        int[] a = new int[Math.min(b.readVarInt(), 100_000)];
        for (int i = 0; i < a.length; i++) a[i] = b.readInt();
        return a;
    }

    private void write(RegistryFriendlyByteBuf b) {
        b.writeBoolean(enabled);
        b.writeBoolean(open);
        b.writeInt(at);
        ints(b, discovered);
        ints(b, placed);
        ints(b, placedX);
        ints(b, placedZ);
    }

    private static LandmarkSyncPayload read(RegistryFriendlyByteBuf b) {
        return new LandmarkSyncPayload(b.readBoolean(), b.readBoolean(), b.readInt(), ints(b), ints(b), ints(b), ints(b));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
