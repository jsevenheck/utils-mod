# Graph Report - .  (2026-08-05)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 271 nodes · 664 edges · 12 communities (10 shown, 2 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d5cbba0e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ItemIdentity
- BundleFeature.java
- .tick
- BundleScreen
- BundleInteractionExecutor
- CompassHudRenderer
- .plan
- InventoryOperationLock
- AbstractContainerScreenAccessor
- ExampleClientMixin.java
- ExampleMixin.java
- GuiGraphicsExtractor

## God Nodes (most connected - your core abstractions)
1. `BundleScreen` - 32 edges
2. `ItemIdentity` - 25 edges
3. `SortSlot` - 23 edges
4. `ClickOperation` - 20 edges
5. `InventorySortPlannerTest` - 16 edges
6. `CompassHudRenderer` - 15 edges
7. `InventoryClickPlannerTest` - 14 edges
8. `BundleInteractionExecutor` - 13 edges
9. `InventoryClickPlanner` - 13 edges
10. `BundleInteractionPlanner` - 12 edges

## Surprising Connections (you probably didn't know these)
- `BundleInteractionExecutor` --references--> `BundleScreen`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleInteractionExecutor.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/bundle/BundleScreen.java
- `InventoryClickExecutor` --references--> `ClickOperation`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventoryClickExecutor.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ClickOperation.java
- `InventorySortFeature` --references--> `InventorySortController`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortFeature.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortController.java
- `SortSession` --references--> `SortSlot`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/SortSession.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/SortSlot.java
- `InventorySortPlannerTest` --references--> `ItemIdentity`  [EXTRACTED]
  src/test/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventorySortPlannerTest.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java

## Import Cycles
- None detected.

## Communities (12 total, 2 thin omitted)

### Community 0 - "ItemIdentity"
Cohesion: 0.11
Nodes (12): ClickOperation, Kind, PICKUP, PICKUP_ALL, Cell, ContentKey, InventoryClickPlanner, InventorySortPlanner (+4 more)

### Community 1 - "BundleFeature.java"
Cohesion: 0.08
Nodes (21): ClientModInitializer, Gson, Logger, ModInitializer, ModConfig, BundleFeature, Minecraft, MouseButtonEvent (+13 more)

### Community 2 - ".tick"
Cohesion: 0.09
Nodes (14): DataComponentType, ItemStack, Slot, InventoryClickExecutor, AbstractContainerMenu, Minecraft, InventorySortController, Minecraft (+6 more)

### Community 3 - "BundleScreen"
Cohesion: 0.13
Nodes (11): KeyEvent, Screen, BundleScreen, AbstractContainerMenu, BundleContents, GuiGraphicsExtractor, Identifier, Inventory (+3 more)

### Community 4 - "BundleInteractionExecutor"
Cohesion: 0.17
Nodes (13): ContainerInput, BundleInteractionExecutor, AbstractContainerMenu, Inventory, Minecraft, BundleClickStep, BundleInteractionPlanner, BundleSelectionStep (+5 more)

### Community 5 - "CompassHudRenderer"
Cohesion: 0.19
Nodes (15): Camera, DeltaTracker, Font, GameRenderer, HudElement, Level, LocalPlayer, PartialTickSupplier (+7 more)

### Community 7 - "InventoryOperationLock"
Cohesion: 0.26
Nodes (4): AfterEach, InventoryOperationLock, InventoryOperationLockTest, Test

### Community 8 - "AbstractContainerScreenAccessor"
Cohesion: 0.53
Nodes (3): Accessor, AbstractContainerScreenAccessor, Mixin

### Community 9 - "ExampleClientMixin.java"
Cohesion: 0.53
Nodes (4): ExampleClientMixin, CallbackInfo, Inject, Mixin

### Community 10 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

## Knowledge Gaps
- **2 isolated node(s):** `PICKUP`, `PICKUP_ALL`
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SortSlot` connect `ItemIdentity` to `.tick`, `.plan`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **Why does `ItemIdentity` connect `ItemIdentity` to `.tick`, `.plan`?**
  _High betweenness centrality (0.138) - this node is a cross-community bridge._
- **Why does `BundleScreen` connect `BundleScreen` to `BundleFeature.java`, `BundleInteractionExecutor`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **What connects `PICKUP`, `PICKUP_ALL` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ItemIdentity` be split into smaller, more focused modules?**
  _Cohesion score 0.1125886524822695 - nodes in this community are weakly interconnected._
- **Should `BundleFeature.java` be split into smaller, more focused modules?**
  _Cohesion score 0.07751937984496124 - nodes in this community are weakly interconnected._
- **Should `.tick` be split into smaller, more focused modules?**
  _Cohesion score 0.09446693657219973 - nodes in this community are weakly interconnected._