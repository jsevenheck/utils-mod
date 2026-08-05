# Graph Report - utils-mod  (2026-08-05)

## Corpus Check
- 28 files · ~10,526 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 272 nodes · 680 edges · 10 communities (9 shown, 1 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dc52a712`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ItemIdentity
- BundleFeature.java
- Slot
- BundleScreen
- BundleInteractionExecutor
- CompassHudRenderer
- .plan
- InventorySortController.java
- ClickOperation
- ItemIdentitiesTest

## God Nodes (most connected - your core abstractions)
1. `BundleScreen` - 32 edges
2. `ItemIdentity` - 26 edges
3. `SortSlot` - 26 edges
4. `ClickOperation` - 20 edges
5. `InventoryClickPlanner` - 16 edges
6. `InventoryClickPlannerTest` - 16 edges
7. `InventorySortPlannerTest` - 16 edges
8. `CompassHudRenderer` - 15 edges
9. `BundleInteractionExecutor` - 13 edges
10. `BundleInteractionPlanner` - 12 edges

## Surprising Connections (you probably didn't know these)
- `BundleInteractionExecutor` --references--> `BundleScreen`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleInteractionExecutor.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleScreen.java
- `InventorySortFeature` --references--> `InventorySortController`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortFeature.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortController.java
- `SortSession` --references--> `SortSlot`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/SortSession.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/SortSlot.java
- `ClickOperation` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ClickOperation.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java
- `InventorySortPlannerTest` --references--> `ItemIdentity`  [EXTRACTED]
  src/test/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventorySortPlannerTest.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java

## Import Cycles
- None detected.

## Communities (10 total, 1 thin omitted)

### Community 0 - "ItemIdentity"
Cohesion: 0.14
Nodes (7): Cell, ContentKey, InventoryClickPlanner, ItemIdentity, SortSlot, InventoryClickPlannerTest, Test

### Community 1 - "BundleFeature.java"
Cohesion: 0.07
Nodes (24): Accessor, ClientModInitializer, Gson, Logger, Mixin, ModInitializer, ModConfig, BundleFeature (+16 more)

### Community 2 - "Slot"
Cohesion: 0.16
Nodes (9): Slot, ItemIdentities, DataComponentPatch, DataComponentType, ItemStack, Minecraft, SortableSlotResolver, AbstractContainerMenu (+1 more)

### Community 3 - "BundleScreen"
Cohesion: 0.13
Nodes (11): KeyEvent, Screen, BundleScreen, AbstractContainerMenu, BundleContents, GuiGraphicsExtractor, Identifier, Inventory (+3 more)

### Community 4 - "BundleInteractionExecutor"
Cohesion: 0.17
Nodes (13): ContainerInput, BundleInteractionExecutor, AbstractContainerMenu, Inventory, Minecraft, BundleClickStep, BundleInteractionPlanner, BundleSelectionStep (+5 more)

### Community 5 - "CompassHudRenderer"
Cohesion: 0.19
Nodes (15): Camera, DeltaTracker, Font, GameRenderer, HudElement, Level, LocalPlayer, PartialTickSupplier (+7 more)

### Community 7 - "InventorySortController.java"
Cohesion: 0.18
Nodes (5): AfterEach, InventoryOperationLock, InventorySortPlanner, InventoryOperationLockTest, Test

### Community 8 - "ClickOperation"
Cohesion: 0.18
Nodes (9): InventoryClickExecutor, AbstractContainerMenu, Minecraft, InventorySortController, Minecraft, ClickOperation, Kind, PICKUP (+1 more)

### Community 9 - "ItemIdentitiesTest"
Cohesion: 0.44
Nodes (4): ItemIdentitiesTest, DataComponentPatch, DataComponentType, Test

## Knowledge Gaps
- **2 isolated node(s):** `PICKUP`, `PICKUP_ALL`
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SortSlot` connect `ItemIdentity` to `ClickOperation`, `Slot`, `.plan`, `InventorySortController.java`?**
  _High betweenness centrality (0.175) - this node is a cross-community bridge._
- **Why does `ItemIdentity` connect `ItemIdentity` to `ClickOperation`, `Slot`, `.plan`, `InventorySortController.java`?**
  _High betweenness centrality (0.169) - this node is a cross-community bridge._
- **Why does `BundleScreen` connect `BundleScreen` to `BundleFeature.java`, `BundleInteractionExecutor`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **What connects `PICKUP`, `PICKUP_ALL` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ItemIdentity` be split into smaller, more focused modules?**
  _Cohesion score 0.13937282229965156 - nodes in this community are weakly interconnected._
- **Should `BundleFeature.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06612244897959184 - nodes in this community are weakly interconnected._
- **Should `BundleScreen` be split into smaller, more focused modules?**
  _Cohesion score 0.1337126600284495 - nodes in this community are weakly interconnected._