# Informe de verificación — Iglesia Digital (RN → Kotlin)

Verificación con evidencia de afirmaciones sobre la migración.
- RN: `/home/mauricionc/Documentos/bible-app/AppMovil`
- Kotlin: `/home/mauricionc/kotlin-igle-dig`
- Reglas respetadas: no se aceptaron licencias, no se instaló nada, no se modificó nada fuera de los dos repos, no hubo commits/push. Todo lo generado quedó en `verificacion/` (untracked) o temporal.
- Desviación declarada: A.1 dice usar `db/test-utils.ts`; NO se usó porque usa `__dirname` (incompatible con ESM en Node). Se usó `sql.js` + un loader que stubea solo `expo-sqlite`, importando la función **real** `getSantosDelDia` desde `db/santos.ts` sin modificar el repo RN. La parte Kotlin se corrió con un test Robolectric temporal (creado y **borrado**; el repo quedó limpio).

---

## A. Paridad de santos

Ejecutado: RN real (`db/santos.ts`) y Kotlin real (`LiturgiaRepository.getSantosDelDia` → `SantosDeduplication`) sobre la misma DB, para todos los días de 2024 (bisiesto) y 2026.

```
===== 2024 =====   dias RN: 366  dias Kotlin: 366   IGUALES: 366   DIFERENTES: 0
===== 2026 =====   dias RN: 365  dias Kotlin: 365   IGUALES: 365   DIFERENTES: 0

CASO PUNTUAL 2026-10-12
  RN     : ["Nuestra Señora del Pilar / Nuestra Señora de Aparecida","S. Edisto, mártir en la via Ardeatina","S. Serafín de Montegranaro, laico capuchino"]
  Kotlin : ["Nuestra Señora del Pilar / Nuestra Señora de Aparecida","S. Edisto, mártir en la via Ardeatina","S. Serafín de Montegranaro, laico capuchino"]
```

**Resultado: paridad total (731/731 días, orden y contenido idénticos).**

### Divergencia latente detectada (no manifestada)
`santoKey(nombre)` difiere en el **fallback** cuando el nombre tiene una sola palabra:
- RN: `normalizeKey(nombre)` con el nombre en su caso original. Como `normalizeKey` elimina `[^a-z]` (case-sensitive), la mayúscula inicial se borra → `santoKey("Santiago") = "antiago"`.
- Kotlin: `normalizeKey(nombre.lowercase())` → `santoKey("Santiago") = "santiago"`.

Alcanzabilidad real: **1** santo de una sola palabra en la tabla (`id=103`, `mes=7, dia=25, nombre="Santiago"`).
En la práctica **no cambia el resultado** (2024 y 2026 → `["Santiago","S. Cristóbal, mártir"]` en ambos), porque el grupo se resuelve por separado de todos modos. Queda como diferencia de implementación a decidir por vos.

`sameSaint`, `normalizeKey` (acentos, `ñ`, colapso de dobles) y `levenshtein` son paridad exacta (mismo "bug" portado fielmente).

---

## B. DB y contenido

**B7 — ¿mismo archivo?** `sha256` idéntico:
```
Kotlin: size 17383424  sha256 8f77f2b93e16ce2905f2ef774c12fa063fca8c16af72c8a0acb20464235450b7
RN:     size 17383424  sha256 8f77f2b93e16ce2905f2ef774c12fa063fca8c16af72c8a0acb20464235450b7   IGUALES: true
```

