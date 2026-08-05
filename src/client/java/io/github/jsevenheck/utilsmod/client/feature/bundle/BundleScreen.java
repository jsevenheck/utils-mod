package io.github.jsevenheck.utilsmod.client.feature.bundle;

import io.github.jsevenheck.utilsmod.feature.InventoryOperationLock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.core.component.DataComponents;

import java.util.List;

/**
 * A client-only presentation layer over the player's real InventoryMenu.  Bundle entries are
 * virtual slots; player-inventory clicks still target the real menu slot indices.
 */
public final class BundleScreen extends Screen {

    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int PAGE_SIZE = 12;
    private static final int COLUMNS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = COLUMNS * SLOT_SIZE + 8;
    private static final String LOCK_OWNER = "bundle-ui";

    private final AbstractContainerMenu menu;
    private final Inventory inventory;
    private final int bundleSlotIndex;

    private int left;
    private int top;
    private int bundleGridTop;
    private int playerGridTop;
    private int hotbarTop;
    private int bundleRows;
    private int page;
    private BundleInteractionExecutor activeExecution;

    public BundleScreen(AbstractContainerMenu menu, Inventory inventory, int bundleSlotIndex) {
        super(Component.translatable("screen.compass-hud.bundle"));
        this.menu = menu;
        this.inventory = inventory;
        this.bundleSlotIndex = bundleSlotIndex;
    }

    @Override
    protected void init() {
        layout();
    }

