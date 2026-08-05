# Utils Mod

A client-side utility mod for Minecraft 26.2, built on Fabric. It currently bundles three independent
features under one mod (`compass-hud`):

- **Compass HUD** — a horizontally scrolling compass strip at the top-center of the screen, showing
  heading ticks, N/NE/E/SE/S/SW/W/NW labels, degree numbers, and dots for nearby players/waypoints.
  Unchanged from earlier versions of this mod — only its source location moved (see
  [Architecture](#architecture) below).
- **Inventory Sort** — a keybind that sorts your inventory and, when a supported container is open,
  the container's storage slots too.
- **Improved Bundle UI** — a chest-like client screen for viewing and moving items in a Bundle.

All three features are purely client-side. The mod never needs to be installed on a server, and it never
requires anything from the server beyond normal vanilla container interaction.

The mod also includes an **Improved Bundle UI**. With the player inventory open, Shift + Right Click
a Bundle in your own inventory or hotbar to open a chest-like view of all its contents.

## Inventory Sort

Press the **Sort Inventory** key (default: `R`, rebindable in *Options → Controls → Inventory*) while a
supported inventory screen is open. Nothing happens if you press it anywhere else (main menu, while not
looking at an inventory, etc.) — it's a safe no-op.

### Supported

- Your own inventory screen (main inventory slots only; the hotbar is intentionally excluded).
- The player-inventory row shown at the bottom of a container screen.
- Standard chest-like storage: chests (including double chests), hoppers, dispensers/droppers, and
  shulker boxes.

### Not supported (left untouched)

Armor slots, the offhand slot, crafting-grid input/output, furnace/smoker/blast-furnace fuel and
result slots, villager trading slots, anvil/smithing/enchanting/stonecutter/loom/cartography/beacon
slots, creative-mode inventory, and any other menu this mod doesn't specifically recognize. Opening one
of those and pressing the key either does nothing to those slots or shows a small "this menu doesn't
support sorting" message, depending on whether *any* part of the open screen is sortable.

### How sorting works (and why it's safe)

Minecraft is server-authoritative: the client can't just rearrange items locally. Sorting instead plans
a queue of ordinary container interactions (the same click primitives vanilla screens use) and plays
them back a few ticks apart, checking before every interaction that the slot and cursor still contain
what the plan expects. For safe groups of several small stacks, it uses vanilla's collect-to-cursor
interaction to reduce the number of visible moves. This fast path is disabled whenever the same item
exists outside the section being sorted, because vanilla's collect operation searches the complete
open menu. Groups that do not fit into one cursor stack use the conservative pickup/merge flow. If
the screen closes, the container changes, the server corrects something unexpectedly, or your cursor
state differs from the plan, the sort aborts immediately and sends no further clicks. It does not try
to force a recovery click in an untrusted menu state; after an abort, check the cursor before continuing.
You'll see a message if sorting can't start (menu not supported, cursor not empty) or gets aborted
partway through; if your inventory is already sorted, nothing visibly happens.

### Configuration

A config file is created at `config/compass-hud.json` on first run:

| Key | Default | Meaning |
| --- | --- | --- |
| `inventorySortEnabled` | `true` | Turn the whole feature off. |
| `sortSectionsIndependently` | `true` | If `true`, your inventory and an open container are sorted separately, so items never move between the two. If `false`, both are sorted as one combined pool. |
| `clickDelayTicks` | `1` | Minimum ticks per queued click; clamped to a minimum of `1`. |
| `bundleUiEnabled` | `true` | Turn the improved Bundle screen off. |
| `bundleUiShiftRightClick` | `true` | Enable the default Bundle shortcut. |

## Improved Bundle UI

The Bundle screen is fully client-side and does not create a custom server menu, so no server
installation is required. It reads the synchronized Bundle component from the real player inventory
and sends only vanilla Bundle-selection packets and normal server-authoritative inventory clicks.

The upper panel shows every Bundle entry, with mouse-wheel paging for large Bundles. Click an entry to
extract it to the cursor, or Shift-click to distribute it into compatible inventory stacks and empty
slots. Click a player-inventory stack to insert it; unsupported items and Bundle capacity are handled
by vanilla's Bundle APIs, and any remainder is returned to its source slot. The Bundle slot, menu,
cursor, and expected state are validated before every queued interaction. Sorting and Bundle operations
share a lock and cannot run concurrently.

Supported: survival player inventory, main inventory and hotbar Bundles, all vanilla Bundle colors,
viewing all contents, extraction, Shift-extraction, insertion, and Shift-insertion. External-container
Bundles, creative manipulation, nested Bundle workflows, drag-and-drop, mass filling, and Bundle
sorting are intentionally not supported. Some multiplayer servers or anti-cheat systems may reject
unusual automated inventory timing; rejected actions are allowed to resolve to the server state.

## Documentation

- [Local development (VS Code)](docs/local-development.md) — building, running, and debugging the mod.
- [Publishing](docs/publishing.md) — building a release jar and publishing to CurseForge.

## Architecture

Both features live under one Gradle project / mod id, split into `feature` packages:

```text
src/main/java/io/github/jsevenheck/utilsmod/
  feature/inventorysort/     <- pure, Minecraft-independent sort planning logic (unit-tested)
src/client/java/io/github/jsevenheck/utilsmod/client/
  UtilsModClient.java        <- client entrypoint, initializes every feature via FeatureRegistry
  config/ModConfig.java
  feature/FeatureRegistry.java
  feature/compass/           <- Compass HUD rendering
  feature/inventorysort/     <- keybind, slot resolution, click execution/orchestration
  feature/bundle/            <- virtual Bundle UI over the real player InventoryMenu
```

The inventory-sort planning algorithm (what goes where, and which sequence of clicks gets there) is
implemented with no dependency on Minecraft classes and is covered by unit tests in `src/test`.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This project is licensed under CC0-1.0.
