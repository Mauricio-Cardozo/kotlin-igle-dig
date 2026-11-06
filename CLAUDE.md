@AGENTS.md

## Claude Code

- `AGENTS.md` is the single source of truth for this project; edit it there, not here.
- A Stop hook (`.claude/hooks/compile-check.sh`) runs `./gradlew compileDebugKotlin` at the end of each turn when `.kt`/`.kts` files changed since the last successful check. If it reports errors, fix them before finishing.
