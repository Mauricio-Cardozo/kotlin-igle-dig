# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Full architecture, design decisions, and migration-phase status live in **AGENTS.md**. Read it before making non-trivial changes.

---

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew clean

# Tests
./gradlew testDebugUnitTest                     # JUnit + Robolectric (no device needed)
./gradlew connectedDebugAndroidTest             # Instrumented (requires device/emulator)
./gradlew lintDebug

# Single test class
./gradlew testDebugUnitTest --tests com.example.SeasonsTest

# Single test method
./gradlew testDebugUnitTest --tests com.example.SeasonsTest.romanToInt_convertsRomanNumerals

# Single instrumented class
./gradlew connectedDebugAndroidTest --tests com.example.BibliaFtsInstrumentedTest
```

CI runs `testDebugUnitTest` on every push/PR to `main` (`.github/workflows/ci.yml`).

---

## Architecture Overview

**Iglesia Digital** is a 100 % offline Catholic Android app (Kotlin + Jetpack Compose). It is a native port of a React Native reference app at `/home/mauricionc/Documentos/bible-app/AppMovil`.

### Three-layer persistence

| Layer | Tech | Purpose |
|---|---|---|
| Static content | `IglesiaDatabaseHelper` (raw SQLite) | 17 MB read-only asset (`iglesia_digital.db`): Bible ×2 versions, Missal, Leccionario, Saints, FTS4 indexes |
| User data | Room (`UserDatabase`) | `Favorito`, `Tag` — reactive via `Flow<List<T>>` |
| Settings | DataStore Preferences | `biblia_version`, `font_size_multiplier` |

### Dependency injection

Manual DI: `DefaultAppContainer` (singleton) in `IglesiaApplication`. ViewModels receive dependencies through `ViewModelProvider.Factory`.

### Navigation

Jetpack Compose `NavHost` with 4 bottom-nav tabs: **Liturgia** (home), **Biblia**, **Calendario**, **Oración**. Route parameters with spaces/accents must use `Uri.encode`/`Uri.decode` (e.g. "1 Corintios", "Génesis"). See `NavigationDestinations.kt`.

### Key patterns

- `StateFlow<UiState>` (sealed Success/Loading/Error) in every ViewModel; Composables receive state, not ViewModels (except root screens).
- All I/O strictly on `Dispatchers.IO` via `withContext`.
- FTS4 (not FTS5 — Android's bundled SQLite lacks FTS5). Tokenizer: `unicode61 "remove_diacritics=1"`. Index recreation detected via `sqlite_master`, never `PRAGMA user_version` (the asset already carries `user_version = 10`).
- Press feedback via `Modifier.pressable` (opacity), not Material 3 ripple.
- Package namespace is still `com.example`; `applicationId` is `com.iglesiadigital.app`. Renaming is deferred (Phase 1 remainder).

---

## Commit Convention

Conventional Commits in Spanish:

```
<tipo>(<ámbito>): <resumen imperativo, ≤72 chars>

Por qué: <razón del cambio>

- <cambio concreto>
```

**Tipos:** `feat` `fix` `refactor` `perf` `docs` `test` `chore` `ci` `delete` `redesign`  
**Ámbitos:** `biblia` `liturgia` `misal` `oracion` `catecismo` `ui` `db` `data` `nav` `build`

Each commit must compile and leave all tests green. Schema changes (Room/SQLite) go in their own commit.

---

## Important Constraints

- Do not accept licences, install software, or modify anything outside this repository without explicit permission.
- If a required tool (SDK, JDK, emulator) is missing, stop and report what is needed — never fabricate directories or fake SDK folders.
- When bumping the bundled database asset, increment `CURRENT_CONTENT_VERSION` in `IglesiaDatabaseHelper.kt` to force re-copy on existing installs; do **not** touch `user_version`.
