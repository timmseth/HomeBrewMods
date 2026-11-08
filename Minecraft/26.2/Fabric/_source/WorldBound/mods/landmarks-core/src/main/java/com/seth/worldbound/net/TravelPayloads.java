package com.seth.worldbound.net;

import com.seth.worldbound.WorldBoundMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Map travel: the places a player has discovered (S2C), a trip request (C2S) and trip progress (S2C). */
public final class TravelPayloads {
    /** Discovered anchor points (lat/lon pairs) and the travel range around each, in km. */
    public record Anchors(double rangeKm, double[] latLon) implements CustomPacketPayload {
        public static final Type<Anchors> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "travel_anchors"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Anchors> CODEC = CustomPacketPayload.codec(Anchors::write, Anchors::read);

        private void write(RegistryFriendlyByteBuf b) {
            b.writeDouble(rangeKm);
            b.writeVarInt(latLon.length);
            for (double v : latLon) b.writeFloat((float) v);
        }

        private static Anchors read(RegistryFriendlyByteBuf b) {
            double r = b.readDouble();
            double[] a = new double[Math.min(b.readVarInt(), 200_000)];
            for (int i = 0; i < a.length; i++) a[i] = b.readFloat();
            return new Anchors(r, a);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Request(double lat, double lon) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "travel_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = CustomPacketPayload.codec(Request::write, Request::read);

        private void write(RegistryFriendlyByteBuf b) {
            b.writeDouble(lat);
            b.writeDouble(lon);
        }

        private static Request read(RegistryFriendlyByteBuf b) {
            return new Request(b.readDouble(), b.readDouble());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** percent 0..100; done = arrived (or failed, with a message). */
    public record Progress(String label, int percent, boolean done) implements CustomPacketPayload {
        public static final Type<Progress> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "travel_progress"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Progress> CODEC = CustomPacketPayload.codec(Progress::write, Progress::read);

        private void write(RegistryFriendlyByteBuf b) {
            b.writeUtf(label);
            b.writeVarInt(percent);
            b.writeBoolean(done);
        }

        private static Progress read(RegistryFriendlyByteBuf b) {
            return new Progress(b.readUtf(), b.readVarInt(), b.readBoolean());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