    @Override
    public void tick() {
        if (!sourceStillValid()) {
            abortAndClose("compass-hud.bundle.changed");
            return;
        }
        if (activeExecution != null && activeExecution.tick(this.minecraft)) {
            boolean aborted = activeExecution.wasAborted();
            activeExecution = null;
            InventoryOperationLock.release(LOCK_OWNER);
            if (aborted) {
                feedback("compass-hud.bundle.aborted");
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        layout();
        drawPanel(graphics);

        BundleContents contents = currentContents();
        List<ItemStack> items = contents == null ? List.of() : contents.itemCopyStream().toList();
        int pageCount = pageCount(items.size());
        page = Math.min(page, Math.max(0, pageCount - 1));
        int start = page * PAGE_SIZE;

        graphics.text(this.font, this.title, left, top + 6, -12566464, false);
        if (items.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("compass-hud.bundle.empty"), left + PANEL_WIDTH / 2, bundleGridTop + 5, 0xff555555);
        } else {
            for (int visible = 0; visible < Math.min(PAGE_SIZE, items.size() - start); visible++) {
                ItemStack stack = items.get(start + visible);
                int x = slotX(visible % COLUMNS);
                int y = slotY(visible / COLUMNS, bundleGridTop);
                drawSlot(graphics, x, y, stack, mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE);
            }
        }

        graphics.text(this.font, Component.translatable("container.inventory"), left, playerGridTop - 12, -12566464, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = InventoryMenu.INV_SLOT_START + row * 9 + column;
                drawRealSlot(graphics, slotIndex, left + column * SLOT_SIZE + 4, playerGridTop + row * SLOT_SIZE, mouseX, mouseY);
            }
        }
        for (int column = 0; column < 9; column++) {
            int slotIndex = InventoryMenu.USE_ROW_SLOT_START + column;
            drawRealSlot(graphics, slotIndex, left + column * SLOT_SIZE + 4, hotbarTop, mouseX, mouseY);
        }

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            graphics.item(carried, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(this.font, carried, mouseX - 8, mouseY - 8);
        }

        int hovered = hoveredBundleEntry(mouseX, mouseY, items.size());
        if (hovered >= 0) {
            graphics.setTooltipForNextFrame(this.font, items.get(page * PAGE_SIZE + hovered), mouseX, mouseY);
        } else {
            int slotIndex = hoveredPlayerSlot(mouseX, mouseY);
            if (slotIndex >= 0 && !menu.getSlot(slotIndex).getItem().isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, menu.getSlot(slotIndex).getItem(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 && event.button() != 1) {
            return false;
        }
        if (activeExecution != null) {
            return true;
        }

        List<ItemStack> items = currentItems();
        int bundleEntry = hoveredBundleEntry(event.x(), event.y(), items.size());
        if (bundleEntry >= 0) {
            if (!menu.getCarried().isEmpty()) {
                feedback("compass-hud.inventorysort.cursor_not_empty");
                return true;
            }
            BundleInteractionPlanner.Plan plan = BundleInteractionPlanner.extract(
                menu,
                bundleSlotIndex,
                page * PAGE_SIZE + bundleEntry,
                event.hasShiftDown()
            );
            if (plan != null) {
                activeExecution = new BundleInteractionExecutor(this, menu, inventory, bundleSlotIndex, plan);
            }
            return true;
        }

        int playerSlot = hoveredPlayerSlot(event.x(), event.y());
        if (playerSlot < 0) {
            return false;
        }
        if (!menu.getCarried().isEmpty()) {
            if (sourceStillValid()) {
                minecraft.gameMode.handleContainerInput(menu.containerId, playerSlot, event.button(), ContainerInput.PICKUP, minecraft.player);
            }
            return true;
        }

        if (!menu.getSlot(playerSlot).getItem().isEmpty()) {
            BundleInteractionPlanner.Plan plan = BundleInteractionPlanner.insert(menu, bundleSlotIndex, playerSlot);
            if (plan != null) {
                activeExecution = new BundleInteractionExecutor(this, menu, inventory, bundleSlotIndex, plan);
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        List<ItemStack> items = currentItems();
        if (items.size() > PAGE_SIZE && x >= left && x < left + PANEL_WIDTH && y >= bundleGridTop && y < bundleGridTop + bundleRows * SLOT_SIZE) {
            int pages = pageCount(items.size());
            page = Math.max(0, Math.min(pages - 1, page + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (activeExecution != null && !menu.getCarried().isEmpty()) {
            feedback("compass-hud.bundle.cursor_busy");
            return;
        }
        closeToInventory();
    }

    @Override
    public void removed() {
        if (activeExecution != null) {
            activeExecution.cancel();
            activeExecution = null;
        }
        InventoryOperationLock.release(LOCK_OWNER);
    }

    private void closeToInventory() {
        if (activeExecution != null) {
            activeExecution.cancel();
            activeExecution = null;
        }
        InventoryOperationLock.release(LOCK_OWNER);
        if (minecraft.player != null && minecraft.level != null) {
            minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
        } else {
            minecraft.gui.setScreen(null);
        }
    }

    private void abortAndClose(String message) {
        feedback(message);
        closeToInventory();
    }

    private boolean sourceStillValid() {
        if (minecraft.player == null || minecraft.level == null || minecraft.player.getInventory() != inventory) {
            return false;
        }
        if (bundleSlotIndex < 0 || bundleSlotIndex >= menu.slots.size()) {
            return false;
        }
        Slot slot = menu.getSlot(bundleSlotIndex);
        return slot.container == inventory && slot.getItem().is(ItemTags.BUNDLES);
    }

    private BundleContents currentContents() {
        if (!sourceStillValid()) {
            return null;
        }
        return menu.getSlot(bundleSlotIndex).getItem().getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    private List<ItemStack> currentItems() {
        BundleContents contents = currentContents();
        return contents == null ? List.of() : contents.itemCopyStream().toList();
    }

    private void layout() {
        int visibleItems = Math.max(1, Math.min(PAGE_SIZE, currentItems().size()));
        bundleRows = Math.max(1, (visibleItems + COLUMNS - 1) / COLUMNS);
        int bundlePanelHeight = 22 + bundleRows * SLOT_SIZE + 5;
        int playerPanelHeight = 18 + 3 * SLOT_SIZE + 4 + SLOT_SIZE + 7;
        int totalHeight = bundlePanelHeight + 12 + playerPanelHeight;
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(4, (height - totalHeight) / 2);
        bundleGridTop = top + 20;
        playerGridTop = top + bundlePanelHeight + 12 + 18;
        hotbarTop = playerGridTop + 3 * SLOT_SIZE + 4;
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        int bottom = hotbarTop + SLOT_SIZE + 5;
        graphics.fill(left - 5, top - 5, left + PANEL_WIDTH + 5, bottom + 5, 0xff202020);
        graphics.fill(left - 3, top - 3, left + PANEL_WIDTH + 3, bottom + 3, 0xffc6c6c6);
        graphics.fill(left, top, left + PANEL_WIDTH, bottom, 0xff8b8b8b);
    }

    private void drawRealSlot(GuiGraphicsExtractor graphics, int slotIndex, int x, int y, int mouseX, int mouseY) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        ItemStack stack = menu.getSlot(slotIndex).getItem();
        drawSlot(graphics, x, y, stack, mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE);
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, boolean hovered) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE);
        if (hovered) {
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x66ffffff);
        }
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 1, y + 1);
            graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private int hoveredBundleEntry(double mouseX, double mouseY, int itemCount) {
        int start = page * PAGE_SIZE;
        int visible = Math.min(PAGE_SIZE, itemCount - start);
        for (int i = 0; i < visible; i++) {
            int x = slotX(i % COLUMNS);
            int y = slotY(i / COLUMNS, bundleGridTop);
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredPlayerSlot(double mouseX, double mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int x = left + column * SLOT_SIZE + 4;
                int y = playerGridTop + row * SLOT_SIZE;
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    return InventoryMenu.INV_SLOT_START + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = left + column * SLOT_SIZE + 4;
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= hotbarTop && mouseY < hotbarTop + SLOT_SIZE) {
                return InventoryMenu.USE_ROW_SLOT_START + column;
            }
        }
        return -1;
    }

    private int slotX(int column) {
        return left + 4 + column * SLOT_SIZE;
    }

    private static int slotY(int row, int gridTop) {
        return gridTop + row * SLOT_SIZE;
    }

    private static int pageCount(int itemCount) {
        return Math.max(1, (itemCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void feedback(String key) {
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(key));
        }
    }

    static String lockOwner() {
        return LOCK_OWNER;
    }
}
