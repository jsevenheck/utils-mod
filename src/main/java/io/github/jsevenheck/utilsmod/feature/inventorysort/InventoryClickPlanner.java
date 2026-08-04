package io.github.jsevenheck.utilsmod.feature.inventorysort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns a {@code current} slot snapshot and a desired {@code target} snapshot (as produced by
 * {@link InventorySortPlanner}, over the same set of slot indices) into a deterministic sequence of
 * plain single-clicks ({@link ClickOperation}) that realizes the target arrangement.
 * <p>
 * Every operation is a simple "pickup" click, so the whole plan can be executed with the ordinary
 * container click API. Planning happens in two phases entirely on an internal simulated snapshot; the
 * real menu/slots are never touched or mutated by this class.
 * <ol>
 *   <li><b>Consolidation</b> — for each item identity, merges its fragmented stacks (using their
 *   own slots) into as few, as-full-as-possible stacks, via a waterfall of donor→fill merges.</li>
 *   <li><b>Permutation</b> — matches the now-consolidated stacks (and remaining empty slots) 1:1
 *   against the target by identical (identity, count), then realizes that permutation with the
 *   minimum number of pairwise slot swaps (2 clicks when one side is empty, 3 when both are occupied).</li>
 * </ol>
 * If {@code current} already equals {@code target}, no operations are produced.
 */
public final class InventoryClickPlanner {

    private InventoryClickPlanner() {
    }

    public static List<ClickOperation> plan(List<SortSlot> current, List<SortSlot> target) {
        Map<Integer, Cell> state = new HashMap<>();
        Map<Integer, Integer> maxBySlot = new HashMap<>();
        for (SortSlot slot : current) {
            state.put(slot.slotIndex(), new Cell(slot.identity(), slot.count()));
            maxBySlot.put(slot.slotIndex(), slot.maxStackSize());
        }
        if (state.size() != target.size()) {
            throw new IllegalArgumentException("current and target must describe the same set of slots");
        }

        List<ClickOperation> ops = new ArrayList<>();

        consolidate(current, state, maxBySlot, ops);
        permute(state, target, maxBySlot, ops);

        return ops;
    }

    // ---- Phase 1: consolidation -------------------------------------------------------------

    private static void consolidate(List<SortSlot> current, Map<Integer, Cell> state, Map<Integer, Integer> maxBySlot, List<ClickOperation> ops) {
        Map<ItemIdentity, List<Integer>> groups = new TreeMap<>(ItemIdentity::compare);
        for (SortSlot slot : current) {
            if (slot.isEmpty()) {
                continue;
            }
            groups.computeIfAbsent(slot.identity(), key -> new ArrayList<>()).add(slot.slotIndex());
        }
        for (List<Integer> slots : groups.values()) {
            slots.sort(Comparator.naturalOrder());
            consolidateGroup(slots, state, maxBySlot, ops);
        }
    }

    private static void consolidateGroup(List<Integer> slots, Map<Integer, Cell> state, Map<Integer, Integer> maxBySlot, List<ClickOperation> ops) {
        int lo = 0;
        int hi = slots.size() - 1;
        while (lo < hi) {
            int fillSlot = slots.get(lo);
            Cell fill = state.get(fillSlot);
            int fillCap = maxBySlot.get(fillSlot);
            if (fill.count >= fillCap) {
                lo++;
                continue;
            }

            int donorSlot = slots.get(hi);
            Cell donor = state.get(donorSlot);
            if (donor.isEmpty()) {
                hi--;
                continue;
            }

            // Pick up the donor stack (cursor was empty).
            ops.add(new ClickOperation(donorSlot, donor.identity, donor.count, true));
            ItemIdentity cursorIdentity = donor.identity;
            int cursorCount = donor.count;
            donor.identity = null;
            donor.count = 0;

            // Click the fill slot: same item, so it merges up to capacity; any remainder stays on the cursor.
            ops.add(new ClickOperation(fillSlot, fill.identity, fill.count, false));
            int room = fillCap - fill.count;
            int transferred = Math.min(room, cursorCount);
            fill.count += transferred;
            cursorCount -= transferred;

            if (cursorCount > 0) {
                // Remainder can't be merged further right now; place it back into the now-empty donor slot.
                ops.add(new ClickOperation(donorSlot, null, 0, false));
                donor.identity = cursorIdentity;
                donor.count = cursorCount;
            }
        }
    }

    // ---- Phase 2: permutation ----------------------------------------------------------------

    private record ContentKey(ItemIdentity identity, int count) {
        static final ContentKey EMPTY = new ContentKey(null, 0);
    }

    private static ContentKey keyOf(Cell cell) {
        return cell.isEmpty() ? ContentKey.EMPTY : new ContentKey(cell.identity, cell.count);
    }

