import type { ExtensionAPI } from '@earendil-works/pi-coding-agent';
import { resolve, relative, sep } from 'node:path';

// ─── Hilfsfunktionen ──────────────────────────────────────────────────────────

function matches(input: string, patterns: RegExp[]): boolean {
  return patterns.some((re) => re.test(input));
}

function normalizePath(path: string): string {
  return path.replace(/\\/g, '/');
}

function isInsideRoot(absPath: string, rootDir: string): boolean {
  // Härtet den relative()-Check der Vorlage ab: funktioniert auch auf Windows,
  // wo relative() bei anderen Laufwerken (C:\ vs. D:\) kein '..'-Präfix liefert.
  return absPath === rootDir || absPath.startsWith(rootDir + sep);
}

// ─── Datei-Richtlinien ────────────────────────────────────────────────────────

// Lesen grundsätzlich verboten: sensible Credentials & interne Metadaten.
// Kein node_modules/- und kein .log-Muster: dieses Projekt ist ein Fabric-Mod
// ohne node_modules, und Minecraft-Logs (run/logs/, crash-reports/) werden zum
// Debuggen benötigt.
const READ_DENY = [
  /(?:^|\/)\.env(\.[^/]*)?$/, // .env*
  /(?:^|\/)\.git\//,
  /(?:^|\/)graphify-out(?:\/|$)/, // Graph-artefakt nie direkt lesen
  /\.pem$/,
  /\.key$/,
  /\.p12$/,
  /\.pfx$/,
  /\.crt$/,
  /\.cer$/,
];

// Schreiben/Editieren grundsätzlich verboten: Credentials & eigene Policy-Dateien.
// .env.example ist hier bewusst NICHT ausgenommen — identisch zu .claude/settings.json.
const WRITE_DENY = [
  /(?:^|\/)\.env(\.[^/]*)?$/,
  /(?:^|\/)\.claude\/settings\.json$/, // eigene Agent-Settings
  /(?:^|\/)\.pi\/extensions\/policy\.ts$/, // eigene Policy (Selbstschutz)
  /\.pem$/,
  /\.key$/,
  /\.p12$/,
  /\.pfx$/,
  /\.crt$/,
  /\.cer$/,
];

// Nur für das write-Tool (Edits bleiben erlaubt) — spiegelt die Write-Denies
// der .claude/settings.json: kritische Build-/Mod-Konfigurationen dürfen
// gezielt angepasst, aber nie blind überschrieben werden.
const WRITE_DENY_WRITE_ONLY = [
  /(?:^|\/)fabric\.mod\.json$/,
  /(?:^|\/)build\.gradle(?:\.kts)?$/,
  /(?:^|\/)settings\.gradle(?:\.kts)?$/,
  /(?:^|\/)gradle\.properties$/,
  /(?:^|\/)gradle\/wrapper\//,
  /(?:^|\/)gradlew(?:\.bat)?$/,
];

// Schreiben/Editieren verboten nach Dateityp: Binärdateien & kompilierte
// Artefakte. Alles andere ist erlaubt (Open-World-Ansatz statt Whitelist).
// Mobile-Build-Artefakte (.apk/.aab/.ipa/.dSYM) wurden entfernt (kein Flutter/Android hier).
const WRITE_DENY_TYPES = [
  // Binäre / kompilierte Artefakte
  /\.exe$/,
  /\.dll$/,
  /\.so$/,
  /\.dylib$/,
  /\.class$/,
  /\.jar$/,
  /\.war$/,
  /\.ear$/,
  /\.pyc$/,
  /\.pyd$/,
  /\.wasm$/,

  // Medien & Archive (kein sinnvoller Text-Edit)
  /\.zip$/,
  /\.tar$/,
  /\.gz$/,
  /\.bz2$/,
  /\.7z$/,
  /\.rar$/,
  /\.png$/,
  /\.jpe?g$/,
  /\.gif$/,
  /\.webp$/,
  /\.svg$/, // SVG ist XML — bei Bedarf diese Zeile entfernen
  /\.ico$/,
  /\.mp3$/,
  /\.mp4$/,
  /\.mov$/,
  /\.avi$/,
  /\.pdf$/,
  /\.ttf$/,
  /\.woff2?$/,
  /\.eot$/,
];

