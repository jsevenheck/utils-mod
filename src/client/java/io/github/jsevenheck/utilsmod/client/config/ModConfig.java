package io.github.jsevenheck.utilsmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jsevenheck.utilsmod.UtilsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small, dependency-light client-side settings for the mod's features, persisted as a hand-rolled
 * JSON file via Gson (already bundled with the game) under the standard Fabric config directory.
 * Deliberately avoids Cloth Config/Mod Menu since neither is a dependency of this project.
 */
public final class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("compass-hud.json");
    private static final int MIN_CLICK_DELAY_TICKS = 1;

    private static ModConfig instance;

    /** Master switch for the inventory sort feature; the keybinding does nothing while this is off. */
    public boolean inventorySortEnabled = true;

    /**
     * When {@code true} (default, safer), the container section and the player inventory section of
     * an open container screen are sorted as two independent pools: items never move between the
     * container and the player's own inventory. When {@code false}, both sections are treated as one
     * combined pool that may redistribute items across both.
     */
    public boolean sortSectionsIndependently = true;

    /** Client ticks between each queued click while a sort is running. */
    public int clickDelayTicks = 1;

    public static ModConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int effectiveClickDelayTicks() {
        return Math.max(MIN_CLICK_DELAY_TICKS, clickDelayTicks);
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            UtilsMod.LOGGER.warn("Failed to save config to {}", PATH, e);
        }
    }

    private static ModConfig load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
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
