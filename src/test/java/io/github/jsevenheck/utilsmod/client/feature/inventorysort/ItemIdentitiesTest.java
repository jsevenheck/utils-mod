package io.github.jsevenheck.utilsmod.client.feature.inventorysort;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ItemIdentitiesTest {

    private static final DataComponentType<Integer> FIRST_COMPONENT = DataComponentType.<Integer>builder()
        .persistent(Codec.INT)
        .build();
    private static final DataComponentType<Integer> SECOND_COMPONENT = DataComponentType.<Integer>builder()
        .persistent(Codec.INT)
        .build();

    @Test
    void emptyPatchHasEmptyComponentKey() {
        assertEquals("", componentKey(DataComponentPatch.EMPTY));
    }

    @Test
    void componentKeyIsIndependentOfPatchInsertionOrder() {
        DataComponentPatch firstThenSecond = DataComponentPatch.builder()
            .set(FIRST_COMPONENT, 1)
            .set(SECOND_COMPONENT, 2)
            .build();
        DataComponentPatch secondThenFirst = DataComponentPatch.builder()
            .set(SECOND_COMPONENT, 2)
            .set(FIRST_COMPONENT, 1)
            .build();

        assertEquals(componentKey(firstThenSecond), componentKey(secondThenFirst));
    }

    @Test
    void componentKeyDistinguishesDifferentComponentValues() {
        DataComponentPatch firstValue = DataComponentPatch.builder()
            .set(FIRST_COMPONENT, 1)
            .build();
        DataComponentPatch secondValue = DataComponentPatch.builder()
            .set(FIRST_COMPONENT, 7)
            .build();

        assertNotEquals(componentKey(firstValue), componentKey(secondValue));
    }

    private static String componentKey(DataComponentPatch patch) {
        return ItemIdentities.componentKey(patch, type -> {
            if (type == FIRST_COMPONENT) {
                return "test:first";
            }
            if (type == SECOND_COMPONENT) {
                return "test:second";
            }
            throw new AssertionError("Unexpected component type " + type);
        });
    }
}
