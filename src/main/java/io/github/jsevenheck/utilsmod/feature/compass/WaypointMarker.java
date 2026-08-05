package io.github.jsevenheck.utilsmod.feature.compass;

import java.util.UUID;

/**
 * A user-created, client-local compass marker. Its public fields deliberately make it a simple Gson
 * value object; all mutations and validation are performed by the waypoint service/rules.
 */
public final class WaypointMarker {

    public String id;
    public String name;
    public String dimensionId;
    public int x;
    public int y;
    public int z;
    public int color;
    public boolean visible = true;
    public long createdAtEpochMillis;

    /** Required by Gson. */
    public WaypointMarker() {
    }

    public WaypointMarker(UUID id, String name, String dimensionId, int x, int y, int z, int color,
            boolean visible, long createdAtEpochMillis) {
        this.id = id.toString();
        this.name = name;
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.visible = visible;
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    /** Normalizes persisted values and reports whether this marker is safe to use. */
    public boolean normalize() {
        UUID parsedId;
        try {
            parsedId = UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }

        String normalizedName = LocalWaypointRules.normalizeName(name);
        if (normalizedName == null || dimensionId == null) {
            return false;
        }

        String normalizedDimension = dimensionId.trim();
        if (!normalizedDimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            return false;
        }

        id = parsedId.toString();
        name = normalizedName;
        dimensionId = normalizedDimension;
        color = 0xFF000000 | (color & 0x00FFFFFF);
        return true;
    }
}
