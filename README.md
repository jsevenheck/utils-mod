# Utils Mod

A client-side Fabric utility mod for Minecraft 26.2. It combines a compass HUD with local waypoints,
inventory sorting, and an improved Bundle interface. Install it on the **client only**; vanilla and
Fabric servers do not need this mod installed.

## Features

### Compass HUD and local waypoints

The compass HUD is a compact, horizontally scrolling strip at the top-centre of the screen. It shows
heading ticks, cardinal/intercardinal directions, degree labels, and vanilla/server-provided locator
dots for nearby players or other tracked waypoints.

It also supports named, **local-only** waypoints. Waypoints are saved separately for each multiplayer
server and each singleplayer save, and they only appear while you are in their saved dimension. They
are never sent to a server or shared with other players.

Use the following local commands in a world:

```text
/compasshud waypoint add <name>
/compasshud waypoint addcolor <RRGGBB|preset> <name>
/compasshud waypoint addat <x> <y> <z> <name>
/compasshud waypoint list
/compasshud waypoint remove <name>
/compasshud waypoint show <name>
/compasshud waypoint hide <name>
/compasshud waypoint color <name> <RRGGBB>
/compasshud waypoint rename <old-name> <new-name>

/compasshud setting hud <on|off>
/compasshud setting local <on|off>
/compasshud setting vanilla <on|off>
```

`add` saves the current block position; `addat` saves the supplied coordinates in the current
dimension. `addcolor` creates a marker with a colour immediately; for example,
`/compasshud waypoint addcolor red Base`. If that name already exists in the current profile,
`addcolor` changes its colour and keeps its saved position. Local markers use an outlined diamond/map-pin shape,
distinct from vanilla locator icons. Set an existing marker colour with six RGB hex digits, for example
`/compasshud waypoint color Base FF8800`, or use a fixed colour preset: `red`, `orange`, `yellow`,
`lime`, `green`, `cyan`, `blue`, `purple`, `pink`, or `white`. Command completion suggests all ten
presets and shows a coloured swatch/hex preview in its tooltip. For names containing spaces, quote the
name in `color` or `rename`, for example `/compasshud waypoint color "Old Base" FF8800`.

### Inventory Sort

Press the **Sort Inventory** key (default: `R`, rebindable in *Options → Controls → Inventory*) while a
chest or double chest is open. The feature sorts only that chest's own storage grid.

The player's main inventory and hotbar are never sorted, even while a chest is open. The player
inventory screen (no container open), the custom Bundle view, hoppers, dispensers/droppers, shulker
boxes, and every other menu (crafting, furnace, villager, anvil, enchanting, creative, ...) are
deliberately left untouched.

Minecraft is server-authoritative, so the mod uses ordinary vanilla container clicks rather than
editing item stacks locally. It validates the menu, slots, and cursor before every queued action and
stops safely if the server or screen state changes unexpectedly.

### Improved Bundle UI

With the player inventory open, **Shift + Right Click** a Bundle in your own inventory or hotbar to
open a chest-like view of its contents. You can view every entry, extract normally or with Shift-click,
and insert items using normal vanilla Bundle behaviour.

The screen is client-side and does not create a custom server menu. It works through vanilla Bundle
selection and inventory interactions, while checking the synchronized Bundle, menu, and cursor state
before each action.

Supported: survival player inventory, all vanilla Bundle colours, viewing, extraction,
Shift-extraction, insertion, and Shift-insertion. External-container Bundles, creative manipulation,
drag-and-drop, mass filling, nested Bundle workflows, and Bundle sorting are intentionally not
supported.

## Configuration

A config file is created at `config/compass-hud.json` on first run.

| Key | Default | Meaning |
| --- | --- | --- |
| `compassHudEnabled` | `true` | Show the entire compass HUD. |
| `compassVanillaWaypointMarkersEnabled` | `true` | Show server-provided locator dots. |
| `localWaypointMarkersEnabled` | `true` | Show local waypoint dots without deleting them. |
| `maxVisibleLocalWaypointMarkers` | `8` | Nearest local markers considered by the compass (`1`–`32`). |
| `inventorySortEnabled` | `true` | Enable inventory sorting (chests only). |
| `clickDelayTicks` | `1` | Minimum ticks between sort clicks. |
| `bundleUiEnabled` | `true` | Enable the improved Bundle screen. |
| `bundleUiShiftRightClick` | `true` | Enable the default Bundle shortcut. |

## Requirements and installation

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API
- Java 25-compatible Minecraft runtime

Place the mod jar and Fabric API jar in the Minecraft instance's `mods` folder. No server-side jar,
plugin, or command permission is required.

## Documentation

- [Feature documentation](docs/features.md) — detailed behaviour, commands, and limitations.
- [CurseForge project description](docs/CURSEFORGE_DESCRIPTION.md) — ready-to-copy public project text.
- [Local development (VS Code)](docs/local-development.md)
- [Publishing](docs/publishing.md)

## Architecture

The project is split into pure, testable logic in `src/main` and Minecraft-facing client code in
`src/client`. Compass rendering and local waypoint commands live in `feature/compass`; inventory-sort
planning and Bundle interaction planning are covered by JUnit tests.

## License

This project is licensed under CC0-1.0.
