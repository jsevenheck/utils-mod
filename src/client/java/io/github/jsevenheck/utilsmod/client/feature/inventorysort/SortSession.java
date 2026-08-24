package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.ItemIdentity;
import io.github.jsevenheck.utilsmod.feature.inventorysort.SortSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A resolved snapshot of the currently open, supported menu: the real menu it came from (for click
 * execution) plus the chest's own sortable slots. The player's inventory/hotbar is never included --
 * see {@link SortableSlotResolver}.
 */
record SortSession(AbstractContainerMenu menu, List<SortSlot> slots) {

    boolean isEmpty() {
        return slots.isEmpty();
    }

    /**
     * Vanilla PICKUP_ALL searches every slot in the open menu, so a matching stack outside the
     * sorted slots (e.g. the player's own inventory row) would be collected accidentally. Such
     * identities are deliberately excluded.
     */
    Set<ItemIdentity> pickupAllSafeIdentities() {
        Set<Integer> sectionSlots = new HashSet<>();
        Set<ItemIdentity> safe = new HashSet<>();
        for (SortSlot sortSlot : slots) {
            sectionSlots.add(sortSlot.slotIndex());
            if (!sortSlot.isEmpty()) {
                safe.add(sortSlot.identity());
            }
        }

        for (Slot slot : menu.slots) {
            if (sectionSlots.contains(slot.index)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                safe.remove(ItemIdentities.of(stack));
            }
        }
        return Set.copyOf(safe);
    }
}
