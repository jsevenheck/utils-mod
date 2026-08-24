package io.github.jsevenheck.utilsmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jsevenheck.utilsmod.UtilsMod;
import io.github.jsevenheck.utilsmod.feature.compass.WaypointProfile;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Small, dependency-light client-side settings for the mod's features, persisted as a hand-rolled
 * JSON file via Gson (already bundled with the game) under the standard Fabric config directory.
 * Deliberately avoids Cloth Config/Mod Menu since neither is a dependency of this project.
 */
public final class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("compass-hud.json");
    private static final int MIN_CLICK_DELAY_TICKS = 1;
    private static final int MIN_VISIBLE_LOCAL_WAYPOINTS = 1;
    private static final int MAX_VISIBLE_LOCAL_WAYPOINTS = 32;

    private static ModConfig instance;

    /** Master switch for the inventory sort feature; the keybinding does nothing while this is off. */
    public boolean inventorySortEnabled = true;

    /** Client ticks between each queued click while a sort is running. */
    public int clickDelayTicks = 1;

    /** Master switch for the improved client-side Bundle screen. */
    public boolean bundleUiEnabled = true;

    /** Opens the Bundle screen for the default Shift + Right Click shortcut. */
    public boolean bundleUiShiftRightClick = true;

    /** Master switch for the entire compass HUD strip. */
    public boolean compassHudEnabled = true;

    /** Shows server-provided/vanilla locator dots in the compass HUD. */
    public boolean compassVanillaWaypointMarkersEnabled = true;

    /** Shows locally stored waypoint dots in the compass HUD without deleting them when disabled. */
    public boolean localWaypointMarkersEnabled = true;

    /** Maximum number of nearest local waypoint dots considered by the compass HUD. */
    public int maxVisibleLocalWaypointMarkers = 8;

    /** Schema version for {@link #waypointProfiles}. */
    public int waypointDataVersion = 1;

    /** Local waypoint profiles, isolated by multiplayer server or singleplayer save identity. */
    public List<WaypointProfile> waypointProfiles = new ArrayList<>();

    public static ModConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int effectiveClickDelayTicks() {
        return Math.max(MIN_CLICK_DELAY_TICKS, clickDelayTicks);
    }

    public int effectiveMaxVisibleLocalWaypointMarkers() {
        return Math.clamp(maxVisibleLocalWaypointMarkers, MIN_VISIBLE_LOCAL_WAYPOINTS, MAX_VISIBLE_LOCAL_WAYPOINTS);
    }

    public void save() {
        Path temporaryPath = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temporaryPath, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            UtilsMod.LOGGER.warn("Failed to save config to {}", PATH, e);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException ignored) {
                // The next save replaces this stale temporary file.
            }
        }
    }

    private void normalize() {
        if (waypointDataVersion < 1) {
            waypointDataVersion = 1;
        }
        if (waypointProfiles == null) {
            waypointProfiles = new ArrayList<>();
            return;
        }

        Set<String> profileKeys = new HashSet<>();
        for (Iterator<WaypointProfile> iterator = waypointProfiles.iterator(); iterator.hasNext();) {
            WaypointProfile profile = iterator.next();
            if (profile == null || profile.key == null || profile.key.isBlank() || !profileKeys.add(profile.key)) {
                iterator.remove();
                continue;
            }
            profile.normalize();
        }
    }

    private static ModConfig load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    loaded.normalize();
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                UtilsMod.LOGGER.warn("Failed to load config from {}, using defaults", PATH, e);
            }
        }
        ModConfig defaults = new ModConfig();
        defaults.save();
        return defaults;
    }
}
