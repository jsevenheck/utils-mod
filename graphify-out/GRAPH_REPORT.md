# Graph Report - utils-mod  (2026-08-05)

## Corpus Check
- 22 files · ~7,374 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 176 nodes · 427 edges · 10 communities (9 shown, 1 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 14 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ee37321e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ModFeature
- .tick
- .plan
- ItemIdentity
- InventoryClickPlanner
- CompassHudRenderer
- SortSlot
- ClickOperation
- ExampleClientMixin.java
- ExampleMixin.java

## God Nodes (most connected - your core abstractions)
1. `ItemIdentity` - 25 edges
2. `SortSlot` - 23 edges
3. `ClickOperation` - 20 edges
4. `InventorySortPlannerTest` - 16 edges
5. `CompassHudRenderer` - 15 edges
6. `InventoryClickPlannerTest` - 14 edges
7. `InventoryClickPlanner` - 13 edges
8. `InventoryClickExecutor` - 11 edges
9. `Cell` - 11 edges
10. `ModConfig` - 10 edges

## Surprising Connections (you probably didn't know these)
- `InventorySortController` --references--> `InventoryClickExecutor`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortController.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventoryClickExecutor.java
- `InventorySortFeature` --references--> `InventorySortController`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortFeature.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortController.java
- `SortSession` --references--> `SortSlot`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/SortSession.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/SortSlot.java
- `ClickOperation` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ClickOperation.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java
- `ContentKey` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventoryClickPlanner.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java

## Import Cycles
- None detected.

## Communities (10 total, 1 thin omitted)

### Community 0 - "ModFeature"
Cohesion: 0.12
Nodes (13): ClientModInitializer, Logger, ModInitializer, CompassHudFeature, Override, FeatureRegistry, InventorySortFeature, ModFeature (+5 more)

### Community 1 - ".tick"
Cohesion: 0.16
Nodes (6): Gson, ModConfig, InventorySortController, Minecraft, Override, InventorySortPlanner

### Community 3 - "ItemIdentity"
Cohesion: 0.12
Nodes (9): DataComponentType, ItemStack, Slot, ItemIdentities, Minecraft, SortableSlotResolver, AbstractContainerMenu, SortSession (+1 more)

### Community 4 - "InventoryClickPlanner"
Cohesion: 0.38
Nodes (3): Cell, ContentKey, InventoryClickPlanner

### Community 5 - "CompassHudRenderer"
Cohesion: 0.19
Nodes (15): Camera, DeltaTracker, Font, GameRenderer, GuiGraphicsExtractor, HudElement, Level, LocalPlayer (+7 more)

### Community 6 - "SortSlot"
Cohesion: 0.32
Nodes (3): SortSlot, InventoryClickPlannerTest, Test

### Community 7 - "ClickOperation"
Cohesion: 0.24
Nodes (7): InventoryClickExecutor, AbstractContainerMenu, Minecraft, ClickOperation, Kind, PICKUP, PICKUP_ALL

### Community 8 - "ExampleClientMixin.java"
Cohesion: 0.53
Nodes (4): ExampleClientMixin, CallbackInfo, Inject, Mixin

### Community 9 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

## Knowledge Gaps
- **2 isolated node(s):** `PICKUP`, `PICKUP_ALL`
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SortSlot` connect `SortSlot` to `.tick`, `.plan`, `ItemIdentity`, `InventoryClickPlanner`?**
  _High betweenness centrality (0.240) - this node is a cross-community bridge._
- **Why does `ItemIdentity` connect `ItemIdentity` to `.tick`, `.plan`, `InventoryClickPlanner`, `SortSlot`, `ClickOperation`?**
  _High betweenness centrality (0.180) - this node is a cross-community bridge._
- **Why does `ModConfig` connect `.tick` to `ModFeature`?**
  _High betweenness centrality (0.142) - this node is a cross-community bridge._
- **What connects `PICKUP`, `PICKUP_ALL` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ModFeature` be split into smaller, more focused modules?**
  _Cohesion score 0.11692307692307692 - nodes in this community are weakly interconnected._
- **Should `ItemIdentity` be split into smaller, more focused modules?**
  _Cohesion score 0.1225071225071225 - nodes in this community are weakly interconnected._