**B5 — tablas y filas** (idénticas en ambos assets):
```
biblia_navarra      35617        catecismo_cic        2865        misal_ordinario     202
biblia_pueblo_dios  35852        leccionario           391        misal_plegarias       4
lecturas              336        misal_guia              0        misal_prefacios      67
santos                889        misal_propio          157        misal_santos        207
novena_dias           162        novenas                18        youcat              526
```
- **"Biblia de Navarra": SÍ.** Prueba: `SELECT name FROM sqlite_master WHERE name LIKE '%navarra%'` → `[{"name":"biblia_navarra"}]`.
- Ojo: la tabla del Catecismo es **`catecismo_cic`** (2.865), no `catecismo`.
- **Gap de port**: el RN usa `biblia_navarra` (y `biblia_navarra_fts`) y ofrece un selector Pueblo de Dios / **Navarra** (`app/(tabs)/biblia.tsx`, `db/biblia.ts`). El Kotlin **no tiene ninguna referencia a `navarra`**; solo usa `biblia_pueblo_dios` y muestra "El Pueblo de Dios" fijo (`LibrosScreen.kt:98`). La versión Navarra está en la DB pero no implementada en la app Kotlin.

**B6 — cobertura de `lecturas`** (DB Kotlin):
```
min/max: 2026-01-01 .. 2026-12-02   count: 336   unicas: 336
existe 2026-03-29: true      existe 2026-04-05: true
faltantes en [2026-01-01..2026-12-02]: (ninguna)
```

---

## C. Valores de aceptación (Home) — salida REAL del código Kotlin

```
===== 2026-10-11 =====
  titulo_misa: XXVIII Domingo ordinario
  detectSeason: ordinario  week: 28  isSunday: true
  propio_id: 284  propio_dia: XXVIII DOMINGO DEL TIEMPO ORDINARIO
  evangelio_ref: Lectura del santo evangelio según san Mateo
  evangelio_prefix: "Mateo 22, 1-10..."

===== 2026-10-12 =====
  titulo_misa: Lunes de la XXVIII semana del Tiempo ordinario
  detectSeason: ordinario  week: 28  isSunday: false
  propio_id: 284  propio_dia: XXVIII DOMINGO DEL TIEMPO ORDINARIO   (quirk: lunes muestra el propio del domingo)
  evangelio_ref: null
  evangelio_prefix: "Lucas 11, 29-32..."

===== 2026-10-31 =====
  titulo_misa: Sábado de la XXX semana del Tiempo ordinario
  detectSeason: ordinario  week: 30  isSunday: false
  propio_id: 286  propio_dia: XXX DOMINGO DEL TIEMPO ORDINARIO
  santos (1): S. Alfonso Rodríguez, religioso jesuita

===== 2026-01-01 =====
  detectSeason: null  week: 0  isSunday: false   seasonFromNearest: null
  propio_id: null

===== 2025-12-31 =====
  lectura: NULL (sin fila en lecturas)   seasonFromNearest: null
```
Todos coinciden con lo esperado por tu tabla.

---

## D. Lógica portada

- **Racha** (`app/src/main/java/com/example/util/Streaks.kt`): `diff > 1 → 0`; conteo por defecto `1`; `diff < 0` (fecha futura) → `0`. Cubierto por `StreaksTest.kt` (5 tests): no-fecha→0; hoy→count; ayer→count; anteayer/semana previa→0; futura/invalid→0.
  - **Divergencia con RN**: el RN (`data/streaks.ts`) NO descarta fechas futuras: `diff` negativo no cumple `diff > 1`, así que **devuelve el conteo**. El Kotlin devuelve 0. Es una regla extra en Kotlin (quizá más correcta), pero difiere del RN.
- **Saludo por hora**: coinciden los umbrales (`<6 noche, <12 día, <20 tarde, resto noche`). Zona horaria: **del dispositivo**. Kotlin `DefaultDateProvider(zoneId = ZoneId.systemDefault())`; RN `new Date().getHours()`. (En el emulador se vio "noche" a las 17:29 locales porque el emulador estaba en UTC.)
- **Colores litúrgicos** (`Seasons.kt`): idénticos a RN → adviento/cuaresma `#7B3FAF` (Morado), pascua/navidad `#E8C97A` (Blanco), ordinario `#4CAF50` (Verde).
- **Recopia de contenido + FTS** (`IglesiaDatabaseHelper.kt`):
  - Subir `CURRENT_CONTENT_VERSION` **sí** fuerza recopiar: guarda `installedVersion < CURRENT_CONTENT_VERSION`.
  - **El índice FTS NO se reconstruye** (y de hecho nunca se crea): el guard es `PRAGMA user_version < 1`, pero el asset viene con `user_version = 10`. Verificado empíricamente:
    ```
    PRAGMA user_version tras copiar = 10
    tablas FTS tras initializeDatabase() = []
    ```
  - Efecto: `BibliaRepository.searchVersiculos` intenta `... JOIN biblia_pueblo_dios_fts ... MATCH ?`, lanza excepción (tabla inexistente), cae al `catch` y usa el fallback `LIKE` (sin ranking `f.rank`, más lento). La búsqueda funciona, pero sin FTS.
