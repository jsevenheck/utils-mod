package io.github.jsevenheck.utilsmod.client.feature;

import io.github.jsevenheck.utilsmod.client.feature.compass.CompassHudFeature;
import io.github.jsevenheck.utilsmod.client.feature.inventorysort.InventorySortFeature;

import java.util.List;

/** Central list of the mod's client-side features; {@link #initializeAll()} wires them all up. */
public final class FeatureRegistry {

    private static final List<ModFeature> FEATURES = List.of(
        new CompassHudFeature(),
        new InventorySortFeature()
    );

    private FeatureRegistry() {
    }

    public static void initializeAll() {
        for (ModFeature feature : FEATURES) {
            feature.initializeClient();
        }
    }
}
