package io.github.jsevenheck.utilsmod.client.feature.compass;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.jsevenheck.utilsmod.feature.compass.LocalWaypointRules;
import io.github.jsevenheck.utilsmod.feature.compass.WaypointMarker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Registers local-only commands for managing compass waypoints. */
final class CompassWaypointCommands {

    private CompassWaypointCommands() {
    }

    static void register(LocalWaypointService service) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommands.literal("compasshud");
            var waypoint = ClientCommands.literal("waypoint");

            waypoint.then(ClientCommands.literal("add")
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> addCurrent(service, StringArgumentType.getString(context, "name")))));

            var addColor = ClientCommands.literal("addcolor");
            addColor.then(ClientCommands.argument("rgb", StringArgumentType.word())
                .suggests((context, builder) -> suggestColorPresets(builder))
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> addCurrentWithColor(service, StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "rgb")))));
            waypoint.then(addColor);

            var addAt = ClientCommands.literal("addat");
            var x = ClientCommands.argument("x", IntegerArgumentType.integer());
            var y = ClientCommands.argument("y", IntegerArgumentType.integer());
            var z = ClientCommands.argument("z", IntegerArgumentType.integer());
            z.then(ClientCommands.argument("name", StringArgumentType.greedyString())
                .executes(context -> addAt(service, IntegerArgumentType.getInteger(context, "x"),
                    IntegerArgumentType.getInteger(context, "y"), IntegerArgumentType.getInteger(context, "z"),
                    StringArgumentType.getString(context, "name"))));
            y.then(z);
            x.then(y);
            addAt.then(x);
            waypoint.then(addAt);

            waypoint.then(ClientCommands.literal("list").executes(context -> list(service)));
            waypoint.then(ClientCommands.literal("remove")
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .suggests((context, builder) -> suggestMarkerNames(service, builder))
                    .executes(context -> remove(service, StringArgumentType.getString(context, "name")))));
            waypoint.then(ClientCommands.literal("show")
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .suggests((context, builder) -> suggestMarkerNames(service, builder))
                    .executes(context -> setVisibility(service, StringArgumentType.getString(context, "name"), true))));
            waypoint.then(ClientCommands.literal("hide")
                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .suggests((context, builder) -> suggestMarkerNames(service, builder))
                    .executes(context -> setVisibility(service, StringArgumentType.getString(context, "name"), false))));

            var color = ClientCommands.literal("color");
            var colorName = ClientCommands.argument("name", StringArgumentType.string())
                .suggests((context, builder) -> suggestMarkerNames(service, builder));
            colorName.then(ClientCommands.argument("rgb", StringArgumentType.word())
                .suggests((context, builder) -> suggestColorPresets(builder))
                .executes(context -> setColor(service, StringArgumentType.getString(context, "name"),
                    StringArgumentType.getString(context, "rgb"))));
            color.then(colorName);
            waypoint.then(color);

            var rename = ClientCommands.literal("rename");
            var oldName = ClientCommands.argument("old-name", StringArgumentType.string())
                .suggests((context, builder) -> suggestMarkerNames(service, builder));
            oldName.then(ClientCommands.argument("new-name", StringArgumentType.greedyString())
                .executes(context -> rename(service, StringArgumentType.getString(context, "old-name"),
                    StringArgumentType.getString(context, "new-name"))));
            rename.then(oldName);
            waypoint.then(rename);

            var settings = ClientCommands.literal("setting");
            settings.then(ClientCommands.literal("hud")
                .then(ClientCommands.literal("on").executes(context -> setHud(service, true)))
                .then(ClientCommands.literal("off").executes(context -> setHud(service, false))));
            settings.then(ClientCommands.literal("local")
                .then(ClientCommands.literal("on").executes(context -> setLocalMarkers(service, true)))
                .then(ClientCommands.literal("off").executes(context -> setLocalMarkers(service, false))));
            settings.then(ClientCommands.literal("vanilla")
                .then(ClientCommands.literal("on").executes(context -> setVanillaMarkers(service, true)))
                .then(ClientCommands.literal("off").executes(context -> setVanillaMarkers(service, false))));

            root.then(waypoint);
            root.then(settings);
            dispatcher.register(root);
        });
    }

    private static int addCurrent(LocalWaypointService service, String name) {
        return feedbackMutation("add", service.addAtPlayer(Minecraft.getInstance(), name));
    }

    private static int addAt(LocalWaypointService service, int x, int y, int z, String name) {
        return feedbackMutation("add", service.addAt(Minecraft.getInstance(), name, x, y, z));
    }

    private static int addCurrentWithColor(LocalWaypointService service, String name, String rgb) {
        return feedbackMutation("add", service.addAtPlayerWithColor(Minecraft.getInstance(), name, rgb));
    }

    private static int remove(LocalWaypointService service, String name) {
        return feedbackMutation("remove", service.remove(Minecraft.getInstance(), name));
    }

    private static int setVisibility(LocalWaypointService service, String name, boolean visible) {
        return feedbackMutation(visible ? "show" : "hide",
            service.setVisibility(Minecraft.getInstance(), name, visible));
    }

    private static int setColor(LocalWaypointService service, String name, String rgb) {
        return feedbackMutation("color", service.setColor(Minecraft.getInstance(), name, rgb));
    }

    private static int rename(LocalWaypointService service, String oldName, String newName) {
        return feedbackMutation("rename", service.rename(Minecraft.getInstance(), oldName, newName));
    }

    private static CompletableFuture<Suggestions> suggestMarkerNames(LocalWaypointService service,
            SuggestionsBuilder builder) {
        for (WaypointMarker marker : service.markers(Minecraft.getInstance())) {
            builder.suggest(marker.name);
        }
        return builder.buildFuture();
    }

    /** Offers named colours with a coloured tooltip swatch; arbitrary RGB hex values remain valid. */
    private static CompletableFuture<Suggestions> suggestColorPresets(SuggestionsBuilder builder) {
        String typed = builder.getRemainingLowerCase();
        for (LocalWaypointRules.ColorPreset preset : LocalWaypointRules.colorPresets()) {
            if (preset.name().startsWith(typed)) {
                String hex = colorHex(preset.argb());
                Component preview = Component.literal("■ " + preset.name() + " (#" + hex + ")")
                    .withStyle(style -> style.withColor(preset.argb()));
                builder.suggest(preset.name(), preview);
            }
        }
        return builder.buildFuture();
    }

    private static int list(LocalWaypointService service) {
        List<WaypointMarker> markers = service.markers(Minecraft.getInstance());
        if (markers.isEmpty()) {
            feedback(Component.translatable("compass-hud.waypoint.list.empty"));
            return 1;
        }

        feedback(Component.translatable("compass-hud.waypoint.list.header", markers.size()));
        for (WaypointMarker marker : markers) {
            feedback(Component.translatable("compass-hud.waypoint.list.entry", marker.name, marker.x, marker.y,
                marker.z, marker.dimensionId, marker.visible
                    ? Component.translatable("compass-hud.waypoint.visible")
                    : Component.translatable("compass-hud.waypoint.hidden"), colorHex(marker.color)));
        }
        return 1;
    }

    private static int setHud(LocalWaypointService service, boolean enabled) {
        service.setHudEnabled(enabled);
        feedback(Component.translatable("compass-hud.waypoint.setting.hud", settingState(enabled)));
        return 1;
    }

    private static int setLocalMarkers(LocalWaypointService service, boolean enabled) {
        service.setLocalMarkersEnabled(enabled);
        feedback(Component.translatable("compass-hud.waypoint.setting.local", settingState(enabled)));
        return 1;
    }

    private static int setVanillaMarkers(LocalWaypointService service, boolean enabled) {
        service.setVanillaMarkersEnabled(enabled);
        feedback(Component.translatable("compass-hud.waypoint.setting.vanilla", settingState(enabled)));
        return 1;
    }

    private static Component settingState(boolean enabled) {
        return Component.translatable(enabled ? "compass-hud.waypoint.enabled" : "compass-hud.waypoint.disabled");
    }

    private static int feedbackMutation(String action, LocalWaypointService.Result result) {
        if (result.succeeded()) {
            ClientCommands.refreshCommandCompletions();
        }
        return feedback(action, result);
    }

    private static int feedback(String action, LocalWaypointService.Result result) {
        if (result.succeeded()) {
            WaypointMarker marker = result.marker();
            return switch (action) {
                case "add" -> result.recoloredExistingMarker()
                    ? success("compass-hud.waypoint.color", marker.name, colorHex(marker.color))
                    : success("compass-hud.waypoint.added", marker.name, marker.x, marker.y, marker.z,
                        colorHex(marker.color));
                case "remove" -> success("compass-hud.waypoint.removed", marker.name);
                case "show" -> success("compass-hud.waypoint.shown", marker.name);
                case "hide" -> success("compass-hud.waypoint.hidden_marker", marker.name);
                case "color" -> success("compass-hud.waypoint.color", marker.name, colorHex(marker.color));
                case "rename" -> success("compass-hud.waypoint.renamed", marker.name);
                default -> 1;
            };
        }

        return switch (result.failure()) {
            case NO_PLAYER -> failure("compass-hud.waypoint.error.no_player");
            case NO_ACTIVE_PROFILE -> failure("compass-hud.waypoint.error.no_profile");
            case INVALID_NAME -> failure("compass-hud.waypoint.error.invalid_name");
            case DUPLICATE_NAME -> failure("compass-hud.waypoint.error.duplicate_name");
            case INVALID_COLOR -> failure("compass-hud.waypoint.error.invalid_color");
            case NOT_FOUND -> failure("compass-hud.waypoint.error.not_found");
            case NONE -> 1;
        };
    }

    private static String colorHex(int argb) {
        return String.format(java.util.Locale.ROOT, "%06X", argb & 0x00FFFFFF);
    }

    private static int success(String key, Object... arguments) {
        feedback(Component.translatable(key, arguments));
        return 1;
    }

    private static int failure(String key) {
        feedback(Component.translatable(key));
        return 0;
    }

    private static void feedback(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(message);
        }
    }
}