// ─── Bash-Richtlinien ─────────────────────────────────────────────────────────

// Hart verboten — unabhängig vom Rest (deckt sich mit den Denies der settings.json)
const BASH_DENY = [
  // Destruktive Git-Operationen
  /^git\s+add\b/,
  /^git\s+commit\b/,
  /^git\s+push\b/,
  /^git\s+merge\b/,
  /^git\s+rebase\b/,
  /^git\s+reset\b/,
  /^git\s+clean\b/,
  /^git\s+checkout\s+--/, // Datei-Discard

  // Destruktive Filesystem-Ops
  /\brm\s+.*-[a-z]*r[a-z]*f\b/, // rm -rf und Varianten
  /\brm\s+.*-[a-z]*f[a-z]*r\b/,
  /^sudo\b/,
  /^su\b/,

  // Netzwerk-Exfiltration / Tunneling
  /\bcurl\b.*\|\s*(ba)?sh/,
  /\bwget\b.*\|\s*(ba)?sh/,
  /\bngrok\b/,
  /\bssh\s+-R\b/, // Reverse-Tunnel

  // Gradle: Publish & Wrapper-Regenerierung blockiert
  // (maven-publish ist konfiguriert; `wrapper` würde gradle/wrapper/** neu
  // schreiben, das per WRITE_DENY_WRITE_ONLY gesperrt ist)
  /^\.\/gradlew\s+publish\b/,
  /^\.\/gradlew\s+wrapper\b/,

  // .env-Inhalt via Shell-Tools lesen blockieren
  /\b(cat|less|more|grep|awk|sed)\b[^|]*\.env\b/,
];

// Sichere Bash-Compound-Commands.
// Wichtig: NICHT pauschal /^if\b/ erlauben, sondern nur konkrete sichere Formen.
const BASH_ALLOW_COMPOUND = [
  // if [ -f graphify-out/graph.json ]; then echo yes; else echo no; fi
  /^if\s+\[\s+-f\s+graphify-out\/graph\.json\s+\]\s*;\s*then\s+echo\s+(?:"yes"|'yes'|yes)\s*;\s*else\s+echo\s+(?:"no"|'no'|no)\s*;\s*fi$/,

  // if [ -f graphify-out/graph.json ]; then graphify query "..."; else echo "NO_GRAPH"; fi
  //
  // Erlaubt einfache/sichere Query-Strings in '...' oder "...".
  // Blockiert bewusst Shell-Expansionen wie $, Backticks und unquoted Queries.
  /^if\s+\[\s+-f\s+graphify-out\/graph\.json\s+\]\s*;\s*then\s+graphify\s+query\s+(?:"[^"`$\\]*(?:\\.[^"`$\\]*)*"|'[^'`$\\]*(?:\\.[^'`$\\]*)*')\s*;\s*else\s+echo\s+(?:"NO_GRAPH"|'NO_GRAPH'|NO_GRAPH)\s*;\s*fi$/,
  /^test\s+-f\s+graphify-out\/graph\.json\s*&&\s*echo\s+exists\s*\|\|\s*echo\s+missing\s*&&\s*pwd\s*&&\s*find\s+\.\s+-maxdepth\s+3\s+-type\s+f\s*\|\s*sed\s+'s#\^\.\/##'\s*\|\s*head\s+-80$/,
];

