package io.github.jsevenheck.utilsmod.feature.inventorysort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure planning logic that turns a snapshot of sortable slots into the desired, fully sorted and
 * consolidated arrangement of the same slots. Contains no Minecraft imports and never mutates its
 * input; it only computes a target snapshot for {@link InventoryClickPlanner} to realize via clicks.
 * <p>
 * Ordering: item registry namespace, then path, then distinguishing components/data, then custom
 * name, per {@link ItemIdentity#compare(ItemIdentity, ItemIdentity)}. Within a single item identity,
 * stacks are consolidated up to the identity's effective max stack size (the smallest max stack size
 * observed among the slots currently holding that identity), largest chunks first. Occupied slots are
 * placed at the lowest available logical slot indices; empty slots are pushed to the end.
 */
public final class InventorySortPlanner {

    private InventorySortPlanner() {
    }

    public static List<SortSlot> plan(List<SortSlot> slots) {
        List<Integer> order = new ArrayList<>(slots.size());
        Map<Integer, Integer> maxBySlotIndex = new HashMap<>();
        for (SortSlot slot : slots) {
            order.add(slot.slotIndex());
            maxBySlotIndex.put(slot.slotIndex(), slot.maxStackSize());
        }
        order.sort(Comparator.naturalOrder());

        Map<ItemIdentity, Long> totals = new LinkedHashMap<>();
        Map<ItemIdentity, Integer> effectiveMaxStack = new HashMap<>();
        for (SortSlot slot : slots) {
            if (slot.isEmpty()) {
                continue;
            }
            totals.merge(slot.identity(), (long) slot.count(), Long::sum);
            effectiveMaxStack.merge(slot.identity(), slot.maxStackSize(), Math::min);
        }

        List<ItemIdentity> identities = new ArrayList<>(totals.keySet());
        identities.sort(ItemIdentity::compare);

        Deque<Integer> availableSlots = new ArrayDeque<>(order);
        List<SortSlot> target = new ArrayList<>(slots.size());

        for (ItemIdentity identity : identities) {
            long remaining = totals.get(identity);
            int cap = Math.max(1, effectiveMaxStack.get(identity));
            while (remaining > 0) {
                if (availableSlots.isEmpty()) {
                    // Defensive only: cannot happen since the same slot set already held every item
                    // before sorting, so consolidation can only need the same number of slots or fewer.
                    break;
                }
                int slotIndex = availableSlots.poll();
                int chunk = (int) Math.min(cap, remaining);
                target.add(new SortSlot(slotIndex, identity, chunk, maxBySlotIndex.get(slotIndex)));
                remaining -= chunk;
            }
        }

        Iterator<Integer> leftoverSlots = availableSlots.iterator();
        while (leftoverSlots.hasNext()) {
            int slotIndex = leftoverSlots.next();
            target.add(SortSlot.empty(slotIndex, maxBySlotIndex.get(slotIndex)));
        }

        target.sort(Comparator.comparingInt(SortSlot::slotIndex));
        return target;
    }
}
