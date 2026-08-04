# CLAUDE.md

Single Gradle (Fabric Loom) project, mod id **`compass-hud`**: a client-side utility mod for
Minecraft 26.2. It bundles multiple independent features behind one mod jar/id — currently a compass
HUD strip and an inventory-sort keybind — organized as self-contained `feature` packages under one
shared entrypoint.

```text
src/main/        <- common mod code: ModInitializer entrypoint, feature-agnostic pure logic, common mixins
src/client/      <- client-only code: feature registry + implementations, HUD rendering, ClientModInitializer, client mixins
src/test/        <- JUnit 5 tests for the Minecraft-independent pure logic in src/main
```

The mod is **client-only** (`"environment": "client"` in `fabric.mod.json`) and runs inside the Minecraft client via Fabric Loader. It has no standalone runtime or toolchain — it only works inside the game, and it is never required server-side.

## Commands (always run from workspace root)

```bash
./gradlew build          # build the mod jar into build/libs/ (also runs the test task below)
./gradlew test           # run the JUnit 5 unit tests under src/test
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
- Mixins (SpongePowered) with separate common and client configs — currently unused by either feature; only add one if strictly necessary, prefer Fabric API events first
- SLF4J for logging; JUnit 5 (`junit-bom`/`junit-jupiter`/`junit-platform-launcher`) for `src/test`; Gson (bundled with the game) for config persistence — no new runtime dependencies were added for any of this
- All versions/pins live in `gradle.properties`

## Build Config

- The project uses the **Groovy DSL** (`build.gradle`, `settings.gradle`). The Kotlin DSL duplicates the template originally generated (`build.gradle.kts`, `settings.gradle.kts`) have been removed — Gradle silently prefers `build.gradle`/`settings.gradle` when both exist, so they were dead weight.
- `loom { splitEnvironmentSourceSets() }` registers the `client` source set; `loom.mods { "compass-hud" }` combines `src/main` + `src/client`.
- `processResources` expands `${version}` inside `fabric.mod.json`.
- `java { withSourcesJar() }` + `maven-publish` for publishing.
- `repositories { mavenCentral() }` was added (alongside the Fabric/Loom defaults) specifically so the JUnit test dependencies resolve.
- `base { archivesName = "utils-mod" }` names the built jar `utils-mod-<version>.jar` — independent of, and does not change, the `compass-hud` mod id (`"id"` in `fabric.mod.json`).
- `test { useJUnitPlatform() }`; `testRuntimeOnly "org.junit.platform:junit-platform-launcher"` is required in Gradle 9's `jvm-test-suite` setup or the `test` task fails with "Failed to load JUnit Platform" even though `junit-jupiter` alone looks sufficient.

## Architecture

```text
src/
  main/java/io/github/jsevenheck/utilsmod/
    UtilsMod.java             <- ModInitializer entrypoint; MOD_ID, LOGGER, Identifier helper (whole-mod, not feature-specific)
    mixin/ExampleMixin.java
    feature/inventorysort/         <- pure, Minecraft-independent inventory-sort planning logic (unit-tested from src/test)
      ItemIdentity.java              <- record: namespace/path/componentKey/customName + deterministic compare()
      SortSlot.java                   <- record: logical slot + identity + count + max stack size
      InventorySortPlanner.java       <- List<SortSlot> -> sorted/consolidated target List<SortSlot>
      ClickOperation.java             <- record describing one planned container click + its pre-conditions
      InventoryClickPlanner.java      <- (current, target) -> List<ClickOperation> (consolidate + permute)
  main/resources/
    fabric.mod.json               <- mod metadata, entrypoints, mixins, dependencies
    compass-hud.mixins.json       <- common mixin config
    assets/compass-hud/icon.png
    assets/compass-hud/lang/      <- en_us.json, de_de.json
  client/java/io/github/jsevenheck/utilsmod/client/
    UtilsModClient.java      <- ClientModInitializer entrypoint; delegates to FeatureRegistry
    config/ModConfig.java         <- Gson-backed JSON settings for all client features (config/compass-hud.json)
    feature/ModFeature.java       <- interface: void initializeClient()
    feature/FeatureRegistry.java  <- static list of every ModFeature; initializes them all
    feature/compass/
      CompassHudFeature.java        <- registers the HUD element
      CompassHudRenderer.java       <- HudElement implementation; all compass rendering logic
    feature/inventorysort/
      InventorySortFeature.java     <- registers the keybind, drives the controller every client tick
      InventorySortController.java  <- package-private orchestration: resolve -> plan -> hand off to the executor
      InventoryClickExecutor.java   <- package-private tick-driven playback of a planned click queue
      SortableSlotResolver.java     <- package-private: current screen/menu -> sortable SortSlots (or none)
      SortSession.java              <- package-private record: menu + player-section slots + container-section slots
      ItemIdentities.java           <- package-private: ItemStack -> ItemIdentity (component-based key)
    mixin/ExampleClientMixin.java
  client/resources/
    compass-hud.client.mixins.json  <- client mixin config
  test/java/io/github/jsevenheck/utilsmod/feature/inventorysort/
    InventorySortPlannerTest.java   <- unit tests for the sort planner
    InventoryClickPlannerTest.java  <- unit tests for the click planner, with a from-scratch click simulator
