package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import io.github.jsevenheck.utilsmod.feature.inventorysort.ItemIdentity;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Converts real {@link ItemStack}s into the pure, Minecraft-independent {@link ItemIdentity} model. */
final class ItemIdentities {

    private ItemIdentities() {
    }

    static ItemIdentity of(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Component customNameComponent = stack.getComponents().get(DataComponents.CUSTOM_NAME);
        String customName = customNameComponent == null ? null : customNameComponent.getString();
        return new ItemIdentity(id.getNamespace(), id.getPath(), componentKey(stack), customName);
    }

    /**
     * A deterministic, order-independent discriminator for whatever data components differ from the
     * item's defaults (durability, enchantments, custom data, ...). {@link DataComponentPatch} is
     * backed by an insertion-order-preserving map, so its own {@code toString()} isn't safe to use
     * directly here; instead each entry is keyed by its component type's stable registry id and the
     * resulting strings are sorted before joining.
     */
    private static String componentKey(ItemStack stack) {
        return componentKey(stack.getComponentsPatch(), ItemIdentities::componentTypeKey);
    }

    static String componentKey(DataComponentPatch patch,
                               Function<DataComponentType<?>, String> typeKey) {
        if (patch.isEmpty()) {
            return "";
        }
        List<String> parts = patch.entrySet().stream()
            .map(entry -> typeKey.apply(entry.getKey()) + "=" + entry.getValue().map(Object::toString).orElse("<removed>"))
            .sorted()
            .collect(Collectors.toList());
        return String.join(";", parts);
    }

    private static String componentTypeKey(DataComponentType<?> type) {
        Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return id == null ? type.toString() : id.toString();
    }
}
