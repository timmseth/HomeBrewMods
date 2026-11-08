package com.seth.worldbound.net;

import com.seth.worldbound.WorldBoundMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client on join: "this is an Earth world" plus the projection the server uses, and for an Earthish world
 * the planet's seed (the client builds the same planet for its map and horizon).
 */
public record EarthInfoPayload(double originLat, double originLon, double scale, boolean seasons, boolean wild, boolean planet, long planetSeed) implements CustomPacketPayload {
    public static final Type<EarthInfoPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "earth_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthInfoPayload> CODEC =
            CustomPacketPayload.codec(EarthInfoPayload::write, EarthInfoPayload::read);

    private void write(RegistryFriendlyByteBuf b) {
        b.writeDouble(originLat);
        b.writeDouble(originLon);
        b.writeDouble(scale);
        b.writeBoolean(seasons);
        b.writeBoolean(wild);
        b.writeBoolean(planet);
        b.writeLong(planetSeed);
    }

    private static EarthInfoPayload read(RegistryFriendlyByteBuf b) {
        return new EarthInfoPayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
