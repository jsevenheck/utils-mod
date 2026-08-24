package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.client.config.ModConfig;
import io.github.jsevenheck.utilsmod.feature.InventoryOperationLock;
import io.github.jsevenheck.utilsmod.feature.inventorysort.ClickOperation;
import io.github.jsevenheck.utilsmod.feature.inventorysort.InventoryClickPlanner;
import io.github.jsevenheck.utilsmod.feature.inventorysort.InventorySortPlanner;
import io.github.jsevenheck.utilsmod.feature.inventorysort.SortSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates a single sort trigger: resolves the current screen into sortable slots, plans the
 * click queue, and hands it to an {@link InventoryClickExecutor}. Only ever one execution runs at a
 * time; a trigger while one is already running is silently ignored (debounced), matching the
 * "another sort already running" abort condition.
 */
final class InventorySortController {

    private InventoryClickExecutor activeExecution;

    void tick(Minecraft minecraft, ModConfig config, boolean triggerPressed) {
        if (activeExecution != null) {
            if (activeExecution.tick(minecraft)) {
                if (activeExecution.wasAborted()) {
                    feedback(minecraft, "compass-hud.inventorysort.aborted");
                }
                InventoryOperationLock.release("inventory-sort");
                activeExecution = null;
            }
            return;
        }

        if (!triggerPressed) {
            return;
        }
        if (!config.inventorySortEnabled) {
            return;
        }

        if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?>)) {
            // No relevant screen open at all -- do nothing, silently, as if the key wasn't bound to anything here.
            return;
        }

        Optional<SortSession> sessionOpt = SortableSlotResolver.resolve(minecraft);
        if (sessionOpt.isEmpty()) {
            feedback(minecraft, "compass-hud.inventorysort.unsupported_menu");
            return;
        }
        SortSession session = sessionOpt.get();

        if (!session.menu().getCarried().isEmpty()) {
            feedback(minecraft, "compass-hud.inventorysort.cursor_not_empty");
            return;
        }

        List<ClickOperation> operations = planOperations(session);
        if (operations.isEmpty()) {
            feedback(minecraft, "compass-hud.inventorysort.already_sorted");
            return;
        }

        if (!InventoryOperationLock.tryAcquire("inventory-sort")) {
            return;
        }
        activeExecution = new InventoryClickExecutor(session.menu(), operations, config.effectiveClickDelayTicks());
    }

    private static List<ClickOperation> planOperations(SortSession session) {
        List<SortSlot> slots = session.slots();
        if (slots.isEmpty()) {
            return List.of();
        }
        List<SortSlot> target = InventorySortPlanner.plan(slots);
        return InventoryClickPlanner.plan(slots, target, session.pickupAllSafeIdentities());
    }

    private static void feedback(Minecraft minecraft, String translationKey) {
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(translationKey));
        }
    }
}
