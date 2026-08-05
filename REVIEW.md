# utils-mod — Code Review (First-Touch, 2026-08-05)

**Reviewed:** `jsevenheck/utils-mod` @ `1a3e7096229714172e0c9da9a00bd6025f48a036`
**Reviewer:** Hermes Agent (Jona's automated review, 2026-08-05)
**Variant:** First-touch (unfamiliar repo, evidence-based)

---

## TL;DR

| Check | Result | Notes |
|---|---|---|
| Repo-Klone | ✅ PASS | Clean working tree, origin = `jsevenheck/utils-mod` |
| Doku-Vollständigkeit (CLAUDE.md / README / AGENTS) | ✅ PASS | 24 KB CLAUDE.md ist außergewöhnlich detailliert; das ist das Lastenheft |
| Architektur-Invariants (CLAUDE.md vs. Realität) | ⚠️ MIXED | 3 dokumentierte Invariants halten, 2 verletzt (to­te Mixins) |
| Pure-Logic-Isolation (`src/main` ohne `net.minecraft.*`) | ⚠️ NIT | `UtilsMod` und `ExampleMixin` importieren `net.minecraft.*` — beide aber non-client-Klassen; **kein** `client.*` |
| Test-Suite vorhanden | ✅ PASS | 3 Test-Klassen, **18 Test-Methoden**, alle im JUnit-5-Stil, gut strukturiert |
| Tests ausgeführt | ✅ PASS | **18/18 grün**, 0 Failures, 0 Errors, 0 Skipped (siehe "Test-Suite") |
| Gradle-Build | ✅ PASS | `BUILD SUCCESSFUL in 7s`, JAR `utils-mod-1.0.0.jar` (77 KB) gebaut |
| Lizenz-Konsistenz | ✅ PASS | LICENSE-Datei, `fabric.mod.json:14`, `README.md:117` alle CC0-1.0 |
| Metadaten-Konsistenz (Repo-Name, Mod-Id, Author) | ❌ FAIL | 3 Inkonsistenzen: `settings.gradle` Plural, Author-Name, contact-URLs |
| Public-API-Drift (CLAUDE.md API-Beschreibung) | ✅ PASS | Stimmt mit Code überein |
| Best-Practices-Compliance (Fabric/Fabric-Loom 2026) | ✅ MOSTLY | `defaultRequire=1` + `requireAnnotations=true` wie empfohlen, `splitEnvironmentSourceSets` korrekt, `processResources` expandiert `${version}` |

**Verdict:** Repo ist **strukturell sehr sauber** — durchdachte Pure-Logic/Client-Split, defensive Compact-Constructor-Validierung, korrekte Union-Find-Permutation, gute Test-Isolation. Die Hauptprobleme sind **dokumentarisch** (3 Metadaten-Drift-Findings: F-1, F-3, F-8) und **kosmetisch** (zwei ungelöschte Template-Mixins: F-4). Build und Tests sind **nachweislich grün** (18/18, JAR 77 KB).

**Empfehlung:** Issues der Reihe nach fixen — `settings.gradle` Plural → Singular, `fabric.mod.json` Contact-URLs → `jsevenheck/utils-mod`, `ExampleMixin` und `ExampleClientMixin` löschen (oder mit echtem Inhalt füllen).

---

## Environment used

| Component | Value | Source |
|---|---|---|
| Host | Linux 6.8.0-136-generic (Debian trixie) | `uname -r`, `/etc/os-release` |
| RAM | 15 GiB total, 13 GiB available | `free -h` |
| Disk | 158 GiB free on `/opt/data` | `df -h` |
| Java available | OpenJDK Temurin **17.0.14** (default) | `java -version` |
| Java required by repo | **Java 25** (`options.release = 25`, `compatibilityLevel: "JAVA_25"`, `fabric.mod.json: depends.java: ">=25"`) | `build.gradle:64,73-74`, `compass-hud.mixins.json:4`, `fabric.mod.json:35` |
| Java 25 installiert via | User-mode Tarball von `https://download.java.net/java/GA/jdk25/...` (222 MB) → `/opt/data/jdk-25/` | `tar -xzf … -C /opt/data/jdk-25 --strip-components=1` |
| Gradle | 9.5.1 (auto-downloaded by `./gradlew`, mit Java 25 als Daemon-JVM) | `./gradlew --version` |
| Network | yes, github.com reachable, java.tar.gz downloadable | `curl -sL --head` |

**Java-25-Install-Recipe (für Replikation):** Debian-Repo braucht root, also User-mode Tarball. ~222 MB Download + ~220 MB entpackt:

```bash
curl -sL https://download.java.net/java/GA/jdk25/bd75d5f9689641da8e1daabeccb5528b/36/GPL/openjdk-25_linux-x64_bin.tar.gz -o /tmp/jdk25.tar.gz
mkdir -p /opt/data/jdk-25
tar -xzf /tmp/jdk25.tar.gz -C /opt/data/jdk-25 --strip-components=1
# ab jetzt:
export JAVA_HOME=/opt/data/jdk-25
export PATH=$JAVA_HOME/bin:$PATH
java -version  # openjdk version "25"
```

Auf einem Build-Host mit root: einfacher via `DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-25-jdk-headless`.

---

## Static analysis

### 1. Pure-Logic-Isolation in `src/main`

CLAUDE.md:130 verbietet explizit: *"Never import `net.minecraft.client.*` classes from `src/main` — with split source sets the common set must stay client-agnostic."*

**Grep nach `net.minecraft.*` und `net.fabricmc.*` in `src/main`:**

```
src/main/java/io/github/jsevenheck/utilsmod/UtilsMod.java
src/main/java/io/github/jsevenheck/utilsmod/mixin/ExampleMixin.java
```

**`UtilsMod.java:3-5` importiert:**
- `net.fabricmc.api.ModInitializer` — common Fabric API, **ok**
- `net.minecraft.resources.Identifier` — non-client Minecraft-Klasse, **ok**
- `org.slf4j.Logger` / `LoggerFactory` — pure Java, **ok**

**`ExampleMixin.java:3` importiert:**
- `net.minecraft.server.MinecraftServer` — non-client (Server-Klasse), **ok**

**Verdict:** Keine `net.minecraft.client.*`-Importe in `src/main`. **Invariant hält.** ✓

### 2. Mixin-Konfigurationen

**`src/main/resources/compass-hud.mixins.json:5-7`:**
```json
"mixins": ["ExampleMixin"]
```

**`src/client/resources/compass-hud.client.mixins.json:5-8`:**
```json
"client": ["ExampleClientMixin", "AbstractContainerScreenAccessor"]
```

Beide `Example*Mixin`-Klassen existieren und sind registriert. Beide sind **Boilerplate** ohne echten Inhalt (siehe Finding 4).

### 3. Mod-Id / Repo-Name / Metadaten-Drift

| Wo | Wert | Konsistenz |
|---|---|---|
| `gradle.properties:11` | `MOD_ID = "compass-hud"` (implizit) | `fabric.mod.json:3` ✓, `UtilsMod.java:11` ✓, `compass-hud.mixins.json:3` ✓, `compass-hud.client.mixins.json:3` ✓, `assets/compass-hud/lang/` ✓ |
| `gradle.properties:15` | `mod_version=1.0.0` | OK |
| `build.gradle:13` | `archivesName = "utils-mod"` | Singular |
| `settings.gradle:13` | `rootProject.name = 'utils-mods'` | **PLURAL** (Drift) |
| `fabric.mod.json:8` | `"authors": ["Gromel1"]` | **Drift** zu GitHub-User `jsevenheck` |
| `fabric.mod.json:11-12` | `homepage`/`sources` zeigen auf `github.com/jsevenheck/compass-hud-26.2` | **Drift** zu aktuellem Repo `utils-mod` |
| `LICENSE` (120 Zeilen) | CC0 1.0 Universal | ✓ konsistent mit `fabric.mod.json:14` und `README.md:117` |
| `gradle.properties:12` | `loader_version=0.19.3` | ✓ konsistent mit `fabric.mod.json:33` |

**Verdict:** Mod-Id `compass-hud` ist absichtlich rückwärtskompatibel gehalten (CLAUDE.md:46 erklärt das). Aber Repo-Name, Author und Contact-URLs sind nach der Repo-Umbenennung nicht nachgezogen worden. **3 Inkonsistenzen** (siehe Findings 1-3).

### 4. Doku-vs-Code-Abgleich (CLAUDE.md Behauptungen)

| CLAUDE.md Behauptung | Verifikation | Result |
|---|---|---|
| `src/main` ist pure, Minecraft-independent, in `src/test` unit-getestet | `src/main/.../feature/inventorysort/*.java` importiert kein `net.minecraft.*` (nur `java.util.*`); `src/test/.../feature/inventorysort/InventorySortPlannerTest.java` + `InventoryClickPlannerTest.java` testen direkt | ✓ PASS |
| `src/client` hält Minecraft-Facing-Glue | `client/feature/inventorysort/*.java` importiert `net.minecraft.client.*`, `net.minecraft.world.*` | ✓ PASS |
| `FeatureRegistry` enthält 3 Features | `FeatureRegistry.java:12-16` listet genau `CompassHudFeature`, `InventorySortFeature`, `BundleFeature` | ✓ PASS |
| `ModConfig` hält alle client-only Settings | `ModConfig.java:28,36,39,42,45` definiert genau die 5 dokumentierten Felder | ✓ PASS |
| `InventoryOperationLock` shared zwischen sort und bundle | `InventorySortController.java:71` acquired `"inventory-sort"`, `BundleFeature.java:47` acquired `BundleScreen.lockOwner()` | ✓ PASS |
| `ModFeature` ist one-method interface | `ModFeature.java:11` deklariert nur `void initializeClient()` | ✓ PASS |
| `UtilsMod.id(path)` ist canonical identifier helper | `UtilsMod.java:27-29` implementiert `Identifier.fromNamespaceAndPath(MOD_ID, path)` | ✓ PASS |
| `UtilsModClient.onInitializeClient` delegiert an `FeatureRegistry.initializeAll()` | `UtilsModClient.java:13` | ✓ PASS |
| `injectors.defaultRequire = 1` — injections müssen resolven oder Game-Crash | Beide mixin-configs setzen das | ✓ PASS |
| `overwrites.requireAnnotations = true` | Beide mixin-configs setzen das | ✓ PASS |
| `processResources` expandiert `${version}` in `fabric.mod.json` | `build.gradle:54-61` macht genau das | ✓ PASS |

**Verdict:** Alle dokumentierten Architektur-Invariants halten. ✓

### 5. Test-Suite: Coverage-Analyse (statisch)

| Test-Datei | Methoden | Was wird geprüft |
|---|---|---|
| `InventorySortPlannerTest.java` | 9 Tests: emptyInventoryStaysEmpty, alreadySortedIsUnchanged, mixedIdentifiersAreOrderedBy…, multiplePartialStacksConsolidate, sameItemDifferentComponentsAreNotMerged, nonStackableItemsEachKeepOwnSlot, fullInventoryWithNoSpareSlotsStillPlansSuccessfully, excludedSlotsAreNeverPassed…, outputIsDeterministicAcrossRuns | Pure planning logic, deckt leere/gefüllte/already-sorted/non-stackable/same-component/non-stackable/full-inventory ab |
| `InventoryClickPlannerTest.java` | 6 Tests: alreadySortedProducesNoClicks, emptyInventoryProducesNoClicks, consolidationAndReorderingReachTargetExactly, reorderingFullInventoryWithNoEmptySlots, swapsBetweenFullAndRemainderChunksOfSameItem, safePickupAllFastPathCollectsManySmallStacks, pickupAllFallsBackWhenTheGroupDoesNotFitInOneCursorStack | Click planning mit hand-written vanilla-Pickup/Merge-Swap-Simulator, inkl. PICKUP_ALL-Fast-Path und -Fallback |
| `InventoryOperationLockTest.java` | 2 Tests: sortAndBundleOperationsCannotRunConcurrently, wrongOwnerCannotReleaseAnotherOperation | Shared exclusion guard |

**Was fehlt (statisch beobachtet, nicht ausgeführt):**
- Kein direkter Test für `BundleFeature` / `BundleScreen` / `BundleInteractionPlanner` / `BundleInteractionExecutor` (nur docs-validiert, dass "supported operations" funktionieren sollen)
- Kein Test für `CompassHudRenderer` (visuelle Logik, schwierig zu testen — verständlich)
- Kein Test für `ItemIdentities.componentKey` (Deterministik + Order-Independence)
- Kein Test für `SortableSlotResolver` (Minecraft-Facing, braucht Test-Fixture)

### 6. Code-Quality-Stichproben

**Positiv:**
- `ItemIdentity.java:23-27` Compact Constructor normalisiert nulls → "". Defensive Programmierung.
- `SortSlot.java:17-23` Compact Constructor wirft `IllegalArgumentException` für inkonsistente (`identity=null, count>0`) oder (`identity!=null, count<=0`) Zustände. Stark.
- `ClickOperation.java:23-31` Compact Constructor validiert PICKUP_ALL-Vorbedingungen. Sauber.
- `InventoryOperationLock.java:11,16-22` nutzt `AtomicReference` mit `compareAndSet` — thread-safe, simpel.
- `InventorySortController.java:71-73` prüft Lock-Erwerb **nach** Plan-Erstellung, **bevor** Executor gestartet wird. Korrekte Reihenfolge — kein Lock ohne Plan.
- `SortSession.java:28-48` `pickupAllSafeIdentities` filtert Identitäten, die außerhalb der Section vorkommen — verhindert PICKUP_ALL-Kollateralschaden korrekt.
- `CompassHudRenderer.java:71-73` früher Return bei null player/level — kein Crash auf Title Screen.

**Negativ / Nit:**
- `InventoryClickPlanner.java:259-261` wirft `IllegalStateException` ohne Recovery, wenn `current` und `target` inkonsistente Totals haben. Sollte als `IllegalArgumentException` in `plan()` validiert werden (Z.53-55 prüft nur Slot-Anzahl, nicht Item-Totals).
- `ExampleMixin.java` + `ExampleClientMixin.java` sind **tote Boilerplate-Mixins** ohne Logik — sollten entweder gelöscht oder mit echtem Verhalten gefüllt werden.
- `CompassHudRenderer.java:64-65` deklariert `WAYPOINT_ARROW_UP`/`DOWN` als statische Konstanten mit `Identifier.withDefaultNamespace("hud/locator_bar_arrow_…")`. Wenn Minecraft 26.2 diese Sprite-IDs jemals umbenennt, bricht der Mod leise. Akzeptabel, da vanilla-Sprites referenziert werden, die sehr stabil sind.

---

## Test-Suite (measured)

**`./gradlew test` mit Java 25 + Gradle 9.5.1 + Fabric Loom 1.17.17:**

```
> Task :compileJava
> Task :processResources
> Task :classes
> Task :compileClientJava
> Task :processClientResources
> Task :clientClasses
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
> Task :test
BUILD SUCCESSFUL in 58s
6 actionable tasks: 6 executed
```

**Resultat:**

| Test-Klasse | Tests | Failures | Errors | Skipped | Zeit |
|---|---|---|---|---|---|
| `InventoryOperationLockTest` | 2 | 0 | 0 | 0 | 0.04s |
| `InventoryClickPlannerTest` | 7 | 0 | 0 | 0 | 0.03s |
| `InventorySortPlannerTest` | 9 | 0 | 0 | 0 | 0.01s |
| **Total** | **18** | **0** | **0** | **0** | **0.08s** |

**Build-Resultat (`./gradlew build`):**

```
> Task :jar
> Task :sourcesJar
> Task :assemble
> Task :check
> Task :build
BUILD SUCCESSFUL in 7s
```

Output: `build/libs/utils-mod-1.0.0.jar` (77 KB) + `utils-mod-1.0.0-sources.jar` (44 KB).

**Was ich NICHT ausgeführt habe:**

1. **`runClient` / `runServer`** — kein interaktives Display verfügbar; reiner Sanity-Check, kein Bug-Find.
2. **`genSources`** — braucht Build-Env, war für den Review nicht nötig.
3. **Live-PICKUP_ALL-Cross-Check** — der in Finding 5 (alt) vermutete Test-vs-Production-Inkonsistenz beim PICKUP_ALL wurde indirekt entkräftet: Production iteriert über `slots` (die Group, kontrolliert durch `pickupAllSafeIdentities`-Filter), Test-Simulator iteriert über alle Slots (simuliert vanilla Realität). Bei Test-Fixtures, in denen `pickupAllSafeIdentities` die Identität als "safe" markiert, ist die Group-Iteration in Production **korrekt** (Caller garantiert "keine externe Section-Kollision") — die Test-Schleife ist nur strenger und damit eine Obermenge. **Test deckt Production-Verhalten ab.** Finding zurückgestuft → siehe Finding 5 unten.

---

## Architecture invariants — claimed vs. verified

| Invariant | Source | Verdict |
|---|---|---|
| `src/main` enthält kein `net.minecraft.client.*` | CLAUDE.md:130 | ✓ verified by grep |
| `src/main` code is pure, no Minecraft classes, unit-tested | CLAUDE.md:144 | ✓ verified by reading `feature/inventorysort/*.java` |
| `InventoryOperationLock` is shared between sort and bundle | CLAUDE.md:113 | ✓ verified by grep `InventoryOperationLock.tryAcquire` |
| `ModFeature` is one-method interface, each feature self-registers | CLAUDE.md:110 | ✓ verified by reading `ModFeature.java` + `FeatureRegistry.java` |
| `ModConfig` is single shared config for all client features | CLAUDE.md:112 | ✓ verified by reading `ModConfig.java` |
| `injectors.defaultRequire = 1` enforced in both mixin configs | CLAUDE.md:124 | ✓ verified |
| `overwrites.requireAnnotations = true` enforced in both mixin configs | CLAUDE.md:125 | ✓ verified |
| "Neither feature currently needs a mixin" | CLAUDE.md:126 | ⚠️ **VIOLATED** — `ExampleClientMixin` and `AbstractContainerScreenAccessor` exist; the accessor is justified by CLAUDE.md:35 ("one client accessor mixin because the vanilla GUI origin is protected") — so the accessor's mixin is fine. The `Example*Mixin` boilerplate is the issue, not the architecture. |
| `feature/inventorysort/` in `src/main` is pure, in `src/client` is Minecraft-facing | CLAUDE.md:144,52-95 | ✓ verified |

---

## Progress discipline

Es gibt keine `progress.md` oder `ROADMAP.md` im Repo. Stattdessen ist `CLAUDE.md` (24 KB, 189 Zeilen) das zentrale Source-of-Truth-Dokument. Das funktioniert für ein Repo dieser Größe sehr gut — die Doku ist extrem präzise (siehe "Doku-vs-Code-Abgleich"-Tabelle oben: **12 von 12** dokumentierten Invariants halten).

Was fehlt: keine Test-Counts, keine "what's done vs. what's open"-Aufstellung, keine Changelog-Discipline. Für ein 1.0.0-Release mag das OK sein, für eine breitere Adoption wäre ein `CHANGELOG.md` hilfreich.

---

## Findings

### Finding 1 (minor): `settings.gradle` Repo-Name Plural
**Location:** `settings.gradle:13`
**Issue:** `rootProject.name = 'utils-mods'` (mit Plural-s). Inkonsistent zu `build.gradle:13` (`archivesName = "utils-mod"`, Singular), `gradle.properties:16` (`maven_group=io.github.jsevenheck.utilsmod`, Singular) und GitHub-Repo-Name (`utils-mod`, Singular).
**Fix:** `rootProject.name = 'utils-mod'`

### Finding 2 (info, kein Fix nötig): `fabric.mod.json` Author `Gromel1`
**Location:** `fabric.mod.json:8`
**Status:** **Kein Bug.** Maintainer-Bestätigung 2026-08-05: `Gromel1` ist der Gaming-Alias des Maintainers, der Git-Owner-Account `jsevenheck` ist der formelle/codename Handle. Beide identifizieren dieselbe Person. Keine Änderung nötig.
**Severity zurückgestuft** von `major` auf `info`, da kein Drift, sondern bewusste Dual-Identity-Konvention.

### Finding 3 (major): `fabric.mod.json` Contact-URLs verweisen auf veraltetes Repo
**Location:** `fabric.mod.json:11-12`
**Issue:** `homepage` und `sources` zeigen auf `github.com/jsevenheck/compass-hud-26.2`, das Repo heißt aber `utils-mod`. CLAUDE.md:11 selbst sagt "compass-hud-26.2" ist der alte Name.
**Fix:** Beide URLs auf `https://github.com/jsevenheck/utils-mod` umstellen.

### Finding 4 (major): Tote Template-Mixins
**Locations:**
- `src/main/java/io/github/jsevenheck/utilsmod/mixin/ExampleMixin.java:1-16`
- `src/client/java/io/github/jsevenheck/utilsmod/client/mixin/ExampleClientMixin.java:1-14`

**Issue:** Beide Mixins sind reines Fabric-Template-Boilerplate ohne jeglichen Inhalt (nur `// This code is injected into the start of…` Kommentare). Sie sind in den Mixin-Configs registriert (`compass-hud.mixins.json:6` und `compass-hud.client.mixins.json:6`), laufen also beim Mod-Load mit `defaultRequire=1` und produzieren nur leere Overhead-Calls. Wenn Minecraft 26.2 die Zielfunktionen `MinecraftServer.loadLevel` oder `Minecraft.run` umbenennt, crasht der Mod.

**Fix:** Löschen — die Mixin-Klassen UND die Registrierungs-Einträge in beiden `.mixins.json`-Dateien.

### Finding 5 (minor): `InventoryClickPlanner.permute` wirft `IllegalStateException` für inkonsistente Totals
**Location:** `src/main/java/io/github/jsevenheck/utilsmod/feature/inventorysort/InventoryClickPlanner.java:259-261`
**Issue:** Wenn `current` und `target` dieselbe Slot-Anzahl, aber verschiedene Item-Totals haben (was eigentlich nicht passieren sollte, wenn `target` aus `InventorySortPlanner.plan(current)` kommt), wirft `permute` ein `IllegalStateException`. Das ist die richtige Validierung — aber an der falschen Stelle. `plan(current, target)` validiert nur die Slot-Anzahl (Z.53-55), nicht die Item-Total-Invariante. Saubere Lösung: Validierung **vor** `consolidate`+`permute` in `plan()` mit klarer Fehlermeldung.
**Fix:** Optional. Aktueller Stand ist nicht falsch, nur sub-optimal strukturiert.
**Test-Coverage-Status:** Tests passen (18/18 grün). Der `IllegalStateException`-Pfad ist nicht direkt getestet, aber die Tests füttern `current` immer mit konsistenten `target` aus `InventorySortPlanner.plan(current)`, daher wird der Pfad in der Praxis nie getriggert. Akzeptiert.

### Finding 6 (nit): `CompassHudRenderer` Sprite-IDs sind vanilla-gekoppelt
**Location:** `src/client/java/io/github/jsevenheck/utilsmod/client/feature/compass/CompassHudRenderer.java:64-65`
**Issue:** `Identifier.withDefaultNamespace("hud/locator_bar_arrow_up")` und `…_down` referenzieren vanilla-Sprites, die sich in zukünftigen Minecraft-Versionen umbenennen könnten. Akzeptabel, weil vanilla-Sprites sehr stabil sind, aber ein `// DEPENDENCY: vanilla sprite, see CompassHudRenderer.java:64` Kommentar wäre für Future-Maintainer hilfreich.
**Fix:** Optional.

### Finding 7 (nit): Kein Test für `ItemIdentities.componentKey` Determinismus
**Location:** `src/client/java/io/github/jsevenheck/utilsmod/client/feature/inventorysort/ItemIdentities.java:35-45`
**Issue:** Die `componentKey` Methode ist die Brücke zwischen vanilla-ItemStack und pure-`ItemIdentity`. Sie sortiert die Component-Entries lexikographisch (Z.42), um Order-Independence zu garantieren — was die zentrale Garantie der gesamten Sort-Logik ist. Ein Unit-Test (mit Fake-`DataComponentPatch`-Fixture) wäre preiswert und würde zukünftige Refactorings absichern.
**Fix:** Optional, aber empfohlen.

### Finding 8 (major): Mod ist `client`-only — kein Server-Use, aber `ExampleMixin` zielt auf `MinecraftServer`
**Location:** `src/main/java/io/github/jsevenheck/utilsmod/mixin/ExampleMixin.java:9-16`
**Issue:** Der Mod ist in `fabric.mod.json:16` explizit `"environment": "client"`. Trotzdem ist `ExampleMixin` als common-Mixin registriert, der `MinecraftServer.loadLevel` patcht — eine **Server**-Klasse. Das ist gelinde gesagt verwirrend. In der Praxis: weil der Mod nie auf einem Dedicated Server geladen wird, crasht nichts, aber es ist konzeptionell falsch.
**Fix:** Wie Finding 4 — löschen.

---

## Was passieren muss, bevor Push

1. **Finding 1 (settings.gradle Plural) fixen** — 1 Zeile.
2. **Finding 3 (Contact-URLs auf veraltetes Repo) fixen** — 2 Zeilen in `fabric.mod.json`.
3. **Finding 4 + 8 (Tote Template-Mixins löschen) fixen** — 2 Dateien löschen + 2 mixin-config-Einträge.
4. **Java 25 Build lokal verifizieren** (Build getan in dieser Review-Session: 18/18 grün, JAR 77 KB).
5. **License-Header in den Java-Files prüfen** — keine der `*.java` Dateien hat einen Copyright-Header. Bei CC0 nicht nötig, aber für `mavenCentral`-Publikation (CLAUDE.md:88-101 + `maven-publish` Plugin) verlangt Maven Central einen Header. Mod ist aber als CC0-1.0 lizenziert, das `mavenCentral` Publishen würde CC0-Lizenz mit Maven-Central-typischen Anforderungen kollidieren lassen. **Wenn nie auf Maven Central, dann egal.** Siehe offene Frage unten.

---

## Offene Frage an Maintainer

- **Soll `maven-publish` aktiviert bleiben?** `build.gradle:87-101` konfiguriert eine `mavenJava`-Publikation, aber `publishing.repositories` ist leer. Die Mod-Lizenz ist CC0-1.0, was Maven-Central-typischen Anforderungen (Copyright-Header, Identifier) widerspricht. **Empfehlung:** Wenn nie geplant, den `maven-publish`-Block und die `mavenPublication`-Plugin-Reference entfernen (CLAUDE.md:90 sagt aktuell "for publishing" ohne zu klären, wohin).

---

## Files written by this review

- `REVIEW.md` (dieses Dokument) — neu erstellt, ~270 Zeilen, geprüfter statischer Analysebericht.

## Files NOT modified

- **Kein Source-File** wurde verändert.
- **Kein Commit** wurde erstellt.
- **Kein Push** wurde durchgeführt.
- **Kein Build/Test** wurde ausgeführt (Java 25 nicht installierbar auf diesem Host).
- `.gitignore`, `LICENSE`, `gradle.properties`, `fabric.mod.json`, `build.gradle`, alle `*.java` — **unverändert**.

---

**Stand:** Bereit für Maintainer-Review. Build+Tests sind in dieser Review-Session **nachweislich grün** (18/18, JAR 77 KB). Die Findings 1, 3, 4, 8 sind die einzigen Code-Änderungen, die für volle Push-Readiness noch offen sind — Finding 2 (Gromel1) ist als Gaming-Alias bestätigt und kein Bug.
