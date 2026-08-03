# CLAUDE.md

Single Gradle (Fabric Loom) project: **Compass HUD** — a client-side HUD mod for Minecraft 26.2 that displays a compass at the top of the HUD.

```text
src/main/        <- common mod code: ModInitializer entrypoint, shared logic, common mixins
src/client/      <- client-only code: HUD rendering, ClientModInitializer, client mixins
```

The mod is **client-only** (`"environment": "client"` in `fabric.mod.json`) and runs inside the Minecraft client via Fabric Loader. It has no standalone runtime or toolchain — it only works inside the game.

## Commands (always run from workspace root)

```bash
./gradlew build          # build the mod jar into build/libs/ (also runs any configured tests)
./gradlew runClient      # launch the Minecraft client with the mod loaded
./gradlew runServer      # launch a dedicated server (for testing common-side code)
./gradlew genSources     # decompile Minecraft sources for IDE navigation
./gradlew clean          # clean build output
```

CI (`.github/workflows/build.yml`) runs `./gradlew build` on Ubuntu 24.04 with Java 25 for every push/PR and uploads `build/libs/` as artifacts.

## Tech Stack

- Java 25 (`options.release = 25`; source/target 25)
- Minecraft 26.2 · Fabric Loader 0.19.3 · Fabric API `0.156.0+26.2`
- Fabric Loom `1.17-SNAPSHOT` (Gradle plugin `net.fabricmc.fabric-loom`)
- Split source sets (`splitEnvironmentSourceSets()`): `src/main` + `src/client`
- Mixins (SpongePowered) with separate common and client configs
- SLF4J for logging; all versions/pins live in `gradle.properties`

## Build Config

- The project uses the **Groovy DSL** (`build.gradle`, `settings.gradle`). The Kotlin DSL duplicates the template originally generated (`build.gradle.kts`, `settings.gradle.kts`) have been removed — Gradle silently prefers `build.gradle`/`settings.gradle` when both exist, so they were dead weight.
- `loom { splitEnvironmentSourceSets() }` registers the `client` source set; `loom.mods { "compass-hud" }` combines `src/main` + `src/client`.
- `processResources` expands `${version}` inside `fabric.mod.json`.
- `java { withSourcesJar() }` + `maven-publish` for publishing.

## Architecture

```text
src/
  main/java/io/github/jsevenheck/compasshud/
    CompassHUD.java               <- ModInitializer entrypoint; MOD_ID, LOGGER, Identifier helper
    mixin/ExampleMixin.java
  main/resources/
    fabric.mod.json               <- mod metadata, entrypoints, mixins, dependencies
    compass-hud.mixins.json       <- common mixin config
    assets/compass-hud/icon.png
  client/java/io/github/jsevenheck/compasshud/client/
    CompassHUDClient.java         <- ClientModInitializer entrypoint; registers the HUD element
    CompassHudRenderer.java       <- HudElement implementation; all compass rendering logic
    mixin/ExampleClientMixin.java
  client/resources/
    compass-hud.client.mixins.json  <- client mixin config
```

The `io.github.jsevenheck.compasshud` package (both source sets) is the only package in this project — there is no other package to route code into.

### Entrypoints (`fabric.mod.json`)

| Entrypoint | Class | Interface |
| ---------- | ----- | --------- |
| `main` | `io.github.jsevenheck.compasshud.CompassHUD` | `ModInitializer.onInitialize()` |
| `client` | `io.github.jsevenheck.compasshud.client.CompassHUDClient` | `ClientModInitializer.onInitializeClient()` |

`CompassHUD.id(path)` is the canonical identifier helper (`Identifier.fromNamespaceAndPath(MOD_ID, path)`). Always use it instead of constructing `Identifier`s inline.

### Mixin configs

| Config file | Package | Example target |
| ----------- | ------- | -------------- |
| `compass-hud.mixins.json` | `io.github.jsevenheck.compasshud.mixin` | `MinecraftServer.loadLevel` |
| `compass-hud.client.mixins.json` | `io.github.jsevenheck.compasshud.client.mixin` | `Minecraft.run` |

Rules:
- Every mixin class must be registered in its config file or it will not load.
- `injectors.defaultRequire = 1` — injections must resolve or the game fails to load.
- `overwrites.requireAnnotations = true` — all overrides must carry the required annotations.

## HUD / Rendering

