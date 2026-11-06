#!/usr/bin/env bash
# Stop hook: when the agent finishes a turn, compiles Kotlin if any .kt/.kts
# file changed since the last successful check. Exit 2 sends the compiler
# errors back to the agent so it fixes them before stopping.
set -uo pipefail

input=$(cat)
# Avoid loops: if we already blocked once in this stop cycle, let it stop.
[ "$(jq -r '.stop_hook_active // false' <<<"$input")" = "true" ] && exit 0

repo="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
cd "$repo" || exit 0

state_file="$(git rev-parse --git-dir)/claude-kotlin-compile.hash"
current=$( {
  git diff HEAD -- '*.kt' '*.kts'
  git ls-files --others --exclude-standard -- '*.kt' '*.kts' | xargs -r cat
} | sha1sum | cut -d' ' -f1 )

[ -f "$state_file" ] && [ "$(cat "$state_file")" = "$current" ] && exit 0

if output=$(./gradlew compileDebugKotlin --quiet 2>&1); then
  echo "$current" > "$state_file"
  exit 0
fi

echo "La compilación de Kotlin falló (./gradlew compileDebugKotlin):" >&2
errors=$(grep -E '^e: ' <<<"$output" | head -40)
if [ -n "$errors" ]; then echo "$errors" >&2; else tail -40 <<<"$output" >&2; fi
exit 2
