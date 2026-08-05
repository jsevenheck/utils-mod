package io.github.jsevenheck.utilsmod.client.feature.bundle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

/** Tick-paced executor for validated operations against the real player InventoryMenu. */
final class BundleInteractionExecutor {

    private final BundleScreen screen;
    private final AbstractContainerMenu menu;
    private final Inventory playerInventory;
    private final int bundleSlotIndex;
    private final BundleInteractionPlanner.Plan plan;

    private int stepIndex;
    private boolean aborted;
    private boolean finished;
    private boolean settling;

    BundleInteractionExecutor(
        BundleScreen screen,
        AbstractContainerMenu menu,
        Inventory playerInventory,
        int bundleSlotIndex,
        BundleInteractionPlanner.Plan plan
    ) {
        this.screen = screen;
        this.menu = menu;
        this.playerInventory = playerInventory;
        this.bundleSlotIndex = bundleSlotIndex;
        this.plan = plan;
    }

    boolean wasAborted() {
        return aborted;
    }

    boolean tick(Minecraft minecraft) {
        if (finished || aborted) {
            return true;
        }
        if (!stillValid(minecraft)) {
            aborted = true;
            return true;
        }
        if (stepIndex >= plan.steps().size()) {
            if (!settling) {
                // Give the normal server response one client tick to correct the predictive local
                // menu state before declaring a multi-click operation successful.
                settling = true;
                return false;
            }
            if (!(plan.steps().get(plan.steps().size() - 1) instanceof BundleInteractionPlanner.BundleClickStep last)
                || !matchesAfter(last)
                || (plan.cursorMustBeEmpty() && !menu.getCarried().isEmpty())) {
                aborted = true;
            } else {
                finished = true;
            }
            return true;
        }

        BundleInteractionPlanner.BundleStep step = plan.steps().get(stepIndex);
        if (step instanceof BundleInteractionPlanner.BundleSelectionStep selection) {
            ItemStack actual = menu.getSlot(selection.bundleSlotIndex()).getItem();
            if (!BundleInteractionPlanner.same(actual, selection.expectedBefore()) || !menu.getCarried().isEmpty()) {
                aborted = true;
                return true;
            }
            if (minecraft.getConnection() == null) {
                aborted = true;
                return true;
            }
            BundleItem.toggleSelectedItem(actual, selection.selectedIndex());
            minecraft.getConnection().send(new ServerboundSelectBundleItemPacket(selection.bundleSlotIndex(), selection.selectedIndex()));
        } else if (step instanceof BundleInteractionPlanner.BundleClickStep click) {
            if (!matches(click)) {
                aborted = true;
                return true;
            }
            minecraft.gameMode.handleContainerInput(
                menu.containerId,
                click.slotIndex(),
                click.button(),
                click.input(),
                minecraft.player
            );
        }

        stepIndex++;
        return false;
    }

    void cancel() {
        if (!finished) {
            aborted = true;
        }
    }

    private boolean stillValid(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != screen) {
            return false;
        }
        if (minecraft.player.getInventory() != playerInventory || minecraft.player.containerMenu != menu) {
            return false;
        }
        if (bundleSlotIndex < 0 || bundleSlotIndex >= menu.slots.size()) {
            return false;
        }
        Slot bundleSlot = menu.getSlot(bundleSlotIndex);
        return bundleSlot.container == playerInventory && bundleSlot.getItem().is(ItemTags.BUNDLES);
    }

    private boolean matches(BundleInteractionPlanner.BundleClickStep click) {
        if (!BundleInteractionPlanner.same(menu.getSlot(bundleSlotIndex).getItem(), click.expectedBundle())) {
            return false;
        }
        if (!BundleInteractionPlanner.same(menu.getCarried(), click.expectedCursor())) {
            return false;
        }
        return click.slotIndex() >= 0
            && click.slotIndex() < menu.slots.size()
            && BundleInteractionPlanner.same(menu.getSlot(click.slotIndex()).getItem(), click.expectedSlot());
    }

    private boolean matchesAfter(BundleInteractionPlanner.BundleClickStep click) {
        return BundleInteractionPlanner.same(menu.getSlot(bundleSlotIndex).getItem(), click.expectedBundleAfter())
            && BundleInteractionPlanner.same(menu.getCarried(), click.expectedCursorAfter())
            && click.slotIndex() >= 0
            && click.slotIndex() < menu.slots.size()
            && BundleInteractionPlanner.same(menu.getSlot(click.slotIndex()).getItem(), click.expectedSlotAfter());
    }
}
