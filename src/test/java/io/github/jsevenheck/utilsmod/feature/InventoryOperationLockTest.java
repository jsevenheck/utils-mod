package io.github.jsevenheck.utilsmod.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryOperationLockTest {

    @AfterEach
    void releaseAnyOwner() {
        InventoryOperationLock.release("inventory-sort");
        InventoryOperationLock.release("bundle-ui");
    }

    @Test
    void sortAndBundleOperationsCannotRunConcurrently() {
        assertTrue(InventoryOperationLock.tryAcquire("inventory-sort"));
        assertFalse(InventoryOperationLock.tryAcquire("bundle-ui"));
        assertTrue(InventoryOperationLock.isHeld());

        InventoryOperationLock.release("inventory-sort");
        assertTrue(InventoryOperationLock.tryAcquire("bundle-ui"));
    }

    @Test
    void wrongOwnerCannotReleaseAnotherOperation() {
        assertTrue(InventoryOperationLock.tryAcquire("bundle-ui"));
        InventoryOperationLock.release("inventory-sort");
        assertTrue(InventoryOperationLock.isHeld());
    }
}
