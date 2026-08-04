package io.github.jsevenheck.utilsmod.feature.inventorysort;

/**
 * A single planned click on one logical slot, expressed purely in terms of the state it expects to
 * find before it fires. The executor is responsible for validating the real slot/cursor against this
 * expectation immediately before performing the click, and aborting the whole queue on any mismatch.
 * <p>
 * Every click planned by {@link InventoryClickPlanner} is a plain "pickup" style interaction
 * (equivalent to a left mouse click): it either picks the slot's content onto an empty cursor, drops
 * the cursor into an empty slot, merges same-item stacks, or swaps two different stacks — exactly the
 * behavior of the vanilla single-click pickup interaction, so it can always be realized with the
 * normal container click API.
 *
 * @param logicalSlot               opaque logical slot identifier, matching {@link SortSlot#slotIndex()}
 * @param expectedIdentity          the item expected to occupy the slot before this click, or {@code null}
 *                                  if the slot is expected to be empty
 * @param expectedCount             the stack size expected before this click; {@code 0} when empty
 * @param expectedCursorEmptyBefore whether the cursor is expected to be empty immediately before this click
 */
public record ClickOperation(int logicalSlot, ItemIdentity expectedIdentity, int expectedCount, boolean expectedCursorEmptyBefore) {

    public boolean expectedSlotEmpty() {
        return expectedIdentity == null;
    }
}
