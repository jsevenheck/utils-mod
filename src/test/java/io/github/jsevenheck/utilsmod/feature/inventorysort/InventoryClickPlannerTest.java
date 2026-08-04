package io.github.jsevenheck.utilsmod.feature.inventorysort;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link InventoryClickPlanner} by simulating each planned click against a plain-click
 * (pickup/merge/swap) model of a real container and checking that: (1) every click's expectation
 * matches the simulated state at the moment it fires, (2) the cursor is empty before the first click
 * and after the last, and (3) the simulation's final state matches {@link InventorySortPlanner}'s target.
 */
class InventoryClickPlannerTest {

    private static final ItemIdentity DIRT = item("dirt");
    private static final ItemIdentity STONE = item("stone");
    private static final ItemIdentity DIAMOND = item("diamond");

    private static ItemIdentity item(String path) {
        return new ItemIdentity("minecraft", path, "", null);
    }

    private static SortSlot occupied(int index, ItemIdentity identity, int count) {
        return new SortSlot(index, identity, count, 64);
    }

    private static SortSlot empty(int index) {
        return SortSlot.empty(index, 64);
    }

    @Test
    void alreadySortedProducesNoClicks() {
        List<SortSlot> current = List.of(occupied(0, DIAMOND, 64), occupied(1, DIRT, 1), empty(2));
        List<SortSlot> target = InventorySortPlanner.plan(current);
        List<ClickOperation> ops = InventoryClickPlanner.plan(current, target);
        assertTrue(ops.isEmpty());
    }

    @Test
    void emptyInventoryProducesNoClicks() {
        List<SortSlot> current = List.of(empty(0), empty(1));
        List<ClickOperation> ops = InventoryClickPlanner.plan(current, current);
        assertTrue(ops.isEmpty());
    }

    @Test
    void consolidationAndReorderingReachTargetExactly() {
        List<SortSlot> current = List.of(
            occupied(0, STONE, 1),
            occupied(1, DIRT, 10),
            occupied(2, DIAMOND, 3),
            occupied(3, DIRT, 20),
            empty(4),
            occupied(5, DIRT, 40)
        );
        assertPlanReachesTarget(current);
    }

    @Test
    void reorderingFullInventoryWithNoEmptySlots() {
        List<SortSlot> current = List.of(
            occupied(0, STONE, 1),
            occupied(1, DIAMOND, 1),
            occupied(2, DIRT, 1)
        );
        assertPlanReachesTarget(current);
    }

    @Test
    void swapsBetweenFullAndRemainderChunksOfSameItem() {
        // 64 + 20 = 84 total dirt -> target: one full stack (64) and one remainder (20), but starting
        // arrangement already has those exact chunk sizes in the "wrong" (reverse-sorted) slots, forcing
        // phase 2 to swap a full chunk directly against a differently-sized chunk of the same item.
        List<SortSlot> current = List.of(
            occupied(0, DIRT, 20),
            occupied(1, STONE, 1),
            occupied(2, DIRT, 64)
        );
        assertPlanReachesTarget(current);
    }

    @Test
    void safePickupAllFastPathCollectsManySmallStacks() {
        List<SortSlot> current = List.of(
            occupied(0, DIRT, 1),
            occupied(1, DIRT, 1),
            occupied(2, DIRT, 1),
            occupied(3, DIRT, 1),
            occupied(4, DIRT, 1),
            occupied(5, DIRT, 1),
            occupied(6, DIRT, 1),
            occupied(7, DIRT, 1)
        );
        List<SortSlot> target = InventorySortPlanner.plan(current);
        List<ClickOperation> ops = InventoryClickPlanner.plan(current, target, Set.of(DIRT));

        assertEquals(3, ops.size(), "pick up, collect, and place should replace the waterfall");
        assertEquals(ClickOperation.Kind.PICKUP_ALL, ops.get(1).kind());
        assertPlanReachesTarget(current, Set.of(DIRT));
    }

    @Test
    void pickupAllFallsBackWhenTheGroupDoesNotFitInOneCursorStack() {
        List<SortSlot> current = List.of(
            occupied(0, DIRT, 10),
            occupied(1, DIRT, 10),
            occupied(2, DIRT, 10),
            occupied(3, DIRT, 10),
            occupied(4, DIRT, 10),
            occupied(5, DIRT, 10),
            occupied(6, DIRT, 10),
            occupied(7, DIRT, 10)
        );
        List<SortSlot> target = InventorySortPlanner.plan(current);
        List<ClickOperation> ops = InventoryClickPlanner.plan(current, target, Set.of(DIRT));

        assertTrue(ops.stream().noneMatch(op -> op.kind() == ClickOperation.Kind.PICKUP_ALL));
        assertPlanReachesTarget(current, Set.of(DIRT));
    }

