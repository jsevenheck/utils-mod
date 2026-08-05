package io.github.jsevenheck.utilsmod.client.feature.compass;

import io.github.jsevenheck.utilsmod.client.config.ModConfig;
import io.github.jsevenheck.utilsmod.feature.compass.LocalWaypointRules;
import io.github.jsevenheck.utilsmod.feature.compass.WaypointMarker;
import io.github.jsevenheck.utilsmod.feature.compass.WaypointProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owns all local-waypoint mutations and exposes read-only render snapshots. */
final class LocalWaypointService {

    static final int DEFAULT_MARKER_COLOR = 0xFF35C759;

    enum Failure {
        NONE,
        NO_ACTIVE_PROFILE,
        NO_PLAYER,
        INVALID_NAME,
        DUPLICATE_NAME,
        INVALID_COLOR,
        NOT_FOUND
    }

    record Result(Failure failure, WaypointMarker marker, boolean recoloredExistingMarker) {
        Result(Failure failure, WaypointMarker marker) {
            this(failure, marker, false);
        }

        boolean succeeded() {
            return failure == Failure.NONE;
        }
    }

    private final ModConfig config;
    private final WaypointProfileResolver profileResolver = new WaypointProfileResolver();

    LocalWaypointService(ModConfig config) {
        this.config = config;
    }

    Result addAtPlayer(Minecraft minecraft, String name) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return new Result(Failure.NO_PLAYER, null);
        }
        return add(minecraft, name, player.getBlockX(), player.getBlockY(), player.getBlockZ(), dimensionId(player),
            DEFAULT_MARKER_COLOR);
    }

    /**
     * Creates a coloured marker, or changes the colour of an existing same-named marker without
     * changing its saved location. This makes repeated {@code addcolor} commands convenient.
     */
    Result addAtPlayerWithColor(Minecraft minecraft, String name, String rgb) {
        Integer color = LocalWaypointRules.parseOpaqueRgb(rgb);
        if (color == null) {
            return new Result(Failure.INVALID_COLOR, null);
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return new Result(Failure.NO_PLAYER, null);
        }
        String normalizedName = LocalWaypointRules.normalizeName(name);
        if (normalizedName == null) {
            return new Result(Failure.INVALID_NAME, null);
        }
        WaypointProfile profile = activeProfile(minecraft, true);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        WaypointMarker existing = profile.markerNamed(normalizedName);
        if (existing != null) {
            existing.color = color;
            config.save();
            return new Result(Failure.NONE, existing, true);
        }
        return add(minecraft, normalizedName, player.getBlockX(), player.getBlockY(), player.getBlockZ(),
            dimensionId(player), color);
    }

    Result addAt(Minecraft minecraft, String name, int x, int y, int z) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return new Result(Failure.NO_PLAYER, null);
        }
        return add(minecraft, name, x, y, z, dimensionId(player), DEFAULT_MARKER_COLOR);
    }

    Result remove(Minecraft minecraft, String name) {
        WaypointProfile profile = activeProfile(minecraft, false);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        WaypointMarker marker = profile.markerNamed(name);
        if (marker == null) {
            return new Result(Failure.NOT_FOUND, null);
        }
        profile.markers.remove(marker);
        config.save();
        return new Result(Failure.NONE, marker);
    }

    Result setVisibility(Minecraft minecraft, String name, boolean visible) {
        WaypointProfile profile = activeProfile(minecraft, false);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        WaypointMarker marker = profile.markerNamed(name);
        if (marker == null) {
            return new Result(Failure.NOT_FOUND, null);
        }
        marker.visible = visible;
        config.save();
        return new Result(Failure.NONE, marker);
    }

    Result setColor(Minecraft minecraft, String name, String rgb) {
        Integer color = LocalWaypointRules.parseOpaqueRgb(rgb);
        if (color == null) {
            return new Result(Failure.INVALID_COLOR, null);
        }

        WaypointProfile profile = activeProfile(minecraft, false);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        WaypointMarker marker = profile.markerNamed(name);
        if (marker == null) {
            return new Result(Failure.NOT_FOUND, null);
        }
        marker.color = color;
        config.save();
        return new Result(Failure.NONE, marker);
    }

    Result rename(Minecraft minecraft, String oldName, String newName) {
        String normalizedName = LocalWaypointRules.normalizeName(newName);
        if (normalizedName == null) {
            return new Result(Failure.INVALID_NAME, null);
        }

        WaypointProfile profile = activeProfile(minecraft, false);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        WaypointMarker marker = profile.markerNamed(oldName);
        if (marker == null) {
            return new Result(Failure.NOT_FOUND, null);
        }
        WaypointMarker sameName = profile.markerNamed(normalizedName);
        if (sameName != null && sameName != marker) {
            return new Result(Failure.DUPLICATE_NAME, null);
        }
        marker.name = normalizedName;
        config.save();
        return new Result(Failure.NONE, marker);
    }

    List<WaypointMarker> markers(Minecraft minecraft) {
        WaypointProfile profile = activeProfile(minecraft, false);
        if (profile == null || profile.markers == null) {
            return List.of();
        }
        return List.copyOf(profile.markers);
    }

    List<WaypointMarker> visibleMarkers(Minecraft minecraft, LocalPlayer player) {
        if (!config.localWaypointMarkersEnabled) {
            return List.of();
        }
        return LocalWaypointRules.selectVisibleMarkers(activeProfile(minecraft, false), dimensionId(player),
            player.getX(), player.getZ(), config.effectiveMaxVisibleLocalWaypointMarkers());
    }

    void setHudEnabled(boolean enabled) {
        config.compassHudEnabled = enabled;
        config.save();
    }

    void setLocalMarkersEnabled(boolean enabled) {
        config.localWaypointMarkersEnabled = enabled;
        config.save();
    }

    void setVanillaMarkersEnabled(boolean enabled) {
        config.compassVanillaWaypointMarkersEnabled = enabled;
        config.save();
    }

    private Result add(Minecraft minecraft, String name, int x, int y, int z, String dimensionId, int color) {
        String normalizedName = LocalWaypointRules.normalizeName(name);
        if (normalizedName == null) {
            return new Result(Failure.INVALID_NAME, null);
        }

        WaypointProfile profile = activeProfile(minecraft, true);
        if (profile == null) {
            return new Result(Failure.NO_ACTIVE_PROFILE, null);
        }
        if (profile.markerNamed(normalizedName) != null) {
            return new Result(Failure.DUPLICATE_NAME, null);
        }

        WaypointMarker marker = new WaypointMarker(UUID.randomUUID(), normalizedName, dimensionId, x, y, z,
            color, true, System.currentTimeMillis());
        profile.markers.add(marker);
        config.save();
        return new Result(Failure.NONE, marker);
    }

    private WaypointProfile activeProfile(Minecraft minecraft, boolean create) {
        Optional<String> key = profileResolver.resolve(minecraft);
        if (key.isEmpty()) {
            return null;
        }

        for (WaypointProfile profile : config.waypointProfiles) {
            if (key.get().equals(profile.key)) {
                return profile;
            }
        }
        if (!create) {
            return null;
        }

        WaypointProfile profile = new WaypointProfile(key.get());
        config.waypointProfiles.add(profile);
        return profile;
    }

    private static String dimensionId(LocalPlayer player) {
        return player.level().dimension().identifier().toString();
    }
}
