package io.github.jsevenheck.utilsmod.feature.compass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A persisted collection of local waypoints belonging to exactly one server or singleplayer world. */
public final class WaypointProfile {

    public String key;
    public List<WaypointMarker> markers = new ArrayList<>();

    /** Required by Gson. */
    public WaypointProfile() {
    }

    public WaypointProfile(String key) {
        this.key = key;
    }

    /** Removes malformed, duplicate, or otherwise unusable persisted marker entries. */
    public void normalize() {
        if (markers == null) {
            markers = new ArrayList<>();
            return;
        }

        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Iterator<WaypointMarker> iterator = markers.iterator(); iterator.hasNext();) {
            WaypointMarker marker = iterator.next();
            if (marker == null || !marker.normalize()) {
                iterator.remove();
                continue;
            }

            String normalizedName = marker.name.toLowerCase(Locale.ROOT);
            if (!ids.add(marker.id) || !names.add(normalizedName)) {
                iterator.remove();
            }
        }
    }

    public WaypointMarker markerNamed(String name) {
        String normalizedName = LocalWaypointRules.normalizeName(name);
        if (normalizedName == null || markers == null) {
            return null;
        }

        String normalizedKey = normalizedName.toLowerCase(Locale.ROOT);
        for (WaypointMarker marker : markers) {
            if (marker != null && normalizedKey.equals(marker.name.toLowerCase(Locale.ROOT))) {
                return marker;
            }
        }
        return null;
    }
}
