package io.github.jsevenheck.utilsmod.client.feature.compass;

import io.github.jsevenheck.utilsmod.UtilsMod;
import io.github.jsevenheck.utilsmod.client.feature.ModFeature;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/** Registers the compass HUD element. See {@link CompassHudRenderer} for the actual rendering logic. */
public final class CompassHudFeature implements ModFeature {

    @Override
    public void initializeClient() {
        HudElementRegistry.addLast(UtilsMod.id("compass"), new CompassHudRenderer());
    }
}
