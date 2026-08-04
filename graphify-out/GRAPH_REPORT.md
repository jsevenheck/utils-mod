# Graph Report - compass-hud-26.2  (2026-08-04)

## Corpus Check
- 22 files · ~6,422 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 168 nodes · 395 edges · 10 communities (9 shown, 1 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 12 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5261b97b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ModConfig
- ClickOperation
- ItemIdentity
- SortSlot
- InventoryClickPlanner
- CompassHudRenderer
- InventoryClickPlannerTest
- .of
- ExampleClientMixin.java
- ExampleMixin.java

## God Nodes (most connected - your core abstractions)
1. `SortSlot` - 22 edges
2. `ClickOperation` - 17 edges
3. `ItemIdentity` - 17 edges
4. `InventorySortPlannerTest` - 16 edges
5. `CompassHudRenderer` - 15 edges
6. `ModConfig` - 12 edges
7. `InventoryClickPlanner` - 12 edges
8. `InventoryClickPlannerTest` - 12 edges
9. `InventoryClickExecutor` - 11 edges
10. `Cell` - 10 edges

## Surprising Connections (you probably didn't know these)
- `InventorySortFeature` --references--> `InventorySortController`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortFeature.java → src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/InventorySortController.java
- `ClickOperation` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ClickOperation.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java
- `ContentKey` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventoryClickPlanner.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java
- `Cell` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventoryClickPlanner.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java
- `SortSlot` --references--> `ItemIdentity`  [EXTRACTED]
  src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/SortSlot.java → src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/ItemIdentity.java

## Import Cycles
- None detected.

## Communities (10 total, 1 thin omitted)

### Community 0 - "ModConfig"
Cohesion: 0.09
Nodes (16): ClientModInitializer, Gson, Logger, ModInitializer, ModConfig, CompassHudFeature, Override, FeatureRegistry (+8 more)

### Community 1 - "ClickOperation"
Cohesion: 0.17
Nodes (7): InventoryClickExecutor, AbstractContainerMenu, Minecraft, InventorySortController, Minecraft, ClickOperation, InventorySortPlanner

### Community 2 - "ItemIdentity"
Cohesion: 0.25
Nodes (3): ItemIdentity, InventorySortPlannerTest, Test

### Community 3 - "SortSlot"
Cohesion: 0.21
Nodes (6): Slot, Minecraft, SortableSlotResolver, AbstractContainerMenu, SortSession, SortSlot

### Community 4 - "InventoryClickPlanner"
Cohesion: 0.38
Nodes (3): Cell, ContentKey, InventoryClickPlanner

### Community 5 - "CompassHudRenderer"
Cohesion: 0.19
Nodes (15): Camera, DeltaTracker, Font, GameRenderer, GuiGraphicsExtractor, HudElement, Level, LocalPlayer (+7 more)

### Community 7 - ".of"
Cohesion: 0.43
Nodes (3): DataComponentType, ItemStack, ItemIdentities

### Community 8 - "ExampleClientMixin.java"
Cohesion: 0.53
Nodes (4): ExampleClientMixin, CallbackInfo, Inject, Mixin

### Community 9 - "ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

## Knowledge Gaps
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SortSlot` connect `SortSlot` to `ClickOperation`, `ItemIdentity`, `InventoryClickPlanner`, `InventoryClickPlannerTest`?**
  _High betweenness centrality (0.266) - this node is a cross-community bridge._
- **Why does `ModConfig` connect `ModConfig` to `ClickOperation`, `SortSlot`?**
  _High betweenness centrality (0.194) - this node is a cross-community bridge._
- **Why does `ItemIdentity` connect `ItemIdentity` to `ClickOperation`, `SortSlot`, `InventoryClickPlanner`, `InventoryClickPlannerTest`, `.of`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Should `ModConfig` be split into smaller, more focused modules?**
  _Cohesion score 0.0907563025210084 - nodes in this community are weakly interconnected._