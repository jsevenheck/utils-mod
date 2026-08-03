# Graph Report - compass-hud-26.2  (2026-08-03)

## Corpus Check
- 5 files · ~1,013 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 42 nodes · 79 edges · 9 communities (4 shown, 5 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 1 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- CompassHUDClient.java
- CompassHUD
- CompassHudRenderer
- GuiGraphicsExtractor
- compasshud/client/mixin/ExampleClientMixin.java
- CompassHudRenderer.java
- compasshud/mixin/ExampleMixin.java
- .extractRenderState
- .onInitialize

## God Nodes (most connected - your core abstractions)
1. `CompassHudRenderer` - 13 edges
2. `CompassHUD` - 6 edges
3. `CompassHUDClient` - 3 edges
4. `ExampleClientMixin` - 3 edges
5. `ExampleMixin` - 3 edges

## Surprising Connections (you probably didn't know these)
- `CompassHudRenderer` --implements--> `HudElement`  [EXTRACTED]
  src/client/java/io/github/jsevenheck/compasshud/client/CompassHudRenderer.java →   _Bridges community 2 → community 5_

## Import Cycles
- None detected.

## Communities (9 total, 5 thin omitted)

### Community 0 - "CompassHUDClient.java"
Cohesion: 0.50
Nodes (3): ClientModInitializer, CompassHUDClient, Override

### Community 1 - "CompassHUD"
Cohesion: 0.53
Nodes (4): Identifier, Logger, ModInitializer, CompassHUD

### Community 4 - "compasshud/client/mixin/ExampleClientMixin.java"
Cohesion: 0.53
Nodes (4): ExampleClientMixin, CallbackInfo, Inject, Mixin

### Community 6 - "compasshud/mixin/ExampleMixin.java"
Cohesion: 0.53
Nodes (4): ExampleMixin, CallbackInfo, Inject, Mixin

## Knowledge Gaps
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CompassHudRenderer` connect `CompassHudRenderer` to `CompassHUDClient.java`, `GuiGraphicsExtractor`, `CompassHudRenderer.java`, `.extractRenderState`?**
  _High betweenness centrality (0.299) - this node is a cross-community bridge._
- **Why does `CompassHUD` connect `CompassHUD` to `CompassHUDClient.java`, `.onInitialize`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._