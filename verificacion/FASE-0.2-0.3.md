# Fases 0.2 y 0.3 — Selector de Biblia + Evangelio / Santo / Calendario

Rama: `feature/fase-0-quick-wins` (sin commits; se pide confirmación antes de commitear).

## Fase 0.2 — Selector Pueblo de Dios / Navarra

### Objetivo
Replicar del RN la posibilidad de elegir entre las dos ediciones de la Biblia
(`biblia_pueblo_dios` y `biblia_navarra`), persistir la elección y versionar los
favoritos de versículos para no mezclar ids entre ediciones.

### Cambios
- `data/db/BibliaModels.kt`: nuevo enum `BibliaVersion` (`PUEBLO`, `NAVARRA`) con
  `key`, `tableName`, `label`, `ftsTableName` y `favoritoPrefix` (`biblia-<key>-`).
- `data/db/IglesiaDatabaseHelper.kt`: las tablas FTS se derivan de `BibliaVersion`
  (`FTS_TABLES = BibliaVersion.entries.map { ftsTableName to tableName }`).
- `data/repository/BibliaRepository.kt`: `getLibros`, `getCapitulos`, `getVersiculos`,
  `searchVersiculos` y `toggleFavorito` aceptan `version: BibliaVersion = PUEBLO`.
  La búsqueda usa la tabla FTS de la versión elegida.
- `data/user/UserPreferencesRepository.kt`: preferencia `biblia_version` (DataStore)
  con `bibliaVersion: Flow<String>` y `setBibliaVersion()`.
- `data/user/UserDatabase.kt`: versión Room 1 → 2 con `MIGRATION_1_2`
  (`UPDATE favoritos SET id = 'biblia-pueblo-' || substr(id, 8)
  WHERE tipo = 'biblia' AND id GLOB 'biblia-[0-9]*'`).
- `ui/biblia/BibliaViewModel.kt`: estado `bibliaVersion`, observa la preferencia y
  recarga libros/búsqueda al cambiar; pasa la versión a todas las llamadas.
- `ui/biblia/LibrosScreen.kt`: selector de versión (chips "Pueblo de Dios" / "Navarra")
  y subtítulo dinámico.
- `ui/biblia/LecturaScreen.kt`: la clave de favorito usa `version.favoritoPrefix`.

### Decisiones
- Ids de favorito versionados (`biblia-pueblo-<id>`) como el RN; los ids antiguos
  (`biblia-<id>`) se migran a Pueblo.
- FTS4 (no FTS5) por limitación de SQLite del framework Android (ver FASE-0.1).

### Verificación JVM (Robolectric)
```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL

com.example.BibliaVersionTest            tests=3 fail=0 err=0
com.example.FavoritoMigrationTest        tests=1 fail=0 err=0
com.example.LiturgiaRepositoryTest       tests=7 fail=0 err=0
... (resto sin fallos)
```
- `BibliaVersionTest`: resolución de `fromKey`, nombres de tabla y prefijos.
- `FavoritoMigrationTest`: la migración convierte `biblia-1234` → `biblia-pueblo-1234`
  y no toca ids ya versionados ni otros tipos.

### Verificación en dispositivo
`BibliaVersionInstrumentedTest` (emulador android-34):
- `navarra_hasBooksAndSearchResults`: Navarra lista libros y devuelve resultados.
- `toggleFavorito_createsVersionedIds`: los favoritos se guardan como
  `biblia-pueblo-<id>` / `biblia-navarra-<id>`.

Resultado (run instrumentado completo):
```
Starting 8 tests on igle(AVD) - 14
Finished 8 tests on igle(AVD) - 14
BUILD SUCCESSFUL in 1m 48s

[OK] navarra_hasBooksAndSearchResults
[OK] toggleFavorito_createsVersionedIds
```

## Fase 0.3 — Evangelio del Día, detalle del Santo y Calendario

### Cambios
- `data/db/LiturgiaModels.kt`: modelos `MisalSantosEntry` y `DiaLectura`.
- `data/repository/LiturgiaRepository.kt`: `getMisalSantosDelDia(mes,dia)` y
  `getLecturasDelMes(ano,mes)` (+ `mapMisalSantos`).
- `ui/liturgia/LiturgiaViewModel.kt`: estados `EvangelioUiState`, `SantoDetalleUiState`,
  `CalendarioUiState`; métodos `loadEvangelio`, `loadSantoDetalle`, `loadCalendario`,
  `mesAnterior`, `mesSiguiente`, `selectDiaCalendario`.
- `ui/liturgia/EvangelioScreen.kt`: lecturas del día (o de una fecha), con secciones.
- `ui/liturgia/SantoDetalleScreen.kt`: biografía + Propio del santo (`misal_santos`).
- `ui/liturgia/CalendarioScreen.kt`: grilla mensual (domingo primero) que marca los días
  con Evangelio y muestra santos + Propio del día; tocar un día navega al Evangelio.
- `ui/navigation/NavigationDestinations.kt`: ruta `evangelio?fecha={fecha}` con helper
  `evangelioRoute(fecha)`.
- `ui/navigation/MainScreen.kt`: se reemplazan los `ComingSoonScreen` de Evangelio, Santo
  (detalle) y Calendario. `Misal propio` queda diferido (decisión previa).

### Verificación JVM (Robolectric, asset real)
- `LiturgiaRepositoryTest` ampliado:
  - `testLecturasDelMesOctubre2026`: 31 lecturas, `2026-10-01`..`2026-10-31`,
    el 11/10 es "XXVIII Domingo ordinario" con evangelio.
  - `testMisalSantosDelDia`: el 4/10 devuelve San Francisco de Asís con colecta;
    un día sin propio devuelve lista vacía.

```
com.example.LiturgiaRepositoryTest       tests=7 fail=0 err=0
```

### Verificación en dispositivo
- `LiturgiaExtraInstrumentedTest`: `getLecturasDelMes(2026,10)` = 31; el 4/10 es Francisco.
- `MainFlowUiTest`: arranca la app, abre la pestaña Calendario, toca un día con lectura
  y navega al Evangelio del Día.

Resultado (mismo run instrumentado):
```
Starting 8 tests on igle(AVD) - 14
Finished 8 tests on igle(AVD) - 14
BUILD SUCCESSFUL in 1m 48s

[OK] lecturasDelMes_octubre2026_tiene31Dias
[OK] misalSantosDelDia_4deOctubre_esSanFrancisco
[OK] calendarioTab_showsGrid_andTappingDayWithReading_opensEvangelio  (22.8s)
```

## Pendientes / notas
- `Misal propio` (Propio del Tiempo) queda para una fase posterior.
- Falta decisión del usuario sobre commitear los 2 tests preexistentes modificados
  (`LiturgiaRepositoryTest.kt`, `LiturgiaViewModelTest.kt`) y confirmar el commit `87829d0`.
