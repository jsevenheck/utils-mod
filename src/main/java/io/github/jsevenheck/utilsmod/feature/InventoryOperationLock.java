package io.github.jsevenheck.utilsmod.feature;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared client-operation lock.  It deliberately has no Minecraft dependency so that the
 * coordination rule can stay easy to test and is visible to every client feature.
 */
public final class InventoryOperationLock {

    private static final AtomicReference<String> OWNER = new AtomicReference<>();

    private InventoryOperationLock() {
    }

    public static boolean tryAcquire(String owner) {
        return OWNER.compareAndSet(null, owner);
    }

    public static void release(String owner) {
        OWNER.compareAndSet(owner, null);
    }

    public static boolean isHeld() {
        return OWNER.get() != null;
    }
}
