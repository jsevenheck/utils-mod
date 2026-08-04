package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.ClickOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Executes a planned queue of {@link ClickOperation}s against a real menu, one interaction every
 * {@code delayTicks} client ticks, exclusively through {@code MultiPlayerGameMode.handleContainerInput}
 * -- the same client-facing API vanilla screens use, which predicts locally and sends the normal
 * {@code ServerboundContainerClickPacket}, so the server always stays authoritative. This class never
 * mutates the menu/slots directly.
 * <p>
 * Immediately before every click it validates the real slot content and cursor against what the plan
 * expects to find; on any mismatch (or if the screen/menu/container id changed, or the player is gone)
 * it aborts the remaining queue without firing anything further. Since every planned click leaves the
 * cursor empty once its whole micro-operation completes, an unexpectedly non-empty cursor after the
 * final click is treated as an anomaly rather than a silent success.
 */
final class InventoryClickExecutor {

    private final AbstractContainerMenu menu;
    private final int containerId;
    private final List<ClickOperation> operations;
    private final int delayTicks;

    private int index;
    private int ticksUntilNextClick;
    private boolean aborted;
    private boolean finished;

    InventoryClickExecutor(AbstractContainerMenu menu, List<ClickOperation> operations, int delayTicks) {
        this.menu = menu;
        this.containerId = menu.containerId;
        this.operations = operations;
        this.delayTicks = Math.max(1, delayTicks);
    }

    boolean isDone() {
        return finished || aborted;
    }

    boolean wasAborted() {
        return aborted;
    }

    /**
     * Advances the queue by at most one click. Returns {@code true} once this execution has finished
     * (successfully or aborted) as of this tick, so the caller can drop its reference.
     */
    boolean tick(Minecraft minecraft) {
        if (isDone()) {
            return true;
        }
        if (!stillValid(minecraft)) {
            aborted = true;
            return true;
        }
        if (index >= operations.size()) {
            finished = menu.getCarried().isEmpty();
            aborted = !finished;
            return true;
        }
        if (ticksUntilNextClick > 0) {
            ticksUntilNextClick--;
            return false;
        }

        ClickOperation op = operations.get(index);
        if (!matches(op)) {
            aborted = true;
            return true;
        }

        ContainerInput input = op.kind() == ClickOperation.Kind.PICKUP_ALL
            ? ContainerInput.PICKUP_ALL
            : ContainerInput.PICKUP;
        minecraft.gameMode.handleContainerInput(containerId, op.logicalSlot(), 0, input, minecraft.player);
        index++;
        // A delay of one means one click per client tick. Larger values add ticks between clicks.
        ticksUntilNextClick = delayTicks - 1;

        if (index >= operations.size()) {
            finished = menu.getCarried().isEmpty();
            aborted = !finished;
        }
        return isDone();
    }

    private boolean stillValid(Minecraft minecraft) {
        return minecraft.player != null
            && minecraft.level != null
            && minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen
            && screen.getMenu() == menu
            && menu.containerId == containerId;
    }

    private boolean matches(ClickOperation op) {
        Slot slot = menu.getSlot(op.logicalSlot());
        ItemStack cursor = menu.getCarried();
        if (op.expectedCursorEmptyBefore() != cursor.isEmpty()) {
            return false;
        }
        ItemStack current = slot.getItem();
        if (op.expectedSlotEmpty()) {
            return current.isEmpty();
        }
        return !current.isEmpty()
            && current.getCount() == op.expectedCount()
            && ItemIdentities.of(current).equals(op.expectedIdentity());
    }
}
