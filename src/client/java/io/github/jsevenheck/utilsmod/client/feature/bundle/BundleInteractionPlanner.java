package io.github.jsevenheck.utilsmod.client.feature.bundle;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds conservative click sequences against the real player inventory menu.  The Bundle slots
 * in the screen are only a view; every step below targets a real slot and carries the exact state
 * it expects before sending the click.
 */
final class BundleInteractionPlanner {

    private BundleInteractionPlanner() {
    }

    static Plan extract(AbstractContainerMenu menu, int bundleSlotIndex, int selectedIndex, boolean quickMove) {
        if (menu.getCarried().isEmpty() == false || bundleSlotIndex < 0 || bundleSlotIndex >= menu.slots.size()) {
            return null;
        }

        ItemStack initialBundle = menu.getSlot(bundleSlotIndex).getItem().copy();
        BundleContents initialContents = initialBundle.get(DataComponents.BUNDLE_CONTENTS);
        if (initialContents == null || selectedIndex < 0 || selectedIndex >= initialContents.size()) {
            return null;
        }

        BundleContents.Mutable selectedMutable = new BundleContents.Mutable(initialContents);
        selectedMutable.toggleSelectedItem(selectedIndex);
        ItemStack selectedBundle = withContents(initialBundle, selectedMutable.toImmutable());

        BundleContents.Mutable removedMutable = new BundleContents.Mutable(selectedMutable.toImmutable());
        ItemStack removed = removedMutable.removeOne();
        if (removed == null || removed.isEmpty()) {
            return null;
        }
        ItemStack bundleAfterRemoval = withContents(initialBundle, removedMutable.toImmutable());

        List<BundleStep> steps = new ArrayList<>();
        steps.add(new BundleSelectionStep(bundleSlotIndex, selectedIndex, initialBundle, selectedBundle));
        steps.add(new BundleClickStep(
            bundleSlotIndex,
            1,
            ContainerInput.PICKUP,
            selectedBundle,
            ItemStack.EMPTY,
            selectedBundle,
            bundleAfterRemoval,
            removed,
            bundleAfterRemoval
        ));

        if (!quickMove) {
            return new Plan(List.copyOf(steps), false);
        }

        ItemStack cursor = removed.copy();
        ItemStack bundle = bundleAfterRemoval.copy();
        for (int slotIndex : playerSlotIndices(menu)) {
            if (slotIndex == bundleSlotIndex || cursor.isEmpty()) {
                continue;
            }

            Slot slot = menu.getSlot(slotIndex);
            ItemStack target = slot.getItem().copy();
            if (!target.isEmpty()
                && ItemStack.isSameItemSameComponents(target, cursor)
                && target.getCount() < Math.min(target.getMaxStackSize(), slot.getMaxStackSize(cursor))) {
                int capacity = Math.min(cursor.getMaxStackSize(), slot.getMaxStackSize(cursor)) - target.getCount();
                int moved = Math.min(capacity, cursor.getCount());
                ItemStack targetAfter = target.copyWithCount(target.getCount() + moved);
                ItemStack cursorAfter = cursor.copyWithCount(cursor.getCount() - moved);
                steps.add(click(slotIndex, bundle, cursor, target, bundle, cursorAfter, targetAfter));
                target = targetAfter;
                cursor = cursorAfter;
            }
        }
        for (int slotIndex : playerSlotIndices(menu)) {
            if (slotIndex == bundleSlotIndex || cursor.isEmpty()) {
                continue;
            }

            Slot slot = menu.getSlot(slotIndex);
            ItemStack target = slot.getItem().copy();
            if (target.isEmpty() && slot.mayPlace(cursor)) {
                int moved = Math.min(cursor.getCount(), Math.min(cursor.getMaxStackSize(), slot.getMaxStackSize(cursor)));
                ItemStack targetAfter = cursor.copyWithCount(moved);
                ItemStack cursorAfter = cursor.copyWithCount(cursor.getCount() - moved);
                steps.add(click(slotIndex, bundle, cursor, target, bundle, cursorAfter, targetAfter));
                target = targetAfter;
                cursor = cursorAfter;
            }
        }

        if (!cursor.isEmpty()) {
            BundleContents.Mutable returnMutable = new BundleContents.Mutable(bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
            ItemStack cursorAfter = cursor.copy();
            returnMutable.tryInsert(cursorAfter);
            ItemStack bundleAfterReturn = withContents(bundle, returnMutable.toImmutable());
            steps.add(new BundleClickStep(
                bundleSlotIndex,
                0,
                ContainerInput.PICKUP,
                bundle,
                cursor,
                bundle,
                bundleAfterReturn,
                cursorAfter,
                bundleAfterReturn
            ));
            bundle = bundleAfterReturn;
            cursor = cursorAfter;
        }

        return new Plan(List.copyOf(steps), true);
    }

    static Plan insert(AbstractContainerMenu menu, int bundleSlotIndex, int sourceSlotIndex) {
        if (!menu.getCarried().isEmpty()
            || bundleSlotIndex < 0
            || bundleSlotIndex >= menu.slots.size()
            || sourceSlotIndex < 0
            || sourceSlotIndex >= menu.slots.size()
            || bundleSlotIndex == sourceSlotIndex) {
            return null;
        }

        ItemStack bundle = menu.getSlot(bundleSlotIndex).getItem().copy();
        ItemStack source = menu.getSlot(sourceSlotIndex).getItem().copy();
        if (bundle.isEmpty() || source.isEmpty()) {
            return null;
        }

        List<BundleStep> steps = new ArrayList<>();
        steps.add(new BundleClickStep(
            sourceSlotIndex,
            0,
            ContainerInput.PICKUP,
            bundle,
            ItemStack.EMPTY,
            source,
            bundle,
            source,
            ItemStack.EMPTY
        ));
        ItemStack cursor = source.copy();
        source = ItemStack.EMPTY;

        BundleContents.Mutable mutable = new BundleContents.Mutable(bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
        ItemStack bundleBeforeInsert = bundle.copy();
        mutable.tryInsert(cursor);
        bundle = withContents(bundle, mutable.toImmutable());
        steps.add(new BundleClickStep(
            bundleSlotIndex,
            0,
            ContainerInput.PICKUP,
            bundleBeforeInsert,
            source,
            bundleBeforeInsert,
            bundle,
            cursor,
            bundle
        ));

        if (!cursor.isEmpty()) {
            ItemStack sourceAfter = cursor.copy();
            steps.add(new BundleClickStep(
                sourceSlotIndex,
                0,
                ContainerInput.PICKUP,
                bundle,
                cursor,
                source,
                bundle,
                ItemStack.EMPTY,
                sourceAfter
            ));
        }

        return new Plan(List.copyOf(steps), true);
    }

    private static BundleClickStep click(
        int slotIndex,
        ItemStack bundle,
        ItemStack cursor,
        ItemStack slot,
        ItemStack bundleAfter,
        ItemStack cursorAfter,
        ItemStack slotAfter
    ) {
        return new BundleClickStep(slotIndex, 0, ContainerInput.PICKUP, bundle, cursor, slot, bundleAfter, cursorAfter, slotAfter);
    }

    private static List<Integer> playerSlotIndices(AbstractContainerMenu menu) {
        List<Integer> result = new ArrayList<>(36);
        for (int index = InventoryMenu.INV_SLOT_START; index < InventoryMenu.USE_ROW_SLOT_END; index++) {
            if (index < menu.slots.size()) {
                result.add(index);
            }
        }
        return result;
    }

    private static ItemStack withContents(ItemStack stack, BundleContents contents) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.BUNDLE_CONTENTS, contents);
        return copy;
    }

    static boolean same(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
    }

    record Plan(List<BundleStep> steps, boolean cursorMustBeEmpty) {
    }

    sealed interface BundleStep permits BundleSelectionStep, BundleClickStep {
    }

    record BundleSelectionStep(int bundleSlotIndex, int selectedIndex, ItemStack expectedBefore, ItemStack expectedAfter)
        implements BundleStep {
        BundleSelectionStep {
            expectedBefore = expectedBefore.copy();
            expectedAfter = expectedAfter.copy();
        }
    }

    record BundleClickStep(
        int slotIndex,
        int button,
        ContainerInput input,
        ItemStack expectedBundle,
        ItemStack expectedCursor,
        ItemStack expectedSlot,
        ItemStack expectedBundleAfter,
        ItemStack expectedCursorAfter,
        ItemStack expectedSlotAfter
    ) implements BundleStep {
        BundleClickStep {
            expectedBundle = expectedBundle.copy();
            expectedCursor = expectedCursor.copy();
            expectedSlot = expectedSlot.copy();
            expectedBundleAfter = expectedBundleAfter.copy();
            expectedCursorAfter = expectedCursorAfter.copy();
            expectedSlotAfter = expectedSlotAfter.copy();
        }
    }
}
