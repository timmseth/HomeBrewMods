package com.seth.worldbound.client;

import net.fabricmc.api.ClientModInitializer;

/** Client side of Landmarks: the travel map key (K), the landmark list and guide, the travel screen. */
public final class LandmarksClient implements ClientModInitializer {
    private static boolean initialised;

    @Override
    public void onInitializeClient() {
        init();
    }

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        WorldBoundClient.init();
        LandmarkClient.register(EarthHud.CATEGORY);
        WorldBoundClient.travelMap = LandmarkClient::openMap;
    }
}
