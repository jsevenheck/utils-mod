package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.SortSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Identifies which slots of the currently open screen are safe to sort, purely from real menu/slot
 * metadata and slot ownership -- never from hardcoded absolute slot indices.
 * <p>
 * Supported:
 * <ul>
 *   <li>The player's own inventory screen ({@link InventoryMenu}) and the player-inventory row shown
 *   at the bottom of any container screen: main inventory only (the hotbar is intentionally excluded), identified by the slot's backing
 *   container being the player's own {@link Inventory} and its container-local index being below
 *   {@link Inventory#INVENTORY_SIZE} (which already excludes armor/offhand, since those are exposed
 *   through equipment-backed indices at or above that bound) and the slot being a plain {@link Slot}
 *   (which already excludes the anonymous offhand slot subclass, defensively, even without the index
 *   check).</li>
 *   <li>Standard chest-like storage: {@link ChestMenu}, {@link HopperMenu}, {@link DispenserMenu} and
 *   {@link ShulkerBoxMenu}, each verified (via decompiled source) to expose their storage grid only
 *   through plain {@link Slot} (or, for shulker boxes, {@link ShulkerBoxSlot}, which only adds an
 *   item-fits-inside-a-shulker-box restriction) with no crafting/result/equipment slots mixed in.</li>
 * </ul>
 * Every other menu type (crafting result/input, furnace fuel/result, anvil, enchanting, brewing,
 * beacon, merchant, loom, stonecutter, cartography, smithing, grindstone, creative inventory, ...) is
 * simply not in this allow-list, so it is never touched -- an unknown or specialized menu fails safe.
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
        boolean isPlayerInventoryMenu = menu instanceof InventoryMenu;
        boolean isChestLike = menu instanceof ChestMenu
            || menu instanceof HopperMenu
            || menu instanceof DispenserMenu
            || menu instanceof ShulkerBoxMenu;
        if (!isPlayerInventoryMenu && !isChestLike) {
            return Optional.empty();
        }

        Inventory playerInventory = player.getInventory();
        List<SortSlot> playerSlots = new ArrayList<>();
        List<SortSlot> containerSlots = new ArrayList<>();

        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.isFake()) {
                continue;
            }

            if (slot.container == playerInventory
                && slot.getClass() == Slot.class
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
                if (slot.getContainerSlot() < 9) {
                    continue;
                }
                playerSlots.add(toSortSlot(slot));
            } else if (isChestLike
                && slot.container != playerInventory
                && (slot.getClass() == Slot.class || slot.getClass() == ShulkerBoxSlot.class)) {
                containerSlots.add(toSortSlot(slot));
            }
        }

        SortSession session = new SortSession(menu, playerSlots, containerSlots);
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
