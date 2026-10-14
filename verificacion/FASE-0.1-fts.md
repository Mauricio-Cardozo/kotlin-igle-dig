# Fase 0.1 — FTS de búsqueda bíblica

## Hallazgo (verificado en dispositivo)

La SQLite del framework Android **no incluye el módulo FTS5**. El plan original asumía que
el único problema era el guard de `user_version` en `ensureFtsIndex`, pero aunque se
corrija, `CREATE VIRTUAL TABLE ... USING fts5(...)` falla igual.

Sonda ejecutada en el emulador (`emulator-5554`, android-34) con `SQLiteDatabase` del
framework (`app/src/androidTest/java/com/example/FtsProbeInstrumentedTest.kt`, luego borrada):

```text
SQLITE_VERSION=3.39.2
MODULE_fts5=FAIL SQLiteException: no such module: fts5 (code 1 SQLITE_ERROR)
MODULE_fts4=OK count=1
MODULE_fts3=OK count=1
```

## Por qué el RN sí usa FTS5

`expo-sqlite` trae su propia SQLite compilada con FTS5
(`node_modules/expo-sqlite/android/src/main/cpp/libsql/.../sqlite3.c`,
`-DSQLITE_ENABLE_FTS5`), y por eso `db/init.ts` puede crear tablas `fts5(...)`.
La app Kotlin usa `SQLiteOpenHelper` (SQLite del framework), que no trae FTS5.

## Estado anterior (impacto real)

`ensureFtsIndex` usaba el guard `currentVersion < 1`, pero el asset trae
`PRAGMA user_version = 10` → nunca entraba. En la práctica la búsqueda **siempre** cayó al
respaldo `LIKE` (no era un bug funcional; era falta de índice y de relevancia).

## Decisión

Usar **FTS4** (disponible en la SQLite del framework, API 26+) para `biblia_pueblo_dios` y
`biblia_navarra`, con contenido externo (`content='<tabla>'`) y `rebuild`. FTS4 no tiene la
columna `rank` de FTS5, así que los resultados se ordenan por `id`. Se mantiene el respaldo
`LIKE`.

Validación previa de sintaxis FTS4 sobre una copia temporal del asset:

```text
A: JOIN biblia_pueblo_dios v ON v.id=f.rowid ... ORDER BY v.id  -> (33028,'Romanos',13,8) (33030,'Romanos',13,10)
user_version tras rebuild: 10
```

## Cambios

- `data/db/IglesiaDatabaseHelper.kt`: `ensureFtsIndex` verifica la existencia de cada tabla
  FTS en `sqlite_master` (no `user_version`) y crea `fts4(...)` faltantes para pueblo y
  navarra. Ya **no** escribe `PRAGMA user_version`.
- `data/repository/BibliaRepository.kt`: la consulta de búsqueda usa
  `JOIN biblia_pueblo_dios_fts f ON v.id = f.rowid` + `WHERE ..._fts MATCH ?` +
  `ORDER BY v.id LIMIT 50` (antes `ORDER BY f.rank`, que FTS4 no soporta).

## Verificación

- JVM (Robolectric): `BibliaSearchTest` pasa (`search` devuelve resultados, no rompe).
  Nota: la SQLite de Robolectric tampoco trae FTS, así que en JVM el camino real es LIKE;
  la creación del índice se verifica en dispositivo.
- Instrumentado (emulador android-34, `emulator-5554`): `BibliaFtsInstrumentedTest` (pueblo + navarra).

```text
> Starting 2 tests on igle(AVD) - 14
> Finished 2 tests on igle(AVD) - 14
BUILD SUCCESSFUL in 27s

<testsuite name="com.example.BibliaFtsInstrumentedTest" tests="2" failures="0" errors="0" skipped="0" time="1.728">
  <testcase name="initializeDatabase_buildsBibleFtsTables_andKeepsUserVersion" time="1.726" />
  <testcase name="searchVersiculos_usesFts_andReturnsResults" time="0.002" />
```

Conclusión: en el dispositivo real, `initializeDatabase()` crea `biblia_pueblo_dios_fts` y
`biblia_navarra_fts` (FTS4), `PRAGMA user_version` sigue en 10 y `searchVersiculos` devuelve
resultados. Fase 0.1 verificada.