- **`favoritosMap = emptyMap()`** (`BibliaViewModel`): el valor inicial sigue (`cachedFavoritosMap = emptyMap()`), pero `observeFavoritos()` se llama en `init {}` y (a) cachea el mapa, (b) parchea el estado `Success` vigente (`currentState.copy(favoritosMap = favMap)`). Efecto real: a lo sumo, si se abre una lectura **antes** de que los favoritos emitan, se ven sin resaltado por un instante y luego se corrigen. No es un bug funcional en el camino normal.

---

## E. Tabla final

| # | Afirmación | Verificada | Evidencia (resumen) | Diferencias |
|---|---|---|---|---|
| A1 | RN `getSantosDelDia` real corre sobre la DB | Sí | `santos_parity_rn.mjs` → 366/2024, 365/2026 | — |
| A2 | Kotlin dedup corre sobre la misma DB | Sí | test Robolectric → `kotlin_santos_*.json` | — |
| A3 | Paridad de dedup RN↔Kotlin | Sí | `compare_santos.mjs`: 731/731 iguales, 0 diffs | fallback `santoKey` (1 palabra) difiere en principio; no manifestado |
| A4 | 2026-10-12 igual en ambos | Sí | `["Nuestra Señora del Pilar / ...","S. Edisto...","S. Serafín..."]` | — |
| B5 | `biblia_navarra` existe | Sí | query `sqlite_master LIKE '%navarra%'`; 35.617 filas | Kotlin no la usa (RN sí, con selector de versión) |
| B5 | tablas del prompt | Parcial | existe `catecismo_cic`, no `catecismo` | nombre distinto |
| B6 | rango/conteo/faltantes de `lecturas` | Sí | 2026-01-01..2026-12-02, 336, sin faltantes | — |
| B6 | existen 2026-03-29 y 2026-04-05 | Sí | `set.has(...) = true` (ambos) | — |
| B7 | assets Kotlin y RN idénticos | Sí | sha256 iguales | — |
| C | valores Home 5 fechas | Sí | `home_values.txt` | coincide con lo esperado |
| D | racha diff>1→0, default 1 | Sí | `StreaksTest.kt` (5 tests) | Kotlin además descarta fecha futura; RN devuelve count |
| D | saludo por hora + TZ | Sí | umbrales iguales; TZ = dispositivo | — |
| D | colores litúrgicos | Sí | `Seasons.kt` == RN | — |
| D | bump versión → recopia + FTS | Parcial | recopia SÍ; FTS **NO** (asset `user_version=10`) | FTS nunca se crea; búsqueda cae a LIKE |
| D | bug `favoritosMap = emptyMap()` | Sí (atenuado) | `init{}` llama `observeFavoritos()` que cachea+parchea | solo transitorio |

## Lo que NO pude verificar y por qué
- **Paridad visual de la Home** (colores/píxeles): fuera del alcance de este prompt y este modelo no procesa imágenes; verificado a nivel de datos/lógica y (en sesión previa) por volcado del árbol de UI.
- **Comportamiento del RN en runtime** (santos/racha reales en la app RN): verificación a nivel de código/datos, no levanté el RN.
- **Uso de `db/test-utils.ts`** (A.1): no factible sin adaptarlo por `__dirname`/ESM; se sustituyó por loader + `sql.js`, importando la función real.