```

The `io.github.jsevenheck.utilsmod` package (both source sets) is the only base package in this project. Everything feature-specific goes under a `feature/<name>/` subpackage of it (with the pure/testable half in `src/main` and the Minecraft-facing half in `src/client`); there's no other place to route new code.

### Entrypoints (`fabric.mod.json`)

| Entrypoint | Class | Interface |
| ---------- | ----- | --------- |
| `main` | `io.github.jsevenheck.utilsmod.UtilsMod` | `ModInitializer.onInitialize()` |
| `client` | `io.github.jsevenheck.utilsmod.client.UtilsModClient` | `ClientModInitializer.onInitializeClient()` |

`UtilsMod` and `UtilsModClient` are the whole-mod bootstrap classes (constants/logger/id-helper, and "initialize every feature," respectively) — named after the mod's role (a general client-side utilities mod), not after any one feature; compass-specific logic lives entirely in `feature/compass`, so the class names stay accurate as more features are added. `UtilsMod.id(path)` is the canonical identifier helper (`Identifier.fromNamespaceAndPath(MOD_ID, path)`, still namespaced under the `compass-hud` mod id). Always use it instead of constructing `Identifier`s inline. `UtilsModClient.onInitializeClient()` does nothing but call `FeatureRegistry.initializeAll()`.

### Feature architecture

- `ModFeature` is a one-method interface (`initializeClient()`); each feature under `client/feature/<name>/` implements it and does all of its own Fabric API registration inside that method.
- `FeatureRegistry` holds a static `List<ModFeature>` (currently `CompassHudFeature`, `InventorySortFeature`) and calls `initializeClient()` on each from `UtilsModClient`. To add a feature, implement `ModFeature` in a new `feature/<name>/` package and add it to that list — no other wiring is needed.
- Shared client-side settings for all features live in one `ModConfig` (Gson JSON at `config/compass-hud.json`, lazy singleton via `ModConfig.get()`); add new fields there rather than inventing a second config file.

### Mixin configs

| Config file | Package | Example target |
| ----------- | ------- | -------------- |
| `compass-hud.mixins.json` | `io.github.jsevenheck.utilsmod.mixin` | `MinecraftServer.loadLevel` |
| `compass-hud.client.mixins.json` | `io.github.jsevenheck.utilsmod.client.mixin` | `Minecraft.run` |

Rules:
- Every mixin class must be registered in its config file or it will not load.
- `injectors.defaultRequire = 1` — injections must resolve or the game fails to load.
- `overwrites.requireAnnotations = true` — all overrides must carry the required annotations.
- Neither feature currently needs a mixin — both are built entirely on public Fabric API + client tick events. Only add a mixin if there's truly no event/API alternative.

## HUD / Rendering (Compass HUD feature)

- All rendering lives in `src/client` under `feature/compass/`. Never import `net.minecraft.client.*` classes from `src/main` — with split source sets the common set must stay client-agnostic.
- `CompassHudFeature.initializeClient()` registers `CompassHudRenderer` via `HudElementRegistry.addLast(UtilsMod.id("compass"), ...)` — the current Fabric HUD API (`net.fabricmc.fabric.api.client.rendering.v1.hud`), not the deprecated `HudRenderCallback`.
- `CompassHudRenderer implements HudElement`; its `extractRenderState(GuiGraphicsExtractor, DeltaTracker)` draws a horizontally scrolling compass strip (ticks every 5°/15°/45°, N/NE/E/SE/S/SW/W/NW letters, degree numbers between them, a fixed center marker) at the top-center of the screen. No Mixins or raw OpenGL are used for this — it's pure Fabric HUD API + `GuiGraphicsExtractor`.
- Bearing math: Minecraft yaw has 0°=south, 90°=west, 180°=north, 270°=east (verified against decompiled `Direction.fromYRot`). `calculateBearing()` converts this to a standard compass bearing (0°=N, 90°=E) via `wrapDegrees(yaw + 180)`. `angularDifference()` gives the signed shortest angle between a marker and the current bearing (handles the 359°→0° wrap); `angleToScreenX()` turns that into a pixel offset via the `PIXELS_PER_DEGREE` constant.
- Player look direction is read with `player.getViewYRot(partialTick)` (interpolated) rather than the raw per-tick yaw, so rotation is smooth between ticks.
- Tick/degree/direction labels are centered with `GuiGraphicsExtractor.centeredText()` (Mojang's own `x - font.width(text) / 2`, verified via bytecode against the real jar) — do not reintroduce a custom centering offset; an earlier hand-rolled correction (`x - (width + 1) / 2`) was tried to fix perceived off-center labels and made it worse. Vanilla's formula is the one used throughout the game's own menus/buttons and is correct as-is.
- Renderer returns early if `Minecraft.getInstance().player`/`.level` is null (no world loaded, e.g. on the title screen).
- Layout/color constants (`HUD_WIDTH`, `HUD_Y`, `BAR_HEIGHT`, `PIXELS_PER_DEGREE`, tick heights, colors) are declared at the top of `CompassHudRenderer` — tune the compass there instead of touching the render logic.
- Known open issue: vanilla's boss bar also renders top-center and can visually overlap the compass bar during boss fights; not currently handled.
- **Waypoint dots** (`renderWaypoints`/`renderWaypoint`): other players (and any other server-broadcast waypoint, e.g. glowing teammates) are drawn as dots directly on the compass bar itself (`WAYPOINT_ROW_Y` is vertically centered within `BAR_HEIGHT`, overlapping ticks/labels the same way vanilla's own dot overlaps its slim XP-bar background), reusing vanilla's own locator-bar dot sprites/colors and up/down out-of-frustum arrows — same `ClientWaypointManager` (`player.connection.getWaypointManager()`), `WaypointStyleManager` (`Minecraft.gui.hud.getWaypointStyles()`), and `hud/locator_bar_arrow_up`/`_down` sprite identifiers the vanilla XP-bar locator bar uses, verified via decompiled `net.minecraft.client.gui.contextualbar.LocatorBar`/`TrackedWaypoint` (run `./gradlew genClientOnlySources`/`genCommonSources` to get readable sources for these classes again if needed — Loom pulls in Vineflower on demand). `TrackedWaypoint.yawAngleToCamera()` already returns the signed camera-relative angle in degrees (positive = clockwise), which is the same quantity `angularDifference()` produces for compass ticks, so waypoints plug into the same `centerX + angle * PIXELS_PER_DEGREE` placement without needing their own bearing conversion.
- Waypoints only appear on a real multiplayer/LAN session with other nearby players — vanilla only broadcasts a `LivingEntity`'s waypoint to other players when there's actually another player to send it to, so a solo singleplayer world will never show any dots (nothing to test there; use two clients or a dedicated server).

## Inventory Sort feature

- Split the same way as everything else: `feature/inventorysort/` in `src/main` is pure planning logic with **no Minecraft imports at all** (unit-tested directly in `src/test`); `client/feature/inventorysort/` in `src/client` is the thin Minecraft-facing glue that feeds it real data and plays back its output as real clicks.
- **Trigger**: `InventorySortFeature` registers one rebindable `KeyMapping` (default `R`, vanilla `KeyMapping.Category.INVENTORY`) via `fabric-key-mapping-api-v1`, and drives everything from `ClientTickEvents.END_CLIENT_TICK` (`fabric-lifecycle-events-v1`). Outside screens, `sortKey.consumeClick()` provides the debounced trigger; while an inventory/container screen has focus, `ScreenKeyboardEvents.afterKeyPress` captures the bound key and hands the pending trigger to the same tick handler.
- **Resolution** (`SortableSlotResolver`): identifies sortable slots purely from live menu/slot metadata and ownership, never hardcoded slot indices. Only `InventoryMenu` (the player's own screen) and a small chest-like menu-class allowlist (`ChestMenu`, `HopperMenu`, `DispenserMenu`, `ShulkerBoxMenu`) are recognized at all — every other menu type (crafting, furnace, anvil, enchanting, brewing, beacon, merchant, loom, stonecutter, cartography, smithing, grindstone, creative inventory, ...) is simply absent from the allowlist and therefore untouched. Within a recognized menu, player-inventory slots are those whose backing `container` is the player's own `Inventory`, whose `getContainerSlot()` is 9–35 (main inventory only; the hotbar is always excluded), and whose class is exactly `Slot.class`. Container-side slots require `getClass() == Slot.class` or `ShulkerBoxSlot.class` and a `container` different from the player's inventory. Spectators and null player/screen bail out immediately.
- **Planning** is two pure, independently unit-tested stages operating on an immutable snapshot (`List<SortSlot>`), never the live menu:
  1. `InventorySortPlanner.plan(List<SortSlot>)` — groups by `ItemIdentity`, sums counts, sorts identities via `ItemIdentity.compare` (namespace → path → component key → custom name), consolidates each identity into as few slots as possible (capped by the *minimum* max-stack-size observed among that identity's occupied slots, so an item somehow already split with different caps never over-fills a smaller-cap slot), then fills the rest with empty `SortSlot`s. No-op (`plan(x)` reproduces the same order) when the input is already sorted.
  2. `InventoryClickPlanner.plan(current, target)` — turns "current state" and "target state" into a minimal, ordered `List<ClickOperation>` via a two-phase algorithm: Phase 1 consolidates partial stacks of the same identity using a guarded vanilla `PICKUP_ALL` fast path when the complete identity group fits in one cursor stack and is safe for the complete open menu; larger groups use the waterfall pickup/merge/spillback algorithm. Phase 2 realizes the remaining current→target permutation as direct greedy swaps using inverse-index bookkeeping. Every `ClickOperation` records what it expects to find (slot identity/count, cursor empty or not, and interaction kind) *before* it fires, so the executor can verify state rather than assume it.
- **Execution** (`InventoryClickExecutor`, driven by `InventorySortController` from the same tick handler): plays back at most one `ClickOperation` per client tick by default (`clickDelayTicks = 1`; larger values add ticks between interactions), exclusively through `Minecraft.gameMode.handleContainerInput(...)` with either `ContainerInput.PICKUP` or the guarded `ContainerInput.PICKUP_ALL` — the same client-facing API vanilla screens use (predicts locally, sends the normal server packet) — never mutating menu/slot state directly. Before every interaction it re-validates the real slot/cursor against the operation's expectations and that the screen/menu/`containerId` haven't changed and the player/level still exist; any mismatch aborts the remaining queue immediately. A run only reports success if the cursor is empty once the queue is exhausted. Starting a new sort requires an empty cursor and no sort already in progress (`InventorySortController` holds at most one active `InventoryClickExecutor`).
- **Feedback**: `LocalPlayer.sendOverlayMessage(Component.translatable(...))` only (action-bar, local-only, never sent to server/chat), for: unsupported menu, cursor not empty, already sorted, and aborted. No "sort started" message — the visible rearrangement is its own feedback and the spec explicitly asked to keep this unobtrusive. Translation keys live in `assets/compass-hud/lang/{en_us,de_de}.json`.
- **Config** (`ModConfig`, all client-only, all in `config/compass-hud.json`): `inventorySortEnabled`, `sortSectionsIndependently` (default `true` — never redistributes items between an open container and the player's own inventory unless the user opts into `false`), `clickDelayTicks` (default `1`, clamped to a minimum of 1 via `effectiveClickDelayTicks()`). The hotbar is never included.
- **Tests** (`src/test/.../feature/inventorysort/`): `InventorySortPlannerTest` covers empty inventory, already-sorted, mixed identifiers, multi-partial-stack consolidation, same-item-different-components (not merged), non-stackable items, a full inventory with no spare slots, excluded slots never reaching the planner, and deterministic output across runs. `InventoryClickPlannerTest` replays every produced `ClickOperation` through a hand-written simulator of vanilla pickup/merge/swap semantics and asserts each operation's expectations hold, the cursor starts/ends empty, and the final state matches the target exactly — including the full-inventory reorder and same-identity/different-count swap cases.

## Logging Rules

- Use the shared `UtilsMod.LOGGER` (SLF4J, named by `MOD_ID`) so logs are easy to filter.
- Keep messages concise and lifecycle-focused (init, load, render setup, errors).
- Never log secrets or sensitive data (tokens, credentials, account data).

## Adding a Mixin / Feature

- New feature → create `feature/<name>/` (pure logic, if any, in `src/main`) and `client/feature/<name>/` (a `ModFeature` implementation plus any Minecraft-facing glue) and add it to `FeatureRegistry`'s list.
- New mixin → put the class in the matching package (`...utilsmod.mixin` for common, `...utilsmod.client.mixin` for client-only) and register it in the corresponding config file. Prefer a Fabric API event first; only reach for a mixin if there truly is no event/API alternative.
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