    private static void assertPlanReachesTarget(List<SortSlot> current) {
        assertPlanReachesTarget(current, Set.of());
    }

    private static void assertPlanReachesTarget(List<SortSlot> current, Set<ItemIdentity> pickupAllSafeIdentities) {
        List<SortSlot> target = InventorySortPlanner.plan(current);
        List<ClickOperation> ops = InventoryClickPlanner.plan(current, target, pickupAllSafeIdentities);

        Map<Integer, Integer> maxBySlot = new HashMap<>();
        for (SortSlot slot : current) {
            maxBySlot.put(slot.slotIndex(), slot.maxStackSize());
        }

        Map<Integer, SortSlot> finalState = simulate(current, ops, maxBySlot);
        for (SortSlot expected : target) {
            assertEquals(expected, finalState.get(expected.slotIndex()), "mismatch at slot " + expected.slotIndex());
        }
    }

    private static Map<Integer, SortSlot> simulate(List<SortSlot> current, List<ClickOperation> ops, Map<Integer, Integer> maxBySlot) {
        Map<Integer, ItemIdentity> identity = new HashMap<>();
        Map<Integer, Integer> count = new HashMap<>();
        for (SortSlot slot : current) {
            identity.put(slot.slotIndex(), slot.identity());
            count.put(slot.slotIndex(), slot.count());
        }

        ItemIdentity cursorIdentity = null;
        int cursorCount = 0;

        for (ClickOperation op : ops) {
            assertEquals(op.expectedCursorEmptyBefore(), cursorIdentity == null,
                "cursor-empty expectation mismatch before clicking slot " + op.logicalSlot());
            assertEquals(op.expectedIdentity(), identity.get(op.logicalSlot()),
                "identity expectation mismatch at slot " + op.logicalSlot());
            assertEquals(op.expectedCount(), (int) count.getOrDefault(op.logicalSlot(), 0),
                "count expectation mismatch at slot " + op.logicalSlot());

            int slot = op.logicalSlot();
            ItemIdentity slotIdentity = identity.get(slot);
            int slotCount = count.getOrDefault(slot, 0);

            if (op.kind() == ClickOperation.Kind.PICKUP_ALL) {
                assertTrue(cursorIdentity != null, "PICKUP_ALL requires a non-empty cursor");
                int cursorCapacity = maxBySlot.get(slot);
                for (int pass = 0; pass < 2 && cursorCount < cursorCapacity; pass++) {
                    for (Integer otherSlot : identity.keySet()) {
                        if (!cursorIdentity.equals(identity.get(otherSlot))) {
                            continue;
                        }
                        if (pass == 0 && count.getOrDefault(otherSlot, 0) >= cursorCapacity) {
                            continue;
                        }
                        int otherCount = count.getOrDefault(otherSlot, 0);
                        int transferred = Math.min(otherCount, cursorCapacity - cursorCount);
                        count.put(otherSlot, otherCount - transferred);
                        cursorCount += transferred;
                        if (otherCount - transferred == 0) {
                            identity.put(otherSlot, null);
                        }
                        if (cursorCount >= cursorCapacity) {
                            break;
                        }
                    }
                }
                continue;
            }

            if (cursorIdentity == null) {
                cursorIdentity = slotIdentity;
                cursorCount = slotCount;
                identity.put(slot, null);
                count.put(slot, 0);
            } else if (slotIdentity == null) {
                identity.put(slot, cursorIdentity);
                count.put(slot, cursorCount);
                cursorIdentity = null;
                cursorCount = 0;
            } else if (slotIdentity.equals(cursorIdentity)) {
                int cap = maxBySlot.get(slot);
                int room = cap - slotCount;
                int transferred = Math.min(room, cursorCount);
                count.put(slot, slotCount + transferred);
                cursorCount -= transferred;
                if (cursorCount == 0) {
                    cursorIdentity = null;
                }
            } else {
                identity.put(slot, cursorIdentity);
                count.put(slot, cursorCount);
                cursorIdentity = slotIdentity;
                cursorCount = slotCount;
            }
        }

        assertNull(cursorIdentity, "cursor must end empty");

        Map<Integer, SortSlot> result = new HashMap<>();
        for (Integer slotIndex : identity.keySet()) {
            ItemIdentity slotItemIdentity = identity.get(slotIndex);
            int slotCount = count.get(slotIndex);
            result.put(slotIndex, slotItemIdentity == null
                ? SortSlot.empty(slotIndex, maxBySlot.get(slotIndex))
                : new SortSlot(slotIndex, slotItemIdentity, slotCount, maxBySlot.get(slotIndex)));
        }
        return result;
    }
}
