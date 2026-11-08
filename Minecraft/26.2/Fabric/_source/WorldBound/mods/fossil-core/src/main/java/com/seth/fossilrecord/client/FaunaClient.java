package com.seth.fossilrecord.client;

import com.seth.worldbound.WorldBoundMod;
import com.seth.fossilrecord.Fauna;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** Client side of WorldBound's animals: their model layers and renderers. */
public final class FaunaClient implements net.fabricmc.api.ClientModInitializer {

    public static final ModelLayerLocation KANGAROO = new ModelLayerLocation(Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "kangaroo"), "main");
    public static final ModelLayerLocation ANACONDA = new ModelLayerLocation(Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "anaconda"), "main");

    private static boolean initialised;

    @Override
    public void onInitializeClient() {
        register();
    }

    public static synchronized void register() {
        if (initialised) return;
        initialised = true;
        ModelLayerRegistry.registerModelLayer(KANGAROO, KangarooModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ANACONDA, AnacondaModel::createBodyLayer);
        EntityRendererRegistry.register(Fauna.KANGAROO, KangarooRenderer::new);
        EntityRendererRegistry.register(Fauna.ANACONDA, AnacondaRenderer::new);
        com.seth.fossilrecord.client.dino.DinoRenderer.register();
    }
}
