# Features

Utils Mod is a client-only Fabric mod for Minecraft 26.2. It does not need to be installed on a
server and does not send private waypoint data to servers or other players.

## Compass HUD

The HUD is a compact compass strip at the top of the screen. It follows the player's interpolated
look direction and displays cardinal directions, degree markings, heading ticks, and waypoint markers.
Vanilla locator markers and private local waypoint markers can be controlled independently.

The HUD is disabled outside a loaded world. Server-provided locator markers only appear when the
server actually provides them; a solo singleplayer world has no other player marker to display.

Unlike vanilla's own locator bar (above the hotbar, which vanilla temporarily replaces with the XP bar
for 5 seconds every time the player gains experience), the compass HUD strip at the top shows locator
markers unconditionally whenever `compassVanillaWaypointMarkersEnabled` is on — it never hides other
players just because you picked up XP. Vanilla's own bottom bar and its XP-progress display are left
untouched so the normal "XP needed until next level" view still works as expected.

## Local waypoints

Local waypoints are stored in `config/compass-hud.json` and remain private to the client. Profiles are
separated per multiplayer server and per singleplayer save. Profile identifiers are hashed so server
addresses and local save paths are not exposed in the configuration. Markers are additionally filtered
by dimension.

Commands:

```text
/compasshud waypoint add <name>
/compasshud waypoint addcolor <RRGGBB|preset> <name>
/compasshud waypoint addat <x> <y> <z> <name>
/compasshud waypoint list
/compasshud waypoint remove <name>
/compasshud waypoint show <name>
/compasshud waypoint hide <name>
/compasshud waypoint color <name> <RRGGBB|preset>
/compasshud waypoint rename <old-name> <new-name>

/compasshud setting hud <on|off>
/compasshud setting local <on|off>
/compasshud setting vanilla <on|off>
```

`addcolor` creates a coloured marker. If the name already exists in the active profile, it changes
only that marker's colour and preserves its position, dimension, and visibility. The `color` command
recolours an existing marker directly.

The ten case-insensitive presets are `red`, `orange`, `yellow`, `lime`, `green`, `cyan`, `blue`,
`purple`, `pink`, and `white`. Six-digit RGB values are also accepted, with optional `#` or `0x`
prefixes, for example `FF8800`, `#FF8800`, or `0xFF8800`. Colour completion shows each preset with a
coloured swatch and its hex value.

Waypoint names may contain spaces and should be quoted when needed:

```text
/compasshud waypoint addcolor cyan "Old Base"
/compasshud waypoint color "Old Base" FF8800
/compasshud waypoint rename "Old Base" "New Base"
```

Local waypoint markers use an outlined map-pin diamond so they remain distinct from vanilla player or
locator markers. HUD, vanilla markers, local markers, and individual waypoint visibility are separate.
Death markers, teleportation, coordinate conversion, and shared/server-synchronised waypoints are not
included.

## Inventory sorting

The rebindable **Sort Inventory** key defaults to `R` (`Options → Controls → Inventory`). It works only
while a chest or double chest is open, and only sorts that chest's own storage grid. The sorter uses
normal vanilla container clicks, validates the menu, cursor, and slot state before every action, and
stops safely if the screen or server state changes.

The player's main inventory and hotbar are never sorted, even while a chest is open — only the chest's
slots are touched. The player inventory screen (no container open), the custom Bundle view, hoppers,
dispensers/droppers, shulker boxes, and every other menu (crafting, furnace, trading, anvil, enchanting,
creative inventory, ...) are all left untouched; pressing the key there does nothing (or reports the
menu as unsupported).

Sorting is client-triggered but server-authoritative; no item stacks are edited directly.

## Improved Bundle UI

Shift + Right Click a Bundle in the player's own inventory or hotbar to open the virtual Bundle screen.
It displays Bundle contents in a paged grid and renders the real player inventory below it. Normal and
Shift extraction, insertion, remainder handling, tooltips, stack decorations, and mouse-wheel paging
use vanilla Bundle rules and synchronized menu state.

Left-clicking or plain right-clicking a Bundle entry takes out the whole merged stack shown in that
slot (matching vanilla's own Bundle behaviour, which has no concept of a partial amount). Shift + Right
Click instead takes just one item onto the cursor, leaving the rest in the Bundle, and can be clicked
repeatedly on the same entry to keep adding one more to the held stack each time — this needs one free
inventory/hotbar slot as scratch space (vanilla's own Bundle extraction only fires with an empty
cursor, so repeated clicks park the held stack, extract normally, top it up by one, and pick it back
up) and is silently ignored if the inventory is completely full or the stack is already maxed out.

Clicking anywhere outside every slot (Bundle grid or player inventory/hotbar) while carrying an item
throws it into the world, matching vanilla's click-outside-to-drop behaviour (left click throws the
whole stack, right click throws one item).

The Bundle UI is client-side and does not create a custom server menu. External-container Bundles,
creative manipulation, nested workflows, drag-distributing one stack across multiple slots, mass
filling, Bundle-content sorting, and a separate configurable open key are intentionally unsupported.

## Configuration

All settings use `config/compass-hud.json`:

| Key | Default | Purpose |
| --- | --- | --- |
| `compassHudEnabled` | `true` | Show the compass HUD. |
| `compassVanillaWaypointMarkersEnabled` | `true` | Show server-provided locator markers. |
| `localWaypointMarkersEnabled` | `true` | Show local waypoint markers. |
| `maxVisibleLocalWaypointMarkers` | `8` | Maximum nearest local markers considered (`1`–`32`). |
| `inventorySortEnabled` | `true` | Enable inventory sorting (chests only). |
| `clickDelayTicks` | `1` | Minimum ticks between sorting clicks. |
| `bundleUiEnabled` | `true` | Enable the improved Bundle screen. |
| `bundleUiShiftRightClick` | `true` | Enable Shift + Right Click Bundle opening. |

## Requirements and limitations

- Minecraft 26.2, Fabric Loader 0.19.3+, Fabric API, and Java 25.
- No server installation, plugin, operator permission, teleport command, minimap, or data pack is
  required.
- Individual servers or anti-cheat systems may reject automated inventory timing; the operation then
  stops instead of forcing further clicks.
- Death markers and automatic Nether/Overworld coordinate conversion are deferred/not included.
