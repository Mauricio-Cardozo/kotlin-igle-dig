# Rosario v2 — plan de cambios (en curso)

> Estado: **implementado en la rama `feat/rosario-v2`**; pendientes la revisión del usuario y la revisión litúrgica de los textos latinos.
> Base ya hecha en la rama `claude/vibrant-franklin-gx4b70` (PR #42): pulido visual v1 (Spacing, áreas táctiles de 40 dp, cuentas adaptativas, footer con botón principal).

## Seguimiento

- [x] 1. `refactor(oracion)`: extraer textos a `TextosRosario` (sin cambio de comportamiento)
- [x] 2. `feat(oracion)`: idioma latín + preferencia `rosario_idioma` + `setIdioma()`
- [x] 3. `redesign(oracion)`: hoja de ajustes, cuentas en barra fina, footer limpio
- [x] 4. `docs(oracion)`: AGENTS.md / README (idiomas, plan para griego, estado de Oración)
- [x] Tests unitarios nuevos e instrumentados ajustados: 118 unitarios, 22 instrumentados y lint en verde (ver sección Tests)
- [ ] Revisión de los textos en latín (litúrgica)

---


## Context
El pulido visual v1 (ya en el PR #42) no resolvió lo principal: en el estilo gold+navy la pantalla pierde mucho espacio por 3 filas de controles arriba y 2 abajo. Rosarium deja el texto casi a pantalla completa y manda los ajustes a otro panel. Además se quiere **elegir idioma** (Español ahora, **Latín** nuevo, **Griego con pronunciación** más adelante).

Decisiones del usuario: texto a pantalla completa + **hoja de ajustes**; cuentas en **barra fina** sobre el texto; idiomas **Español + Latín**, interfaz de la app siempre en español.

## Estado actual relevante
- `ui/oracion/RosarioScreen.kt`: selectores (`SelectorMisterio`, `SelectorOraciones`, `FilaAutoavance`) siempre visibles + `FontSizeControl` en la cabecera.
- `ui/oracion/RosarioSteps.kt`: textos en `private object RosarioTextos` (solo español) y títulos/subtítulos hardcodeados en `generarPasosRosario()` ("Señal de la Cruz", "${a}ª Avemaría", "Misterio N de 5 · $nombre", ...). Misterios en listas `MISTERIOS_*`; `misteriosDelDia()` devuelve nombre + lista en español.
- Preferencias: `data/user/UserPreferencesRepository.kt` (claves `rosario_*`); estado en `RosarioViewModel.kt` (`RosarioUiState`); progreso por índice en `StreaksPreferencesRepository`.
- Ids de paso estables (`senal`, `avemaria-$m-$a`, ...): el índice de paso es el mismo en cualquier idioma.

## Diseño

### A. Modelo de idiomas (base para latín y griego)
- Nuevo `RosarioIdioma.kt`: `enum class IdiomaRosario(val clave: String, val etiqueta: String) { ESPANOL("es","Español"), LATIN("la","Latín") }` (+ `desdeClave()` con fallback a español). Griego se añade después como un valor más.
- Nuevo `RosarioTextos.kt` con `data class TextosRosario(...)`: las 14 oraciones (señal, contrición, padrenuestro, avemaría, gloria, Oh Jesús, María Madre, letanías, oración final, padrenuestro papa, avemaría/gloria finales, salve, jaculatoria, completado), nombres de las 4 series de misterios y sus 5 misterios, títulos de paso, ordinales y plantillas ("Misterio N de 5 · X", "N.ª Avemaría").
- `TextosRosarioEs.kt` (el contenido actual movido tal cual, sin cambiar texto) y `TextosRosarioLa.kt` (nuevo). Función `textosDe(idioma)`.
- `generarPasosRosario(fecha, manual, oraciones, idioma = ESPANOL)` usa `textosDe(idioma)`; `misteriosDelDia()` pasa a devolver la serie (enum) y el nombre se toma del idioma. Los ids y el orden de pasos no cambian → **cambiar de idioma NO reinicia el rezo** (a diferencia de Fátima/Salve/Final).
- **Extensión Griego**: `RosarioStep` ganará un campo opcional `pronunciacion: String?` (transliteración) y la hoja de ajustes un interruptor "Mostrar pronunciación"; se deja documentado en `AGENTS.md`, no se construye ahora.

### B. Preferencia y ViewModel
- `UserPreferencesRepository`: clave `rosario_idioma` (String, default `"es"`), `setRosarioIdioma()`; mismo patrón que `ROSARIO_AUTOAVANCE_KEY`.
- `RosarioViewModel`: `idioma` en `RosarioUiState`, `setIdioma()` que regenera los pasos conservando `pasoIndex`; sin persistir progreso nuevo.
- Sin cambios de esquema Room ni de `CURRENT_CONTENT_VERSION`.

### C. Layout con más espacio (`RosarioScreen.kt`)
- Cabecera: `ScreenHeader` con atrás + "Paso X de N" y un botón ⚙ (en `rightSlot`, ≥48 dp) que abre una **hoja inferior** (`ModalBottomSheet` de Material3, fondo `NavySurfaceElevated`).
- Hoja de ajustes (nuevo composable `AjustesRosarioSheet`), reutilizando `PillRosario`: **Idioma**, **Misterios** (Auto/Gozosos/Luminosos/Dolorosos/Gloriosos), **Oraciones** (Fátima/Salve/Oración final + aviso "reinicia el rezo"), **Avance** (Off/Lento/Normal/Rápido) y **Tamaño de texto** (`FontSizeControl`). Se eliminan del cuerpo principal los tres selectores y `FontSizeControl` de la cabecera.
- Indicador discreto en la cabecera cuando el avance automático está activo (p. ej. "Paso 3 de 88 · Auto").
- Cuentas: barra fina (≈ 18 dp de cuenta, título del misterio en 11 sp encima, ~56 dp total) solo durante la decena; se conserva la lógica de v1 (`BoxWithConstraints`, hilo, `testTag`s).
- Footer: solo "← Anterior" / "Amén · Siguiente" (fila única, `maxLines = 1`, `TextAlign.Center` — en tu captura el texto se parte en dos líneas y queda pegado a la izquierda).
- Etiquetas de interfaz siguen en español; solo cambia el contenido de la oración, títulos de paso y nombres de misterio.

### D. Textos en latín
- `TextosRosarioLa.kt`: Signum Crucis, Actus Contritionis, Pater noster, Ave Maria, Gloria Patri, Oratio Fatimae (O mi Iesu), «Maria, Mater gratiae», Litaniae Lauretanae, Oratio finalis, Salve Regina, jaculatoria, mensaje de completado, y los 20 misterios en latín (Annuntiatio, Visitatio, …).
- Se escriben a partir de los textos tradicionales de dominio público, **sin copiar** los archivos de Rosarium (MIT+Credo). Quedan **marcados para revisión litúrgica** del usuario antes de publicar.

## Orden de commits (español, Conventional Commits; cada uno compila y deja tests verdes)
1. `refactor(oracion): extraer textos del rosario a TextosRosario` — mover textos/ordinales/misterios sin cambio de comportamiento; `RosarioStepsTest` sigue verde.
2. `feat(oracion): agregar idioma latín al rosario` — `IdiomaRosario`, textos en latín, `rosario_idioma`, `setIdioma()`.
3. `redesign(oracion): ajustes del rosario en hoja inferior` — `AjustesRosarioSheet`, cabecera con ⚙, cuentas en barra fina, footer limpio.
4. `docs(oracion): documentar idiomas y plan para griego` — `AGENTS.md` y README (de paso corregir el estado "Oración 🔴 pendiente").

## Tests
- Unitarios nuevos: mismos ids y misma longitud de pasos en ES y LA para todas las combinaciones de opciones; textos de LA no vacíos y sin el glifo CJK; `fromClave` con fallback; `RosarioViewModel.setIdioma` regenera pasos conservando el índice y persiste la clave.
- Ajustar `RosarioViewModelTest`/`RosarioStepsTest` solo donde cambie la firma (parámetro `idioma` con default).
- Instrumentado: `RosarioFlowUiTest` y `OracionHubUiTest` — los selectores (`rosario_misterio_*`, `rosario_opcion_*`, `rosario_auto_*`) pasan a estar dentro de la hoja; el test debe abrirla antes (nuevo `testTag` `rosario_ajustes`).

## Verificación
1. `./gradlew testDebugUnitTest lintDebug assembleDebug` — **no hay Android SDK en este contenedor**, hay que correrlo en tu máquina o en CI.
2. En emulador: abrir ⚙, cambiar idioma a Latín en medio de una decena y comprobar que el paso e índice no cambian; probar los 4 misterios, Fátima/Salve/Final, avance automático, fuente A+ (texto largo: letanías en latín), pantallas chicas.
3. El push ya funciona (GitHub conectado); CI corre `testDebugUnitTest` en el PR.

## Riesgos / preguntas abiertas
- Revisión de los textos en latín (los escribo yo; conviene que los revises).
- Griego: necesitará una fuente con soporte polytonic y un diseño de "pronunciación"; fuera de este plan.
