# Building a Release Jar & Publishing (CurseForge)

## 1. Bump the version

Edit `gradle.properties`:

```properties
mod_version=1.0.0
```

`build.gradle`'s `processResources` block expands `${version}` into `fabric.mod.json` at build time, and Loom names the output jar `utils-mod-<version>.jar` (the `base { archivesName = "utils-mod" }` in `build.gradle`, independent of the `compass-hud` mod id) — so this one line controls both the mod's reported version and the jar filename. Bump it before every release (e.g. `1.0.1`, `1.1.0` — [semantic versioning](https://semver.org/) is the usual convention).

## 2. Build

```bash
./gradlew.bat build   # Windows
./gradlew build        # macOS/Linux
```

This produces two files in `build/libs/`:

- `utils-mod-<version>.jar` — the file to upload as the release. Contains the remapped, ready-to-run mod.
- `utils-mod-<version>-sources.jar` — optional companion; some platforms let you attach it as a "sources" file for other developers, but it isn't required for players.

Run `./gradlew clean build` if you want a fully fresh build (clears cached/up-to-date task outputs first).

## 3. Pre-publish checklist

- [ ] Tested the exact built jar (`./gradlew runClient` covers the dev environment; for extra confidence, drop the built jar into a real `.minecraft/mods/` folder with matching Fabric Loader + Fabric API and confirm it loads).
- [ ] `src/main/resources/fabric.mod.json` fields are accurate and up to date: `version` (auto-filled), `name`, `description`, `authors`, `license`, `contact.homepage`/`contact.sources`.
  - `contact.sources` points at the project's repository: `https://github.com/jsevenheck/utils-mod`.
  - `license` is `CC0-1.0`. Confirm that this is the intended license before publishing; CurseForge asks you to declare a matching project license too.
- [ ] `depends` in `fabric.mod.json` matches what you actually require: `fabricloader >= 0.19.3`, `minecraft ~26.2`, `java >= 25`, `fabric-api *`. Tighten `fabric-api`'s version range if you rely on APIs added in a specific release.
- [ ] `icon.png` (`src/main/resources/assets/compass-hud/icon.png`) looks right — this is what shows up as the project/file thumbnail.
- [ ] No leftover debug logging or test code.
- [ ] Bundle UI smoke-tested: Shift + Right Click opening, arbitrary extraction, Shift-extraction, insertion, remainder handling, close/reopen synchronization, pressing the inventory-sort key (`R`) while the Bundle view is open, and sorter-operation exclusion.
- [ ] Compass/local-waypoint smoke-tested: add/list/rename/hide/show/remove commands, persistence after reconnect, dimension filtering, per-world/per-server separation, and HUD visibility toggles.
- [ ] README, `docs/features.md`, `docs/local-development.md`, and `docs/CURSEFORGE_DESCRIPTION.md` describe the current feature set and its limitations.

## 4. Publishing to CurseForge

1. Create a CurseForge account and, if you haven't already, a project: **Create Project → Minecraft → Mod**.
2. Project setup (one-time):
   - Name, summary, description (Markdown/BBCode editor), license, and category (e.g. "HUD / Minimap").
   - Add the icon and a couple of screenshots (an in-game shot of the compass HUD is ideal here).
3. Upload a file: **Files → Upload a File**.
   - Upload `utils-mod-<version>.jar` (not the sources jar).
   - **Game Versions**: select `26.2` (and the Fabric mod loader entry — CurseForge lists "Fabric" as a loader alongside the Minecraft version).
   - **Release Type**: Release / Beta / Alpha as appropriate.
   - **Relations → Required Dependency**: add **Fabric API**, since this mod depends on it (`fabric-api` in `fabric.mod.json`). Players won't get prompted to install it otherwise.
   - Changelog: what changed since the last version.
4. Publish. CurseForge review/indexing is usually near-instant for updates to an existing project; new projects can take longer for the initial review.

### Optional: automating uploads

For repeated releases (e.g. from CI), CurseForge has a REST upload API and a matching Gradle plugin, [`net.darkhax.curseforgegradle`](https://github.com/Darkhax/CurseForgeGradle). It needs a CurseForge API token (generate one in your CurseForge account settings) supplied via an environment variable/secret — never commit it to the repo. Not set up in this project; add it only if manual uploads become a bottleneck.

## Other platforms

The same jar works on [Modrinth](https://modrinth.com/) — the upload flow is nearly identical (create project → upload file → select game version `26.2` + loader `Fabric` → add Fabric API as a dependency). Modrinth also has a first-party Gradle publish plugin (`com.modrinth.minotaur`) if you want to automate both platforms together later.
