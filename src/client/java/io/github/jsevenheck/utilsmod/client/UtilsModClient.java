package io.github.jsevenheck.utilsmod.client;

import io.github.jsevenheck.utilsmod.client.feature.FeatureRegistry;
import net.fabricmc.api.ClientModInitializer;

/**
 * Central client entrypoint for the mod. Delegates to {@link FeatureRegistry}, which owns and
 * initializes every individual client-side feature (compass HUD, inventory sorting, ...).
 */
public class UtilsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FeatureRegistry.initializeAll();
    }
}