    private static ContentKey keyOf(SortSlot slot) {
        return slot.isEmpty() ? ContentKey.EMPTY : new ContentKey(slot.identity(), slot.count());
    }

    private static void permute(Map<Integer, Cell> state, List<SortSlot> target, Map<Integer, Integer> maxBySlot, List<ClickOperation> ops) {
        Map<ContentKey, Deque<Integer>> bySourceKey = new HashMap<>();
        List<Integer> domain = new ArrayList<>(state.keySet());
        domain.sort(Comparator.naturalOrder());
        for (int slotIndex : domain) {
            bySourceKey.computeIfAbsent(keyOf(state.get(slotIndex)), key -> new ArrayDeque<>()).add(slotIndex);
        }

        List<SortSlot> targetSorted = new ArrayList<>(target);
        targetSorted.sort(Comparator.comparingInt(SortSlot::slotIndex));

        // dest slot index -> the slot index whose current content belongs there.
        Map<Integer, Integer> wantsSourceOf = new HashMap<>();
        for (SortSlot destSlot : targetSorted) {
            ContentKey key = keyOf(destSlot);
            Deque<Integer> candidates = bySourceKey.get(key);
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("current and target describe different item totals for " + key);
            }
            wantsSourceOf.put(destSlot.slotIndex(), candidates.poll());
        }

        Map<Integer, Integer> currentlyHolds = new HashMap<>(); // slotIndex -> label of content physically there
        Map<Integer, Integer> whereIsLabel = new HashMap<>(); // label -> slotIndex currently holding it
        for (int slotIndex : domain) {
            currentlyHolds.put(slotIndex, slotIndex);
            whereIsLabel.put(slotIndex, slotIndex);
        }

        for (int dest : domain) {
            int wanted = wantsSourceOf.get(dest);
            if (currentlyHolds.get(dest).equals(wanted)) {
                continue;
            }
            int source = whereIsLabel.get(wanted);
            swapPhysical(dest, source, state, maxBySlot, ops);

            int labelAtDest = currentlyHolds.get(dest);
            int labelAtSource = currentlyHolds.get(source);
            currentlyHolds.put(dest, labelAtSource);
            currentlyHolds.put(source, labelAtDest);
            whereIsLabel.put(labelAtSource, dest);
            whereIsLabel.put(labelAtDest, source);
        }
    }

    private static void swapPhysical(int a, int b, Map<Integer, Cell> state, Map<Integer, Integer> maxBySlot, List<ClickOperation> ops) {
        Cell ca = state.get(a);
        Cell cb = state.get(b);
        if (keyOf(ca).equals(keyOf(cb))) {
            return;
        }

        if (ca.isEmpty()) {
            moveWhole(b, a, state, ops);
            return;
        }
        if (cb.isEmpty()) {
            moveWhole(a, b, state, ops);
            return;
        }

        // Both slots are occupied with different content: pick up `a`, click `b`, then place any remainder back into `a`.
        ops.add(new ClickOperation(a, ca.identity, ca.count, true));
        ItemIdentity cursorIdentity = ca.identity;
        int cursorCount = ca.count;
        ca.identity = null;
        ca.count = 0;

        ops.add(new ClickOperation(b, cb.identity, cb.count, false));
        if (cursorIdentity.equals(cb.identity)) {
            int cap = maxBySlot.get(b);
            int room = cap - cb.count;
            int transferred = Math.min(room, cursorCount);
            cb.count += transferred;
            cursorCount -= transferred;
        } else {
            ItemIdentity swappedIdentity = cb.identity;
            int swappedCount = cb.count;
            cb.identity = cursorIdentity;
            cb.count = cursorCount;
            cursorIdentity = swappedIdentity;
            cursorCount = swappedCount;
        }

        if (cursorCount > 0) {
            ops.add(new ClickOperation(a, null, 0, false));
            ca.identity = cursorIdentity;
            ca.count = cursorCount;
        }
    }

    private static void moveWhole(int from, int to, Map<Integer, Cell> state, List<ClickOperation> ops) {
        Cell source = state.get(from);
        ops.add(new ClickOperation(from, source.identity, source.count, true));
        ItemIdentity identity = source.identity;
        int count = source.count;
        source.identity = null;
        source.count = 0;

        Cell destination = state.get(to);
        ops.add(new ClickOperation(to, null, 0, false));
        destination.identity = identity;
        destination.count = count;
    }

    private static final class Cell {
        ItemIdentity identity;
        int count;

        Cell(ItemIdentity identity, int count) {
            this.identity = identity;
            this.count = count;
        }

        boolean isEmpty() {
            return identity == null;
        }
    }
}
