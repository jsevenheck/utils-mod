package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.SortSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/**
 * A resolved snapshot of the currently open, supported menu: the real menu it came from (for click
 * execution) plus the sortable slots split into their two independent sections. Either section may be
 * empty (a plain player inventory screen has no container section).
 */
record SortSession(AbstractContainerMenu menu, List<SortSlot> playerSlots, List<SortSlot> containerSlots) {

    boolean isEmpty() {
        return playerSlots.isEmpty() && containerSlots.isEmpty();
    }
}
