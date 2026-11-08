package com.seth.fossilrecord;

import net.fabricmc.api.ModInitializer;

/** Entrypoint of the shared Fossil Record library (also called by Earth/Earthish Fossil Record, whichever runs first). */
public final class FossilRecordInit implements ModInitializer {
    @Override
    public void onInitialize() {
        FossilRecord.init();
    }
}
