#!/usr/bin/env python3
import json, sys, os, subprocess

# Read stdin so the pipe closes cleanly; the contents are not needed.
try:
    json.load(sys.stdin)
except (json.JSONDecodeError, ValueError):
    pass

root = os.environ.get("CLAUDE_PROJECT_DIR", ".")
graph = os.path.join(root, "graphify-out", "graph.json")
manifest = os.path.join(root, "graphify-out", "manifest.json")

# Only add context when a graph exists.
if not os.path.exists(graph):
    sys.exit(0)

# manifest.json is rewritten on every `graphify update` run, even when the
# graph's topology is unchanged — unlike graph.json, so it's the right
# "last checked" reference point.
last_checked = os.path.getmtime(manifest) if os.path.exists(manifest) else os.path.getmtime(graph)


def graph_is_stale():
    """True if the last commit or an uncommitted change is newer than the last graphify run."""
    try:
        last_commit = subprocess.run(
            ["git", "-C", root, "log", "-1", "--format=%ct"],
            capture_output=True, text=True, timeout=5,
        )
        if last_commit.returncode == 0 and last_commit.stdout.strip():
            if int(last_commit.stdout.strip()) > last_checked:
                return True

        status = subprocess.run(
            ["git", "-C", root, "status", "--porcelain"],
            capture_output=True, text=True, timeout=5,
        )
        for line in status.stdout.splitlines():
            path = line[3:].strip().strip('"')
            if path.startswith("graphify-out/"):
                continue  # the graph's own output, not a source change
            full = os.path.join(root, path)
            if os.path.exists(full) and os.path.getmtime(full) > last_checked:
                return True
    except (subprocess.SubprocessError, OSError, ValueError):
        pass
    return False


hint = ("graphify-out/ is available — for questions about architecture, "
        "file relationships, or call graphs, use `graphify query \"...\"`, "
        "`graphify explain \"...\"`, or `graphify path \"A\" \"B\"`. "
        "Do not read/search graphify-out/ directly; it is too large/token-heavy.")

if graph_is_stale():
    hint += (" Note: the graph looks older than recent source changes — "
             "consider running `graphify update .` before relying on it.")

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "UserPromptSubmit",
        "additionalContext": hint
    }
}))
sys.exit(0)