- All rendering lives in `src/client`. Never import `net.minecraft.client.*` classes from `src/main` — with split source sets the common set must stay client-agnostic.
- `CompassHUDClient.onInitializeClient()` registers `CompassHudRenderer` via `HudElementRegistry.addLast(CompassHUD.id("compass"), ...)` — the current Fabric HUD API (`net.fabricmc.fabric.api.client.rendering.v1.hud`), not the deprecated `HudRenderCallback`.
- `CompassHudRenderer implements HudElement`; its `extractRenderState(GuiGraphicsExtractor, DeltaTracker)` draws a horizontally scrolling compass strip (ticks every 5°/15°/45°, N/NE/E/SE/S/SW/W/NW letters, degree numbers between them, a fixed center marker) at the top-center of the screen. No Mixins or raw OpenGL are used for this — it's pure Fabric HUD API + `GuiGraphicsExtractor`.
- Bearing math: Minecraft yaw has 0°=south, 90°=west, 180°=north, 270°=east (verified against decompiled `Direction.fromYRot`). `calculateBearing()` converts this to a standard compass bearing (0°=N, 90°=E) via `wrapDegrees(yaw + 180)`. `angularDifference()` gives the signed shortest angle between a marker and the current bearing (handles the 359°→0° wrap); `angleToScreenX()` turns that into a pixel offset via the `PIXELS_PER_DEGREE` constant.
- Player look direction is read with `player.getViewYRot(partialTick)` (interpolated) rather than the raw per-tick yaw, so rotation is smooth between ticks.
- Tick/degree/direction labels are centered with `GuiGraphicsExtractor.centeredText()` (Mojang's own `x - font.width(text) / 2`, verified via bytecode against the real jar) — do not reintroduce a custom centering offset; an earlier hand-rolled correction (`x - (width + 1) / 2`) was tried to fix perceived off-center labels and made it worse. Vanilla's formula is the one used throughout the game's own menus/buttons and is correct as-is.
- Renderer returns early if `Minecraft.getInstance().player`/`.level` is null (no world loaded, e.g. on the title screen).
- Layout/color constants (`HUD_WIDTH`, `HUD_Y`, `BAR_HEIGHT`, `PIXELS_PER_DEGREE`, tick heights, colors) are declared at the top of `CompassHudRenderer` — tune the compass there instead of touching the render logic.
- Known open issue: vanilla's boss bar also renders top-center and can visually overlap the compass bar during boss fights; not currently handled.
- **Waypoint dots** (`renderWaypoints`/`renderWaypoint`): other players (and any other server-broadcast waypoint, e.g. glowing teammates) are drawn as dots directly on the compass bar itself (`WAYPOINT_ROW_Y` is vertically centered within `BAR_HEIGHT`, overlapping ticks/labels the same way vanilla's own dot overlaps its slim XP-bar background), reusing vanilla's own locator-bar dot sprites/colors and up/down out-of-frustum arrows — same `ClientWaypointManager` (`player.connection.getWaypointManager()`), `WaypointStyleManager` (`Minecraft.gui.hud.getWaypointStyles()`), and `hud/locator_bar_arrow_up`/`_down` sprite identifiers the vanilla XP-bar locator bar uses, verified via decompiled `net.minecraft.client.gui.contextualbar.LocatorBar`/`TrackedWaypoint` (run `./gradlew genClientOnlySources`/`genCommonSources` to get readable sources for these classes again if needed — Loom pulls in Vineflower on demand). `TrackedWaypoint.yawAngleToCamera()` already returns the signed camera-relative angle in degrees (positive = clockwise), which is the same quantity `angularDifference()` produces for compass ticks, so waypoints plug into the same `centerX + angle * PIXELS_PER_DEGREE` placement without needing their own bearing conversion.
- Waypoints only appear on a real multiplayer/LAN session with other nearby players — vanilla only broadcasts a `LivingEntity`'s waypoint to other players when there's actually another player to send it to, so a solo singleplayer world will never show any dots (nothing to test there; use two clients or a dedicated server).

## Logging Rules

- Use the shared `CompassHUD.LOGGER` (SLF4J, named by `MOD_ID`) so logs are easy to filter.
- Keep messages concise and lifecycle-focused (init, load, render setup, errors).
- Never log secrets or sensitive data (tokens, credentials, account data).

## Adding a Mixin / Feature

- New mixin → put the class in the matching package (`...compasshud.mixin` for common, `...compasshud.client.mixin` for client-only) and register it in the corresponding config file.
- New entrypoint or dependency → update `fabric.mod.json` (`entrypoints` / `depends`).
- New assets (icons, textures, lang) → `src/main/resources/assets/compass-hud/`.
- Version bumps (Minecraft, loader, API, mod version) → `gradle.properties`.

## Graphify

Graphify is available for codebase architecture and relationship queries.

- Use `graphify update .` to re-extract changed code files and update the existing graph without requiring an LLM API key.
- Use `graphify query "..."` for architecture, file relationship, or call-graph questions before broad grep/read searches.
- Use `graphify path "..."` to trace dependency / call paths between files or symbols.
- Use `graphify explain "..."` to get an explanation of a specific subgraph or symbol relationships.
- If `graphify-out/graph.json` exists, prefer a targeted Graphify query for high-level codebase context.
- **Never read or directly access `graphify-out/`** (e.g. `graphify-out/graph.json`). Always go through the `graphify update .`, `graphify query`, `graphify path`, or `graphify explain` commands instead — the raw graph files are an internal artifact and must not be inspected directly.
- `.graphifyignore` restricts the graph to Java sources only (`src/main` + `src/client`).
