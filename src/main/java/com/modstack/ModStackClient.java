package com.modstack;

import net.fabricmc.api.ClientModInitializer;

public class ModStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Nothing client-specific yet — stack-count nametags render for free
        // via the vanilla custom-name rendering since we use setCustomName().
        // Hook here later if you want a custom stack-count HUD/overlay.
    }
}
