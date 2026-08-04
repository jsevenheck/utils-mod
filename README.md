# Compass HUD

A client-side utility mod for Minecraft 26.2, built on Fabric. It currently bundles two independent
features under one mod (`compass-hud`):

- **Compass HUD** — a horizontally scrolling compass strip at the top-center of the screen, showing
  heading ticks, N/NE/E/SE/S/SW/W/NW labels, degree numbers, and dots for nearby players/waypoints.
  Unchanged from earlier versions of this mod — only its source location moved (see
  [Architecture](#architecture) below).
- **Inventory Sort** — a keybind that sorts your inventory and, when a supported container is open,
  the container's storage slots too.

Both features are purely client-side. The mod never needs to be installed on a server, and it never
requires anything from the server beyond normal vanilla container interaction.

## Inventory Sort

Press the **Sort Inventory** key (default: `R`, rebindable in *Options → Controls → Inventory*) while a
supported inventory screen is open. Nothing happens if you press it anywhere else (main menu, while not
looking at an inventory, etc.) — it's a safe no-op.

### Supported

- Your own inventory screen (main inventory + hotbar).
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
a queue of ordinary container clicks (the same click primitive vanilla screens use) and plays them back
a few ticks apart, checking before every click that the slot still contains what the plan expects. If
the screen closes, the container changes, the server corrects something unexpectedly, or your cursor
ends up holding an item it shouldn't, the sort aborts immediately rather than risking item loss — it
never leaves an item stuck on your cursor. You'll see a message if sorting can't start (menu not
supported, cursor not empty) or gets aborted partway through; if your inventory is already sorted,
nothing visibly happens.

### Configuration

A config file is created at `config/compass-hud.json` on first run:

| Key | Default | Meaning |
| --- | --- | --- |
| `inventorySortEnabled` | `true` | Turn the whole feature off. |
| `sortSectionsIndependently` | `true` | If `true`, your inventory and an open container are sorted separately, so items never move between the two. If `false`, both are sorted as one combined pool. |
| `clickDelayTicks` | `1` | Minimum ticks per queued click; clamped to a minimum of `1`. |

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
```

The inventory-sort planning algorithm (what goes where, and which sequence of clicks gets there) is
implemented with no dependency on Minecraft classes and is covered by unit tests in `src/test`.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