// Erlaubt — exakt die Subcommands der .claude/settings.json + generische
// Werkzeuge (Shell/Inspektion), die für die Arbeit am Mod nötig sind.
const BASH_ALLOW = [
  // Gradle Wrapper (Subcommand-Allowlist wie in .claude/settings.json).
  // Achtung: bewusst drei getrennte Muster — Subcommands kommen ohne Strich
  // (build, runClient, …), die Zusatzoptionen mit Strich (--stop, --version).
  /^\.\/gradlew\s*$/, // nackter Aufruf = Hilfe
  /^\.\/gradlew\s+(?:build|runClient|runServer|genSources|clean|check|help|projects|tasks|properties|dependencies)\b/,
  /^\.\/gradlew\s+--(?:stop|version)\b/,

  // Git (nur read-only — deckt sich mit .claude/settings.json)
  /^git\s+status\b/,
  /^git\s+diff\b/,
  /^git\s+log\b/,

  // Shell-Werkzeuge (Suche, Inspektion, Text)
  /^grep\b/,
  /^rg\b/, // ripgrep
  /^find\b/,
  /^ls\b/,
  /^ll\b/,
  /^cat\b/,
  /^head\b/,
  /^tail\b/,
  /^less\b/,
  /^wc\b/,
  /^sort\b/,
  /^uniq\b/,
  /^cut\b/,
  /^awk\b/,
  /^sed\b/,
  /^jq\b/,
  /^yq\b/,
  /^echo\b/,
  /^printf\b/,
  /^env\b/,
  /^printenv\b/,
  /^which\b/,
  /^whereis\b/,
  /^type\b/,
  /^pwd\b/,
  /^cd\b/,
  /^mkdir\b/,
  /^cp\b/,
  /^mv\b/,
  /^touch\b/,
  /^diff\b/,
  /^patch\b/,

  // Python (u.a. für den graphify_hint-Hook & JSON-Validierung)
  /^python3?\b/,

  // Graphify — die Befehle aus der CLAUDE.md (Graphify-Sektion)
  /^graphify\s+update\b/,
  /^graphify\s+query\b/,
  /^graphify\s+path\b/,
  /^graphify\s+explain\b/,
];

// ─── Extension ────────────────────────────────────────────────────────────────

export default function (pi: ExtensionAPI) {
  pi.on('tool_call', async (event, ctx) => {
    const { toolName, input } = event;
    const projectRoot = resolve(ctx.cwd);

    // ── Datei-Tools ──────────────────────────────────────────────────────────
    if (toolName === 'read' || toolName === 'write' || toolName === 'edit') {
      const rawPath = String(input?.path ?? '');
      if (!rawPath) return;

      const absPath = resolve(projectRoot, rawPath);
      const relPath = normalizePath(relative(projectRoot, absPath));

      // Pfad muss innerhalb des Projekts liegen
      if (!isInsideRoot(absPath, projectRoot)) {
        return {
          block: true,
          reason: `${toolName} außerhalb des Projektverzeichnisses blockiert: ${absPath} (root: ${projectRoot})`,
        };
      }

      // Lesen: sensible Dateien blockieren
      if (toolName === 'read' && matches(relPath, READ_DENY)) {
        return { block: true, reason: `Read blocked (protected): ${relPath}` };
      }

      // Schreiben / Editieren
      if (toolName === 'write' || toolName === 'edit') {
        // Credentials & eigene Policy-Dateien → hart blockieren
        if (matches(relPath, WRITE_DENY)) {
          return {
            block: true,
            reason: `Write/Edit blocked (protected file): ${relPath}`,
          };
        }

        // Nur write-Tool: kritische Build-/Mod-Konfigurationen blockieren
        // (Edits bleiben erlaubt — wie in .claude/settings.json)
        if (toolName === 'write' && matches(relPath, WRITE_DENY_WRITE_ONLY)) {
          return {
            block: true,
            reason: `Write blocked (write-protected config): ${relPath}`,
          };
        }

        // Binärdateien & Medien → blockieren
        if (matches(relPath, WRITE_DENY_TYPES)) {
          return {
            block: true,
            reason: `Write/Edit blocked (binary/media file): ${relPath}`,
          };
        }

        // Alles andere: erlaubt (Open-World)
      }
    }

    // ── Bash-Tool ─────────────────────────────────────────────────────────────
    if (toolName === 'bash') {
      const command = String(input?.command ?? '').trim();
      if (!command) return;

      // Deny hat immer Vorrang
      if (matches(command, BASH_DENY)) {
        return { block: true, reason: `Bash blockiert (Denylist): ${command}` };
      }

      // Sichere Compound-Commands wie:
      // if [ -f graphify-out/graph.json ]; then ...; else ...; fi
      if (matches(command, BASH_ALLOW_COMPOUND)) {
        return; // durchlassen
      }

      // Danach normale Allowlist prüfen
      if (!matches(command, BASH_ALLOW)) {
        return {
          block: true,
          reason: `Bash nicht in der Allowlist: ${command}`,
        };
      }
    }
  });
}
