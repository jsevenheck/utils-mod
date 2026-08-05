# Graph Report - utils-mod  (2026-08-05)

## Corpus Check
- 36 files · ~13,912 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 372 nodes · 994 edges · 11 communities (9 shown, 2 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 54 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5a52e965`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ItemIdentity
- ModConfig
- .tick
- BundleScreen
- BundleInteractionExecutor
- CompassHudRenderer.java
- .plan
- LocalWaypointService
- ItemIdentitiesTest
- WaypointMarker
- WaypointProfileResolver

## God Nodes (most connected - your core abstractions)
1. `BundleScreen` - 36 edges
2. `LocalWaypointService` - 36 edges
3. `ItemIdentity` - 26 edges
4. `SortSlot` - 26 edges
5. `CompassWaypointCommands` - 22 edges
6. `ClickOperation` - 20 edges
7. `ModConfig` - 19 edges
8. `CompassHudRenderer` - 18 edges
9. `WaypointMarker` - 18 edges
10. `InventoryClickPlanner` - 16 edges

## Surprising Connections (you probably didn't know these)
- `ModConfig` --references--> `WaypointProfile`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/config/ModConfig.java → src/main/java/io/github/jsevenheck/utilsmod/feature/compass/WaypointProfile.java
- `LocalWaypointService` --references--> `ModConfig`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/LocalWaypointService.java → src/client/java/io/github/jsevenheck/utilsmod/client/config/ModConfig.java
- `BundleInteractionExecutor` --references--> `BundleScreen`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleInteractionExecutor.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleScreen.java
- `CompassHudRenderer` --references--> `LocalWaypointService`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/CompassHudRenderer.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/LocalWaypointService.java
- `LocalWaypointService` --references--> `WaypointProfileResolver`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/LocalWaypointService.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/WaypointProfileResolver.java

## Import Cycles
- None detected.

## Communities (11 total, 2 thin omitted)

### Community 0 - "ItemIdentity"
Cohesion: 0.12
Nodes (11): ClickOperation, Kind, PICKUP, PICKUP_ALL, Cell, ContentKey, InventoryClickPlanner, ItemIdentity (+3 more)

### Community 1 - "ModConfig"
Cohesion: 0.07
Nodes (24): Accessor, ClientModInitializer, Gson, Logger, Mixin, ModInitializer, ModConfig, BundleFeature (+16 more)

### Community 2 - ".tick"
Cohesion: 0.10
Nodes (14): Slot, InventoryClickExecutor, AbstractContainerMenu, Minecraft, InventorySortController, Minecraft, ItemIdentities, DataComponentPatch (+6 more)

### Community 3 - "BundleScreen"
Cohesion: 0.09
Nodes (16): AfterEach, KeyEvent, Screen, BundleScreen, AbstractContainerMenu, BundleContents, GuiGraphicsExtractor, Identifier (+8 more)

### Community 4 - "BundleInteractionExecutor"
Cohesion: 0.17
Nodes (13): ContainerInput, BundleInteractionExecutor, AbstractContainerMenu, Inventory, Minecraft, BundleClickStep, BundleInteractionPlanner, BundleSelectionStep (+5 more)

### Community 5 - "CompassHudRenderer.java"
Cohesion: 0.18
Nodes (15): Camera, DeltaTracker, Font, GameRenderer, HudElement, Level, PartialTickSupplier, CompassHudRenderer (+7 more)

### Community 8 - "LocalWaypointService"
Cohesion: 0.11
Nodes (16): Component, CompassWaypointCommands, Failure, DUPLICATE_NAME, INVALID_COLOR, INVALID_NAME, NO_ACTIVE_PROFILE, NO_PLAYER (+8 more)

### Community 9 - "ItemIdentitiesTest"
Cohesion: 0.44
Nodes (4): ItemIdentitiesTest, DataComponentPatch, DataComponentType, Test

### Community 10 - "WaypointMarker"
Cohesion: 0.11
Nodes (8): ColorPreset, LocalWaypointRules, WaypointMarker, WaypointProfile, Test, LocalWaypointRulesTest, Test, WaypointProfileTest

## Knowledge Gaps
- **9 isolated node(s):** `NONE`, `NO_ACTIVE_PROFILE`, `NO_PLAYER`, `INVALID_NAME`, `DUPLICATE_NAME` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ModConfig` connect `ModConfig` to `.tick`, `BundleScreen`, `CompassHudRenderer.java`, `LocalWaypointService`, `WaypointMarker`?**
  _High betweenness centrality (0.365) - this node is a cross-community bridge._
- **Why does `BundleScreen` connect `BundleScreen` to `ModConfig`, `.tick`, `BundleInteractionExecutor`?**
  _High betweenness centrality (0.196) - this node is a cross-community bridge._
- **Why does `LocalWaypointService` connect `LocalWaypointService` to `ModConfig`, `WaypointMarker`, `WaypointProfileResolver`, `CompassHudRenderer.java`?**
  _High betweenness centrality (0.176) - this node is a cross-community bridge._
- **What connects `NONE`, `NO_ACTIVE_PROFILE`, `NO_PLAYER` to the rest of the system?**
  _9 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ItemIdentity` be split into smaller, more focused modules?**
  _Cohesion score 0.12395929694727105 - nodes in this community are weakly interconnected._
- **Should `ModConfig` be split into smaller, more focused modules?**
  _Cohesion score 0.06636500754147813 - nodes in this community are weakly interconnected._
- **Should `.tick` be split into smaller, more focused modules?**
  _Cohesion score 0.09672830725462304 - nodes in this community are weakly interconnected._