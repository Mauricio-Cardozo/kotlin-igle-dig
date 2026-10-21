# Plan: barra de pestañas colapsable + elegir Credo o Acto de Contrición

> Estado: **plan; implementación a cargo del usuario**, Claude revisa después.
> Base: `main` después del PR #43 (rosario v2: hoja de ajustes e idiomas).
> Pendiente aparte (para después): correcciones del latín señaladas en la revisión del PR #43.

## Seguimiento

- [ ] 1. `fix(nav)`: quitar el hueco fijo de 64 dp bajo el contenido
- [ ] 2. `redesign(nav)`: al bajar, la barra se colapsa a solo la pestaña activa
- [ ] 3. `feat(oracion)`: elegir Credo o Acto de Contrición tras la Señal de la Cruz
- [ ] 4. Tests + `AGENTS.md`

---

## 1. Barra de pestañas: por qué queda el espacio vacío

En `ui/navigation/MainScreen.kt` la barra ya es flotante (está en un `Box` encima del
`NavHost` y se oculta con `AnimatedVisibility` según `TabBarScrollState.hidden`), pero el
`NavHost` tiene un padding **fijo**:

```kotlin
.padding(bottom = if (showTabBar) 64.dp else 0.dp)
```

Cuando la barra se esconde, esos 64 dp siguen reservados: es el "espacio vacío" de las capturas.

### Diseño (referencia: la app de las capturas)
- **Al bajar**: la píldora completa se transforma en una **burbuja con solo la pestaña
  activa** (ícono + punto dorado), abajo a la izquierda. El resto de pestañas desaparece.
- **Al subir** (o al tocar la burbuja): vuelve la píldora completa.
- El contenido pasa **por debajo** de la barra; no se reserva espacio fijo.

### Cambios
1. `MainScreen.kt`
   - Quitar `.padding(bottom = 64.dp)` del `NavHost`.
   - Reemplazar el `AnimatedVisibility` por un `AnimatedContent` (o dos `AnimatedVisibility`)
     sobre `tabBarScrollState.hidden`:
     - `false` → `FloatingTabBar` actual (sin cambios visuales).
     - `true` → nuevo `TabBarColapsada(tab = pestañaActiva, onClick = { tabBarScrollState.reset() })`,
       alineada `BottomStart`, 56 dp, `CircleShape`, mismo fondo `0xF00D1B2A` y borde `GoldSubtle`,
       `testTag("bottom_nav_collapsed")`.
   - Transición: `fadeIn + scaleIn` / `fadeOut + scaleOut` de 200 ms (respeta "reducir movimiento").
   - Calcular `pestañaActiva` con la misma lógica de `selected` de `FloatingTabBar`
     (extraerla a una función `tabActiva(currentRoute, isBibliaTab, isLiturgiaTab)` y reutilizarla).
2. Para que el final de cada lista no quede tapado por la barra expandida, en vez del padding
   global se añade un **espacio al final del contenido** de cada pantalla de pestaña:
   - Constante compartida en `ui/theme/Dimens.kt`: `val TabBarEspacio = 80.dp`.
   - `LiturgiaScreen`, `LibrosScreen` (Biblia), `OracionHubScreen`, Calendario: sumar
     `TabBarEspacio` al `contentPadding` inferior (`LazyColumn`) o un `Spacer` final (`Column` con `verticalScroll`).
   - Así el espacio solo existe al final del scroll, no fijo en pantalla.

### Tests
- Instrumentado (`MainScreen`/nuevo `TabBarUiTest`): tras hacer scroll hacia abajo existe
  `bottom_nav_collapsed` y no existe `bottom_nav_bar`; al tocar la burbuja vuelve `bottom_nav_bar`.
- Revisar que los tests existentes que buscan `bottom_nav_bar` sigan pasando (sin scroll previo).

---

## 2. Credo o Acto de Contrición tras la Señal de la Cruz

### Modelo
- En `RosarioSteps.kt`:
  ```kotlin
  /** Oración tras la Señal de la Cruz. El Acto de Contrición es el valor de siempre. */
  enum class OracionInicial(val valor: Int, val etiqueta: String) {
      CONTRICION(0, "Acto de Contrición"),
      CREDO(1, "Credo");
      companion object { fun desdeValor(v: Int) = entries.firstOrNull { it.valor == v } ?: CONTRICION }
  }
  ```
