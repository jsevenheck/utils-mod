package io.github.jsevenheck.utilsmod.feature.compass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure validation, selection, and compass-bearing rules for local waypoints. */
public final class LocalWaypointRules {

    public record ColorPreset(String name, int argb) {
    }

    public static final int MAX_NAME_CODE_POINTS = 64;
    private static final List<ColorPreset> COLOR_PRESETS = List.of(
        new ColorPreset("red", 0xFFFF5555),
        new ColorPreset("orange", 0xFFFFAA00),
        new ColorPreset("yellow", 0xFFFFFF55),
        new ColorPreset("lime", 0xFFAAFF00),
        new ColorPreset("green", 0xFF55FF55),
        new ColorPreset("cyan", 0xFF55FFFF),
        new ColorPreset("blue", 0xFF5555FF),
        new ColorPreset("purple", 0xFFAA00AA),
        new ColorPreset("pink", 0xFFFF55FF),
        new ColorPreset("white", 0xFFFFFFFF)
    );

    private LocalWaypointRules() {
    }

    /**
     * Trims a marker name and returns {@code null} when it cannot be represented safely in commands
     * or local feedback.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > MAX_NAME_CODE_POINTS) {
            return null;
        }
        return normalized.codePoints().anyMatch(Character::isISOControl) ? null : normalized;
    }

    /** Returns the ten named colours offered by command completion. */
    public static List<ColorPreset> colorPresets() {
        return COLOR_PRESETS;
    }

    /**
     * Parses a named preset or six-digit RGB colour (for example {@code FF8800} or {@code #FF8800})
     * as an opaque ARGB value. The command accepts the unprefixed form so it can be entered without
     * quoting.
     */
    public static Integer parseOpaqueRgb(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        for (ColorPreset preset : COLOR_PRESETS) {
            if (preset.name.equals(hex.toLowerCase(Locale.ROOT))) {
                return preset.argb;
            }
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return 0xFF000000 | Integer.parseInt(hex, 16);
    }

    /** Standard compass bearing: north is 0°, east is 90°, south is 180°, and west is 270°. */
    public static float bearingTo(double playerX, double playerZ, WaypointMarker marker) {
        return wrapDegrees((float) Math.toDegrees(Math.atan2(marker.x - playerX, playerZ - marker.z)));
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        return wrapped < 0.0f ? wrapped + 360.0f : wrapped;
    }

    /**
     * Returns visible markers in the specified dimension, ordered nearest first with a stable UUID
     * tie-breaker, and capped to {@code maximumMarkers}.
     */
    public static List<WaypointMarker> selectVisibleMarkers(WaypointProfile profile, String dimensionId,
            double playerX, double playerZ, int maximumMarkers) {
        if (profile == null || profile.markers == null || dimensionId == null || maximumMarkers <= 0) {
            return List.of();
        }

        List<WaypointMarker> selected = new ArrayList<>();
        for (WaypointMarker marker : profile.markers) {
            if (marker != null && marker.visible && dimensionId.equals(marker.dimensionId)) {
                selected.add(marker);
            }
        }
        selected.sort(Comparator
            .comparingDouble((WaypointMarker marker) -> horizontalDistanceSquared(playerX, playerZ, marker))
            .thenComparing(marker -> marker.id));

        if (selected.size() > maximumMarkers) {
            return List.copyOf(selected.subList(0, maximumMarkers));
        }
        return List.copyOf(selected);
    }

    private static double horizontalDistanceSquared(double playerX, double playerZ, WaypointMarker marker) {
        double x = marker.x - playerX;
        double z = marker.z - playerZ;
        return x * x + z * z;
    }
}
