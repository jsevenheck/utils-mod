package io.github.jsevenheck.utilsmod.client.feature.bundle;

import io.github.jsevenheck.utilsmod.client.config.ModConfig;
import io.github.jsevenheck.utilsmod.client.feature.ModFeature;
import io.github.jsevenheck.utilsmod.client.mixin.AbstractContainerScreenAccessor;
import io.github.jsevenheck.utilsmod.feature.InventoryOperationLock;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/** Registers the Shift + Right Click entry point for the improved Bundle screen. */
public final class BundleFeature implements ModFeature {

    @Override
    public void initializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, event) -> {
                if (!event.hasShiftDown() || event.button() != 1) {
                    return true;
                }
                return !tryOpen(client, currentScreen, event);
            });
        });
    }

    private static boolean tryOpen(Minecraft minecraft, Screen screen, MouseButtonEvent event) {
        ModConfig config = ModConfig.get();
        if (!config.bundleUiEnabled || !config.bundleUiShiftRightClick || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (!(containerScreen.getMenu() instanceof InventoryMenu menu)
            || minecraft.player == null
            || minecraft.player.isSpectator()
            || minecraft.level == null) {
            return false;
        }
        if (!menu.getCarried().isEmpty() || minecraft.player.containerMenu != menu) {
            return false;
        }
        if (!InventoryOperationLock.tryAcquire(BundleScreen.lockOwner())) {
            return false;
        }

        Inventory inventory = minecraft.player.getInventory();
        int left = ((AbstractContainerScreenAccessor) containerScreen).utilsMod$getLeftPos();
        int top = ((AbstractContainerScreenAccessor) containerScreen).utilsMod$getTopPos();
        Slot bundleSlot = null;
        for (Slot slot : menu.slots) {
            if (slot.container == inventory
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE
                && slot.getItem().is(ItemTags.BUNDLES)
                && event.x() >= left + slot.x
                && event.x() < left + slot.x + 16
                && event.y() >= top + slot.y
                && event.y() < top + slot.y + 16) {
                bundleSlot = slot;
                break;
            }
        }

        if (bundleSlot == null) {
            InventoryOperationLock.release(BundleScreen.lockOwner());
            return false;
        }

        minecraft.gui.setScreen(new BundleScreen(menu, inventory, bundleSlot.index));
        return true;
    }
}
