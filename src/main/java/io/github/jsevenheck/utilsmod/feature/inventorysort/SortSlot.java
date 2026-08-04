package io.github.jsevenheck.utilsmod.feature.inventorysort;

/**
 * A pure, immutable snapshot of a single sortable slot's content.
 * <p>
 * {@code slotIndex} is an opaque, caller-defined logical identifier for the slot (the client layer
 * maps this 1:1 onto a real menu slot id); the planner never interprets it beyond using it as a key.
 *
 * @param slotIndex    opaque logical slot identifier, unique within a single plan
 * @param identity     the kind of item occupying the slot, or {@code null} if the slot is empty
 * @param count        current stack size; {@code 0} when {@code identity} is {@code null}
 * @param maxStackSize the maximum stack size usable for this slot (already the minimum of the
 *                      backing container's cap and the item's own max stack size); ignored when empty
 */
public record SortSlot(int slotIndex, ItemIdentity identity, int count, int maxStackSize) {

    public SortSlot {
        if (identity == null && count != 0) {
            throw new IllegalArgumentException("An empty slot must have count 0");
        }
        if (identity != null && count <= 0) {
            throw new IllegalArgumentException("An occupied slot must have a positive count");
        }
    }

    public boolean isEmpty() {
        return identity == null;
    }

    public static SortSlot empty(int slotIndex, int maxStackSize) {
        return new SortSlot(slotIndex, null, 0, maxStackSize);
    }
}
