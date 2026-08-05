package io.github.jsevenheck.utilsmod.client.feature;

/**
 * A single, independently registered client-side feature of the mod (e.g. the compass HUD, inventory
 * sorting, or Bundle UI). Each feature owns its own registration: HUD elements, keybindings, screens,
 * and tick/event listeners.
 */
public interface ModFeature {

    /** Registers everything the feature needs with Fabric/Minecraft. Called once, during client init. */
    void initializeClient();
}