- `generarPasosRosario(..., inicial: OracionInicial = OracionInicial.CONTRICION)`: el segundo paso es
  - `RosarioStep("contricion", textos.tituloContricion, textos.contricion)` o
  - `RosarioStep("credo", textos.tituloCredo, textos.credo)`.
  - **Misma posición y misma cantidad de pasos** → cambiarlo **no reinicia el rezo** (igual que el idioma).
- `TextosRosario`: nuevos campos `tituloCredo` y `credo`; rellenar en `TextosRosarioEs.kt` y `TextosRosarioLa.kt`.

### Preferencia y ViewModel
- `UserPreferencesRepository`: `ROSARIO_ORACION_INICIAL_KEY = intPreferencesKey("rosario_oracion_inicial")`,
  default `0`, `coerceIn(0, 1)`; mismo patrón que `rosario_autoavance`.
- `RosarioUiState.inicial`, `RosarioViewModel.setOracionInicial()` (copiar la forma de `setIdioma()`:
  regenera pasos, conserva `pasoIndex`, persiste).
- Pasar `it.inicial` en **todas** las llamadas a `generarPasosRosario` del ViewModel (init,
  `onScreenEntered`, `setMisterioManual`, `setOracionOpcional`, `setIdioma`).

### Hoja de ajustes
- Nueva sección **"Al comenzar"** en `AjustesRosarioSheet.kt`, justo después de *Idioma*:
  píldoras `Acto de Contrición` / `Credo`, `testTag("rosario_inicial_${name.lowercase()}")`.

### Textos
**Español** (Símbolo de los Apóstoles):
```
Creo en Dios, Padre todopoderoso,
Creador del cielo y de la tierra.

Creo en Jesucristo, su único Hijo, nuestro Señor,
que fue concebido por obra y gracia del Espíritu Santo,
nació de Santa María Virgen,
padeció bajo el poder de Poncio Pilato,
fue crucificado, muerto y sepultado,
descendió a los infiernos,
al tercer día resucitó de entre los muertos,
subió a los cielos
y está sentado a la derecha de Dios, Padre todopoderoso.
Desde allí ha de venir a juzgar a vivos y muertos.

Creo en el Espíritu Santo,
la santa Iglesia católica,
la comunión de los santos,
el perdón de los pecados,
la resurrección de la carne
y la vida eterna.
Amén.
```
**Latín** (Symbolum Apostolorum), título `Symbolum Apostolorum` (o `Credo`):
```
Credo in Deum, Patrem omnipotentem,
Creatorem caeli et terrae.

Et in Iesum Christum, Filium eius unicum, Dominum nostrum,
qui conceptus est de Spiritu Sancto,
natus ex Maria Virgine,
passus sub Pontio Pilato,
crucifixus, mortuus, et sepultus,
descendit ad inferos,
tertia die resurrexit a mortuis,
ascendit ad caelos,
sedet ad dexteram Dei Patris omnipotentis,
inde venturus est iudicare vivos et mortuos.

Credo in Spiritum Sanctum,
sanctam Ecclesiam catholicam,
sanctorum communionem,
remissionem peccatorum,
carnis resurrectionem,
vitam aeternam.
Amen.
```
(Ambos son textos litúrgicos tradicionales de dominio público.)

### Tests
- `RosarioStepsTest`: con `CREDO` el paso 1 tiene id `credo` y texto que empieza por "Creo en Dios";
  con `CONTRICION` sigue igual; la longitud total no cambia; mismos ids en ES y LA para ambos valores.
- `RosarioViewModelTest`: `setOracionInicial` conserva `pasoIndex`, persiste el valor y se restaura al crear el VM.
- `OracionInicial.desdeValor` cae en `CONTRICION` ante valores desconocidos.
- `RosarioFlowUiTest`: abrir ajustes y comprobar la sección "Al comenzar".

## Verificación
1. `./gradlew testDebugUnitTest lintDebug assembleDebug` (CI corre `testDebugUnitTest`).
2. En dispositivo: bajar en Liturgia/Biblia/Oración/Calendario → queda solo la burbuja, sin hueco;
   subir o tocarla → vuelve la barra; el final de cada lista no queda tapado.
3. Rosario: cambiar a Credo en medio del rezo no cambia el paso; con Credo + Latín se ve el Symbolum.
