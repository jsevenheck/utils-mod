package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.SortSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Identifies which slots of the currently open screen are safe to sort, purely from real menu/slot
 * metadata and slot ownership -- never from hardcoded absolute slot indices.
 * <p>
 * Supported: chests and double chests ({@link ChestMenu} only), verified (via decompiled source) to
 * expose their storage grid only through plain {@link Slot} with no crafting/result/equipment slots
 * mixed in. Only the chest's own slots are sorted -- the player's main inventory and hotbar shown at
 * the bottom of the chest screen are intentionally left untouched, and the player's own inventory
 * screen (with no chest open) has nothing to sort at all.
 * <p>
 * Every other menu type (the player inventory screen, hoppers, dispensers/droppers, shulker boxes,
 * crafting result/input, furnace fuel/result, anvil, enchanting, brewing, beacon, merchant, loom,
 * stonecutter, cartography, smithing, grindstone, creative inventory, ...) is simply not in this
 * allow-list, so it is never touched -- an unknown or specialized menu fails safe.
 */
final class SortableSlotResolver {

    private SortableSlotResolver() {
    }

    static Optional<SortSession> resolve(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) {
            return Optional.empty();
        }
        if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return Optional.empty();
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!(menu instanceof ChestMenu)) {
            return Optional.empty();
        }

        Inventory playerInventory = player.getInventory();
        List<SortSlot> slots = new ArrayList<>();

        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.isFake()) {
                continue;
            }
            if (slot.container != playerInventory && slot.getClass() == Slot.class) {
                slots.add(toSortSlot(slot));
            }
        }

        SortSession session = new SortSession(menu, slots);
        return session.isEmpty() ? Optional.empty() : Optional.of(session);
    }

    private static SortSlot toSortSlot(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return SortSlot.empty(slot.index, slot.getMaxStackSize());
        }
        return new SortSlot(slot.index, ItemIdentities.of(stack), stack.getCount(), slot.getMaxStackSize(stack));
    }
}
