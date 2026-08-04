package io.github.jsevenheck.utilsmod.feature.inventorysort;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySortPlannerTest {

    private static final ItemIdentity DIRT = item("dirt");
    private static final ItemIdentity STONE = item("stone");
    private static final ItemIdentity DIAMOND = item("diamond");

    private static ItemIdentity item(String path) {
        return new ItemIdentity("minecraft", path, "", null);
    }

    private static ItemIdentity swordWithDamage(int damage) {
        return new ItemIdentity("minecraft", "diamond_sword", "minecraft:damage=" + damage, null);
    }

    private static SortSlot occupied(int index, ItemIdentity identity, int count) {
        return new SortSlot(index, identity, count, 64);
    }

    private static SortSlot occupied(int index, ItemIdentity identity, int count, int max) {
        return new SortSlot(index, identity, count, max);
    }

    private static SortSlot empty(int index) {
        return SortSlot.empty(index, 64);
    }

    @Test
    void emptyInventoryStaysEmpty() {
        List<SortSlot> input = List.of(empty(0), empty(1), empty(2));
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertTrue(target.stream().allMatch(SortSlot::isEmpty));
        assertEquals(List.of(0, 1, 2), slotIndices(target));
    }

    @Test
    void alreadySortedIsUnchanged() {
        List<SortSlot> input = List.of(
            occupied(0, DIAMOND, 64),
            occupied(1, DIRT, 32),
            empty(2)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(input, target);
    }

    @Test
    void mixedIdentifiersAreOrderedByNamespacePathThenComponentsThenName() {
        ItemIdentity modItem = new ItemIdentity("othermod", "amulet", "", null);
        ItemIdentity namedDirt = new ItemIdentity("minecraft", "dirt", "", "Special");
        List<SortSlot> input = List.of(
            occupied(0, modItem, 1),
            occupied(1, namedDirt, 1),
            occupied(2, DIRT, 1),
            occupied(3, STONE, 1)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        // minecraft:dirt (no name) < minecraft:dirt "Special" < minecraft:stone < othermod:amulet
        assertEquals(DIRT, target.get(0).identity());
        assertEquals(namedDirt, target.get(1).identity());
        assertEquals(STONE, target.get(2).identity());
        assertEquals(modItem, target.get(3).identity());
    }

    @Test
    void multiplePartialStacksConsolidate() {
        List<SortSlot> input = List.of(
            occupied(0, DIRT, 10),
            occupied(1, DIRT, 20),
            occupied(2, DIRT, 30),
            occupied(3, DIRT, 5)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(64, target.get(0).count());
        assertEquals(1, target.get(1).count()); // 65 total -> one full stack of 64 + remainder 1
        long totalAfter = target.stream().mapToLong(SortSlot::count).sum();
        assertEquals(65, totalAfter);
        assertEquals(2, target.stream().filter(s -> !s.isEmpty()).count());
    }

    @Test
    void sameItemDifferentComponentsAreNotMerged() {
        List<SortSlot> input = List.of(
            occupied(0, swordWithDamage(10), 1, 1),
            occupied(1, swordWithDamage(20), 1, 1)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(2, target.stream().filter(s -> !s.isEmpty()).count());
        assertTrue(target.stream().anyMatch(s -> swordWithDamage(10).equals(s.identity())));
        assertTrue(target.stream().anyMatch(s -> swordWithDamage(20).equals(s.identity())));
    }

    @Test
    void nonStackableItemsEachKeepOwnSlot() {
        List<SortSlot> input = List.of(
            occupied(0, swordWithDamage(1), 1, 1),
            occupied(1, swordWithDamage(1), 1, 1),
            occupied(2, swordWithDamage(1), 1, 1)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(3, target.stream().filter(s -> !s.isEmpty()).count());
        assertTrue(target.stream().allMatch(s -> s.isEmpty() || s.count() == 1));
    }

    @Test
    void fullInventoryWithNoSpareSlotsStillPlansSuccessfully() {
        List<SortSlot> input = List.of(
            occupied(0, swordWithDamage(1), 1, 1),
            occupied(1, swordWithDamage(2), 1, 1),
            occupied(2, swordWithDamage(3), 1, 1)
        );
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(3, target.size());
        assertTrue(target.stream().noneMatch(SortSlot::isEmpty));
    }

    @Test
    void excludedSlotsAreNeverPassedToThePlannerSoTheyStayUntouched() {
        // The planner only ever sees the sortable slots it's given; slots that must stay untouched
        // (armor, crafting, result, etc.) are simply never included in the input list by the caller.
        List<SortSlot> input = List.of(occupied(0, STONE, 1), occupied(1, DIRT, 1));
        List<SortSlot> target = InventorySortPlanner.plan(input);
        assertEquals(List.of(0, 1), slotIndices(target));
    }

    @Test
    void outputIsDeterministicAcrossRuns() {
        List<SortSlot> input = List.of(
            occupied(0, STONE, 3),
            occupied(1, DIAMOND, 1),
            occupied(2, DIRT, 7),
            empty(3)
        );
        List<SortSlot> first = InventorySortPlanner.plan(input);
        List<SortSlot> second = InventorySortPlanner.plan(input);
        assertEquals(first, second);
    }

    private static List<Integer> slotIndices(List<SortSlot> slots) {
        return slots.stream().map(SortSlot::slotIndex).collect(Collectors.toList());
    }
}
