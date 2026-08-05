package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jsevenheck.utilsmod.client.config.ModConfig;
import io.github.jsevenheck.utilsmod.client.feature.ModFeature;
import io.github.jsevenheck.utilsmod.client.feature.bundle.BundleScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the (rebindable) inventory-sort keybinding, defaulting to R in the vanilla "Inventory"
 * category, and drives {@link InventorySortController} once per client tick.
 *
 * <p>Vanilla only feeds ordinary {@link KeyMapping} click/press state while no {@link
 * net.minecraft.client.gui.screens.Screen} has input focus (the same reason movement keys stop working
 * while a screen is open) -- so {@link KeyMapping#consumeClick()} alone would never fire while an
 * inventory/container screen is open, which is the only time this feature is ever useful. To work
 * around that, every {@link AbstractContainerScreen} and the virtual Bundle screen additionally gets
 * its own {@link ScreenKeyboardEvents#afterKeyPress} listener that compares the raw key event against
 * the mapping's currently bound key (respecting rebinding) and raises a pending-trigger flag, consumed
 * the same way as a normal {@code consumeClick()} on the next tick.
 */
public final class InventorySortFeature implements ModFeature {

    private final InventorySortController controller = new InventorySortController();

    @Override
    public void initializeClient() {
        KeyMapping sortKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.compass-hud.sort_inventory", InputConstants.KEY_R, KeyMapping.Category.INVENTORY));

        AtomicBoolean screenTriggerPending = new AtomicBoolean();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> || screen instanceof BundleScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((scr, event) -> {
                    InputConstants.Key boundKey = KeyMappingHelper.getBoundKeyOf(sortKey);
                    if (InputConstants.getKey(event).equals(boundKey)) {
                        screenTriggerPending.set(true);
                    }
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            boolean pressed = sortKey.consumeClick() || screenTriggerPending.getAndSet(false);
            controller.tick(minecraft, ModConfig.get(), pressed);
        });
    }
}
