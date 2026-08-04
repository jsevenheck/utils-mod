package io.github.jsevenheck.utilsmod.feature.inventorysort;

/**
 * A single planned interaction on one logical slot, expressed in terms of the state it expects to
 * find before it fires. The executor validates the real slot/cursor immediately before sending it and
 * aborts the queue on any mismatch.
 *
 * @param logicalSlot               opaque logical slot identifier, matching {@link SortSlot#slotIndex()}
 * @param expectedIdentity          the item expected to occupy the slot before this interaction, or
 *                                  {@code null} if the slot is expected to be empty
 * @param expectedCount             the stack size expected before this interaction; {@code 0} when empty
 * @param expectedCursorEmptyBefore whether the cursor is expected to be empty immediately before this interaction
 * @param kind                       the vanilla interaction to send
 */
public record ClickOperation(int logicalSlot, ItemIdentity expectedIdentity, int expectedCount,
                             boolean expectedCursorEmptyBefore, Kind kind) {

    public ClickOperation(int logicalSlot, ItemIdentity expectedIdentity, int expectedCount,
                          boolean expectedCursorEmptyBefore) {
        this(logicalSlot, expectedIdentity, expectedCount, expectedCursorEmptyBefore, Kind.PICKUP);
    }

    public ClickOperation {
        if (kind == null) {
            throw new IllegalArgumentException("Click kind must not be null");
        }
        if (kind == Kind.PICKUP_ALL
            && (expectedIdentity != null || expectedCount != 0 || expectedCursorEmptyBefore)) {
            throw new IllegalArgumentException("PICKUP_ALL requires an empty slot and a non-empty cursor");
        }
    }

    public enum Kind {
        PICKUP,
        PICKUP_ALL
    }

    public boolean expectedSlotEmpty() {
        return expectedIdentity == null;
    }
}
