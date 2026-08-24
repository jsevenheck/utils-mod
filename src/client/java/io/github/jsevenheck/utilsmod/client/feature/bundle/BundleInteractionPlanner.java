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

    static Plan extract(AbstractContainerMenu menu, int bundleSlotIndex, int selectedIndex, boolean quickMove, boolean takeSingleItem) {
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
        steps.add(new BundleSelectionStep(bundleSlotIndex, selectedIndex, initialBundle, selectedBundle, ItemStack.EMPTY));
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

        if (takeSingleItem && removed.getCount() > 1) {
            return extractSingleItem(menu, bundleSlotIndex, steps, removed, bundleAfterRemoval, removedMutable);
        }

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

    /**
     * Splits exactly one item off the whole merged stack the bundle just handed to the cursor,
     * re-inserting the remainder and leaving that one item on the cursor. There is no vanilla
     * protocol operation for a partial bundle extraction — {@code BundleContents.Mutable#removeOne()}
     * always hands over the whole merged entry, whatever its count, both for the real in-hand Bundle
     * item and for this screen's replay of that same click. This borrows one empty real
     * inventory/hotbar slot as scratch space: place 1 there via a normal right-click-on-empty-slot,
     * insert the rest back into the bundle, then pick the 1 back up onto the cursor. Requires a free
     * slot other than the bundle's own; returns null (caller leaves the UI untouched) if none is
     * available or the remainder unexpectedly doesn't fit back in.
     */
    private static Plan extractSingleItem(
        AbstractContainerMenu menu,
        int bundleSlotIndex,
        List<BundleStep> steps,
        ItemStack removed,
        ItemStack bundleAfterRemoval,
        BundleContents.Mutable removedMutable
    ) {
        int scratchSlot = -1;
        for (int slotIndex : playerSlotIndices(menu)) {
            if (slotIndex != bundleSlotIndex && menu.getSlot(slotIndex).getItem().isEmpty()) {
                scratchSlot = slotIndex;
                break;
            }
        }
        if (scratchSlot < 0) {
            return null;
        }

        ItemStack single = removed.copyWithCount(1);
        ItemStack remainder = removed.copyWithCount(removed.getCount() - 1);
        steps.add(new BundleClickStep(
            scratchSlot,
            1,
            ContainerInput.PICKUP,
            bundleAfterRemoval,
            removed,
            ItemStack.EMPTY,
            bundleAfterRemoval,
            remainder,
            single
        ));

        BundleContents.Mutable reinsertMutable = new BundleContents.Mutable(removedMutable.toImmutable());
        ItemStack remainderToInsert = remainder.copy();
        reinsertMutable.tryInsert(remainderToInsert);
        if (!remainderToInsert.isEmpty()) {
            return null;
        }
        ItemStack bundleAfterReinsert = withContents(bundleAfterRemoval, reinsertMutable.toImmutable());
        steps.add(new BundleClickStep(
            bundleSlotIndex,
            0,
            ContainerInput.PICKUP,
            bundleAfterRemoval,
            remainder,
            bundleAfterRemoval,
            bundleAfterReinsert,
            ItemStack.EMPTY,
            bundleAfterReinsert
        ));

        steps.add(new BundleClickStep(
            scratchSlot,
            0,
            ContainerInput.PICKUP,
            bundleAfterReinsert,
            ItemStack.EMPTY,
            single,
            bundleAfterReinsert,
            single,
            ItemStack.EMPTY
        ));

        return new Plan(List.copyOf(steps), false);
    }

    /**
     * Adds one more item from the Bundle onto a cursor that already holds a matching stack — the
     * "Shift + Right Click again to grab another one" gesture. Vanilla's own Bundle extraction
     * ({@code overrideOtherStackedOnMe}'s SECONDARY branch) only fires when the cursor is empty;
     * with something already carried it falls through to a plain slot swap instead, which would
     * move the Bundle itself. To honour repeated clicks anyway, this parks the carried stack in one
     * free slot, performs a normal empty-cursor extraction, tops up the parked stack by exactly one
     * via a right-click combine, reinserts any leftover into the Bundle, then picks the combined
     * stack back up onto the cursor. Needs one free inventory/hotbar slot; returns null (caller
     * leaves the UI untouched) if that space isn't available, the carried item doesn't match the
     * targeted Bundle entry, or the stack is already full.
     */
    static Plan extractAdditional(AbstractContainerMenu menu, int bundleSlotIndex, int selectedIndex) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()
            || carried.getCount() >= carried.getMaxStackSize()
            || bundleSlotIndex < 0
            || bundleSlotIndex >= menu.slots.size()) {
            return null;
        }

        ItemStack initialBundle = menu.getSlot(bundleSlotIndex).getItem().copy();
        BundleContents initialContents = initialBundle.get(DataComponents.BUNDLE_CONTENTS);
        if (initialContents == null || selectedIndex < 0 || selectedIndex >= initialContents.size()) {
            return null;
        }
        if (!ItemStack.isSameItemSameComponents(initialContents.itemCopyStream().toList().get(selectedIndex), carried)) {
            return null;
        }

        int parkSlot = -1;
        for (int slotIndex : playerSlotIndices(menu)) {
            if (slotIndex != bundleSlotIndex && menu.getSlot(slotIndex).getItem().isEmpty()) {
                parkSlot = slotIndex;
                break;
            }
        }
        if (parkSlot < 0) {
            return null;
        }

        List<BundleStep> steps = new ArrayList<>();
        ItemStack carriedCopy = carried.copy();
        steps.add(click(parkSlot, initialBundle, carriedCopy, ItemStack.EMPTY, initialBundle, ItemStack.EMPTY, carriedCopy));

        BundleContents.Mutable selectedMutable = new BundleContents.Mutable(initialContents);
        selectedMutable.toggleSelectedItem(selectedIndex);
        ItemStack selectedBundle = withContents(initialBundle, selectedMutable.toImmutable());
        steps.add(new BundleSelectionStep(bundleSlotIndex, selectedIndex, initialBundle, selectedBundle, ItemStack.EMPTY));

        BundleContents.Mutable removedMutable = new BundleContents.Mutable(selectedMutable.toImmutable());
        ItemStack removed = removedMutable.removeOne();
        if (removed == null || removed.isEmpty() || !ItemStack.isSameItemSameComponents(removed, carried)) {
            return null;
        }
        ItemStack bundleAfterRemoval = withContents(initialBundle, removedMutable.toImmutable());
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

        // Right-click on the parked stack moves exactly 1 from cursor into it (standard vanilla
        // combine-by-one), regardless of how many the bundle just handed over.
        ItemStack parkedAfterCombine = carriedCopy.copyWithCount(carriedCopy.getCount() + 1);
        ItemStack remainder = removed.copyWithCount(removed.getCount() - 1);
        steps.add(new BundleClickStep(
            parkSlot,
            1,
            ContainerInput.PICKUP,
            bundleAfterRemoval,
            removed,
            carriedCopy,
            bundleAfterRemoval,
            remainder,
            parkedAfterCombine
        ));

        ItemStack bundleAfterReinsert = bundleAfterRemoval;
        if (!remainder.isEmpty()) {
            BundleContents.Mutable reinsertMutable = new BundleContents.Mutable(removedMutable.toImmutable());
            ItemStack remainderToInsert = remainder.copy();
            reinsertMutable.tryInsert(remainderToInsert);
            if (!remainderToInsert.isEmpty()) {
                return null;
            }
            bundleAfterReinsert = withContents(bundleAfterRemoval, reinsertMutable.toImmutable());
            steps.add(new BundleClickStep(
                bundleSlotIndex,
                0,
                ContainerInput.PICKUP,
                bundleAfterRemoval,
                remainder,
                bundleAfterRemoval,
                bundleAfterReinsert,
                ItemStack.EMPTY,
                bundleAfterReinsert
            ));
        }

        steps.add(new BundleClickStep(
            parkSlot,
            0,
            ContainerInput.PICKUP,
            bundleAfterReinsert,
            ItemStack.EMPTY,
            parkedAfterCombine,
            bundleAfterReinsert,
            parkedAfterCombine,
            ItemStack.EMPTY
        ));

        return new Plan(List.copyOf(steps), false);
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

    record BundleSelectionStep(int bundleSlotIndex, int selectedIndex, ItemStack expectedBefore, ItemStack expectedAfter, ItemStack expectedCursor)
        implements BundleStep {
        BundleSelectionStep {
            expectedBefore = expectedBefore.copy();
            expectedAfter = expectedAfter.copy();
            expectedCursor = expectedCursor.copy();
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
