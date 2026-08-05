package io.github.jsevenheck.utilsmod.feature.compass;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WaypointProfileTest {

    @Test
    void normalizationRemovesDuplicateNamesAndMalformedMarkers() {
        WaypointProfile profile = new WaypointProfile("profile");
        profile.markers.add(marker("Home", "00000000-0000-0000-0000-000000000001"));
        profile.markers.add(marker("home", "00000000-0000-0000-0000-000000000002"));
        WaypointMarker malformed = marker("Broken", "00000000-0000-0000-0000-000000000003");
        malformed.id = "not-a-uuid";
        profile.markers.add(malformed);

        profile.normalize();

        assertEquals(1, profile.markers.size());
        assertNotNull(profile.markerNamed("HOME"));
    }

    private static WaypointMarker marker(String name, String id) {
        return new WaypointMarker(UUID.fromString(id), name, "minecraft:overworld", 0, 64, 0,
            0xFFFFFFFF, true, 1L);
    }
}
