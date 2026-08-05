# CLAUDE.md

Single Gradle (Fabric Loom) project, mod id **`compass-hud`**: a client-side utility mod for
Minecraft 26.2. It bundles multiple independent features behind one mod jar/id — currently a compass
HUD strip, inventory sorting, and an improved Bundle UI — organized as self-contained `feature` packages under one
shared entrypoint.

```text
src/main/        <- common mod code: ModInitializer entrypoint and feature-agnostic pure logic
src/client/      <- client-only code: feature registry + implementations, HUD rendering, ClientModInitializer, client mixins
src/test/        <- JUnit 5 tests for pure logic and focused Minecraft adapter behavior
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
- Mixins (SpongePowered) with a client config — the Bundle opening hit-test uses one accessor mixin because the vanilla GUI origin is protected; otherwise prefer Fabric API events first
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
    feature/inventorysort/         <- pure, Minecraft-independent inventory-sort planning logic (unit-tested from src/test)
      ItemIdentity.java              <- record: namespace/path/componentKey/customName + deterministic compare()
      SortSlot.java                   <- record: logical slot + identity + count + max stack size
      InventorySortPlanner.java       <- List<SortSlot> -> sorted/consolidated target List<SortSlot>
      ClickOperation.java             <- record describing one planned container click + its pre-conditions
      InventoryClickPlanner.java      <- (current, target) -> List<ClickOperation> (consolidate + permute)
  main/resources/
    fabric.mod.json               <- mod metadata, entrypoints, mixins, dependencies
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
    bundle/
      BundleFeature.java             <- Shift + Right Click entry point for the client Bundle screen
      BundleScreen.java              <- virtual Bundle slots plus real player-inventory slots
      BundleInteractionPlanner.java  <- validated vanilla click sequences for extraction/insertion
      BundleInteractionExecutor.java <- tick-paced execution against the real InventoryMenu
    mixin/AbstractContainerScreenAccessor.java <- client GUI-origin accessor for opening hit-tests
  client/resources/
    compass-hud.client.mixins.json  <- client mixin config
  test/java/io/github/jsevenheck/utilsmod/
    client/feature/inventorysort/ItemIdentitiesTest.java <- component-patch key determinism tests
    feature/inventorysort/
      InventorySortPlannerTest.java   <- unit tests for the sort planner
      InventoryClickPlannerTest.java  <- unit tests for the click planner, with a from-scratch click simulator
    feature/InventoryOperationLockTest.java <- shared sort/Bundle-operation exclusion tests
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
- `FeatureRegistry` holds a static `List<ModFeature>` (currently `CompassHudFeature`, `InventorySortFeature`, `BundleFeature`) and calls `initializeClient()` on each from `UtilsModClient`. To add a feature, implement `ModFeature` in a new `feature/<name>/` package and add it to that list — no other wiring is needed.
- Shared client-side settings for all features live in one `ModConfig` (Gson JSON at `config/compass-hud.json`, lazy singleton via `ModConfig.get()`); add new fields there rather than inventing a second config file.
- `InventoryOperationLock` is the shared, Minecraft-independent exclusion guard. Opening the Bundle screen briefly claims it to exclude a concurrent sort, then Bundle interactions claim it only while executing. The inventory sorter can therefore sort the real player menu while the virtual Bundle screen remains open, while clicks and Bundle operations still cannot overlap.

### Mixin configs

| Config file | Package | Target |
| ----------- | ------- | ------ |
| `compass-hud.client.mixins.json` | `io.github.jsevenheck.utilsmod.client.mixin` | `AbstractContainerScreen` GUI-origin fields |

Rules:
- Every mixin class must be registered in its config file or it will not load.
- `injectors.defaultRequire = 1` — injections must resolve or the game fails to load.
- `overwrites.requireAnnotations = true` — all overrides must carry the required annotations.
- Only the Bundle feature currently needs a mixin: the accessor exposes the protected GUI origin for opening hit-tests. The other features use public Fabric API events. Only add another mixin if there's truly no event/API alternative.

## Feature documentation

Detailed feature behaviour, commands, supported screens, configuration, limitations, and manual test
checklists live in the documentation instead of this instruction file:

- `README.md` — short user-facing overview and command reference.
- `docs/features.md` — central feature documentation.
- `docs/local-development.md` — manual development and smoke tests.
- `docs/CURSEFORGE_DESCRIPTION.md` — public project description.
- `docs/publishing.md` — release checklist and publishing workflow.

Keep this file focused on project architecture and implementation constraints. When changing a feature,
update the relevant documentation as part of the same change. The source layout and feature ownership
remain documented in the architecture sections above; do not move client-only code into `src/main`, add
server requirements, or bypass existing validation and operation-lock rules.

## Logging Rules

- Use the shared `UtilsMod.LOGGER` (SLF4J, named by `MOD_ID`) so logs are easy to filter.
- Keep messages concise and lifecycle-focused (init, load, render setup, errors).
- Never log secrets or sensitive data (tokens, credentials, account data).

## Adding a Mixin / Feature

- New feature → create `feature/<name>/` (pure logic, if any, in `src/main`) and `client/feature/<name>/` (a `ModFeature` implementation plus any Minecraft-facing glue) and add it to `FeatureRegistry`'s list.
- New mixin → put client-only accessors/injections in `...utilsmod.client.mixin` and register them in `compass-hud.client.mixins.json`. Add a common package/config only if common-side behavior truly requires one. Prefer a Fabric API event first; only reach for a mixin if there truly is no event/API alternative.
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
