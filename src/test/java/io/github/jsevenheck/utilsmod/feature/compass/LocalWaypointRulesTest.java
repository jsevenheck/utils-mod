package io.github.jsevenheck.utilsmod.feature.compass;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalWaypointRulesTest {

    private static WaypointMarker marker(String name, String dimension, int x, int z, boolean visible, String id) {
        return new WaypointMarker(UUID.fromString(id), name, dimension, x, 64, z, 0xFF35C759, visible, 1L);
    }

    @Test
    void normalizesAndRejectsInvalidNames() {
        assertEquals("Home", LocalWaypointRules.normalizeName("  Home  "));
        assertNull(LocalWaypointRules.normalizeName("\n"));
        assertNull(LocalWaypointRules.normalizeName("A\u0000B"));
        assertNull(LocalWaypointRules.normalizeName("x".repeat(65)));
    }

    @Test
    void parsesOnlySixDigitRgbColorsAsOpaqueArgb() {
        assertEquals(0xFFFF8800, LocalWaypointRules.parseOpaqueRgb("FF8800"));
        assertEquals(0xFF00AAFF, LocalWaypointRules.parseOpaqueRgb("#00aaff"));
        assertEquals(0xFF35C759, LocalWaypointRules.parseOpaqueRgb("0x35C759"));
        assertEquals(0xFFFF5555, LocalWaypointRules.parseOpaqueRgb("red"));
        assertEquals(0xFF5555FF, LocalWaypointRules.parseOpaqueRgb("BLUE"));
        assertEquals(10, LocalWaypointRules.colorPresets().size());
        assertNull(LocalWaypointRules.parseOpaqueRgb("FF8800CC"));
        assertNull(LocalWaypointRules.parseOpaqueRgb("not-a-color"));
    }

    @Test
    void selectsNearestVisibleMarkersInTheCurrentDimension() {
        WaypointProfile profile = new WaypointProfile("test");
        WaypointMarker near = marker("Near", "overworld", 2, 0, true, "00000000-0000-0000-0000-000000000002");
        WaypointMarker far = marker("Far", "overworld", 12, 0, true, "00000000-0000-0000-0000-000000000001");
        profile.markers.addAll(List.of(far, near,
            marker("Hidden", "overworld", 1, 0, false, "00000000-0000-0000-0000-000000000003"),
            marker("Nether", "nether", 1, 0, true, "00000000-0000-0000-0000-000000000004")));

        assertEquals(List.of(near), LocalWaypointRules.selectVisibleMarkers(profile, "overworld", 0, 0, 1));
        assertEquals(List.of(near, far), LocalWaypointRules.selectVisibleMarkers(profile, "overworld", 0, 0, 8));
    }

    @Test
    void bearingsUseStandardCompassDirections() {
        assertEquals(0.0f, LocalWaypointRules.bearingTo(0, 0,
            marker("North", "overworld", 0, -10, true, "00000000-0000-0000-0000-000000000001")));
        assertEquals(90.0f, LocalWaypointRules.bearingTo(0, 0,
            marker("East", "overworld", 10, 0, true, "00000000-0000-0000-0000-000000000002")));
        assertEquals(180.0f, LocalWaypointRules.bearingTo(0, 0,
            marker("South", "overworld", 0, 10, true, "00000000-0000-0000-0000-000000000003")));
        assertEquals(270.0f, LocalWaypointRules.bearingTo(0, 0,
            marker("West", "overworld", -10, 0, true, "00000000-0000-0000-0000-000000000004")));
    }
}
