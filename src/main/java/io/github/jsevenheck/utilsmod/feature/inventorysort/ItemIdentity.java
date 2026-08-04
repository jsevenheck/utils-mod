package io.github.jsevenheck.utilsmod.feature.inventorysort;

/**
 * A pure, Minecraft-independent identity for a "kind" of item stack, used to decide which stacks
 * may be merged/consolidated and how stacks are ordered relative to each other.
 * <p>
 * Two stacks are considered the same kind (mergeable) exactly when their {@link ItemIdentity} is
 * {@link #equals(Object) equal}. The client-side layer is responsible for deriving this from a real
 * {@code ItemStack} (namespace/path from the item's registry id, {@code componentKey} from a stable,
 * order-independent serialization of the stack's component patch, and {@code customName} from the
 * stack's custom-name component, if any).
 *
 * @param namespace   registry namespace of the item, e.g. {@code "minecraft"}
 * @param path        registry path of the item, e.g. {@code "diamond_sword"}
 * @param componentKey a deterministic, order-independent string discriminating stacks of the same
 *                      item that differ in data components (enchantments, durability, custom data, ...).
 *                      Empty string when the stack carries no distinguishing components.
 * @param customName  the stack's custom display name, or {@code null} if unset. Used only as a
 *                     tie-breaker after {@code componentKey}.
 */
public record ItemIdentity(String namespace, String path, String componentKey, String customName) {

    public ItemIdentity {
        namespace = namespace == null ? "" : namespace;
        path = path == null ? "" : path;
        componentKey = componentKey == null ? "" : componentKey;
    }

    /**
     * Deterministic ordering: registry namespace, then path, then distinguishing components, then
     * custom name (nulls sort before any non-null name), matching the sort order required for
     * inventory sorting.
     */
    public static int compare(ItemIdentity a, ItemIdentity b) {
        int byNamespace = a.namespace.compareTo(b.namespace);
        if (byNamespace != 0) {
            return byNamespace;
        }
        int byPath = a.path.compareTo(b.path);
        if (byPath != 0) {
            return byPath;
        }
        int byComponents = a.componentKey.compareTo(b.componentKey);
        if (byComponents != 0) {
            return byComponents;
        }
        String nameA = a.customName == null ? "" : a.customName;
        String nameB = b.customName == null ? "" : b.customName;
        return nameA.compareTo(nameB);
    }
}
