# Roadmap: cambios inspirados en las apps de referencia

> Estado: **plan; implementación a cargo del usuario**, Claude revisa cada bloque.
> Orden acordado: **UI primero**, modo claro al final.
> Pendiente aparte: correcciones del latín (revisión del PR #43).

## Seguimiento

- [ ] A. Barra colapsable + Credo/Contrición → ver `navegacion-y-credo.md`
- [ ] B1. Inicio: el encabezado se va con el scroll
- [ ] B2. Lecturas separadas por pestañas (1.ª, Salmo, 2.ª, Evangelio, Comentario)
- [ ] C. Oración: selector Rosario / Coronillas / Letanías
- [ ] D. Calendario: registrar misa, confesión y notas (Room v3)
- [ ] E. Rutinas con recordatorios (Room v4 + notificaciones)
- [ ] F. Modo claro dorado y blanco + selector en Ajustes

Reglas de siempre: commits en español (Conventional Commits), cada uno compila con tests verdes,
**cada cambio de esquema Room en su propio commit**, nada de `fallbackToDestructiveMigration`.

> Consejo: el modo claro (F) toca ~24 archivos que importan `NavyBackground`, `GoldPrimary`, etc.
> Si en B–E escribes pantallas nuevas, usa pocos colores y concéntralos en un solo lugar del archivo
> para que la migración de F sea mecánica.

---

## B1. Inicio (Liturgia): el encabezado se va con el scroll

**Hoy:** `ui/liturgia/LiturgiaScreen.kt` es un `Column` con el encabezado fijo (L76-141:
"✝ IGLESIA DIGITAL", "Liturgia", ⚙, fecha y `misaTitle`) y debajo un `Column` con `verticalScroll` (L144).

**Cambio:**
- Mover el bloque del encabezado **dentro** del `Column` con scroll, como primer hijo.
- El ⚙ (`btn_ajustes`) se va con el encabezado (igual que la referencia, donde el engranaje vuelve
  al subir). Alternativa si se quiere siempre a mano: llevarlo a la burbuja de la barra colapsada (bloque A).
- Revisar el mismo patrón en `OracionHubScreen` y `CalendarioScreen` para que todos los tabs se comporten igual
  (`CalendarioScreen` ya scrollea todo).
- Test: el `testTag` del encabezado deja de existir tras `performScrollToIndex`/swipe hacia arriba.

## B2. Lecturas separadas por pestañas

**Hoy:** `ui/liturgia/EvangelioScreen.kt` muestra todas las `ReadingSection` seguidas en un `LazyColumn`
(L217-270). El modelo `Lectura` (`data/db/LiturgiaModels.kt`) ya trae todo separado.
El inicio **no** mostrará las lecturas (se mantiene solo la tarjeta del evangelio como acceso).

**Cambio:**
- `enum class SeccionLectura(val etiqueta: String, val icono)`: `PRIMERA("1.ª")`, `SALMO("Salmo")`,
  `SEGUNDA("2.ª")`, `EVANGELIO("Evangelio")`, `COMENTARIO("Comentario")`.
  - Función pura `seccionesDisponibles(lectura): List<SeccionLectura>` (omite `SEGUNDA` si
    `segundaLectura` es nula, `COMENTARIO` si no hay `comentarioPapal`). Fácil de testear.
  - El aleluya se muestra al inicio de la pestaña Evangelio (es su aclamación).
- En `EvangelioScreen`:
  - Fila de chips desplazable arriba (reutilizar el estilo de `PillRosario`) + `HorizontalPager`
    sincronizado (`pagerState.animateScrollToPage` al tocar un chip; el chip activo sigue al swipe).
  - Cada página es un `LazyColumn` con una sola `ReadingSection`.
  - Salmo: si el texto empieza por una línea de respuesta (`R.` / `℟`), mostrarla en una caja
    "Respuesta" como en la referencia; si no, texto normal. (Verificar el formato real en el asset antes.)
- Ruta: `evangelio?fecha={fecha}&seccion={seccion}` (default `evangelio`), para abrir directo una sección
  desde el Calendario o el Inicio. Actualizar `evangelioRoute(fecha, seccion)` en `NavigationDestinations.kt`.
- Tests: `seccionesDisponibles` (con y sin 2.ª lectura/comentario); UI: tocar "Salmo" muestra el salmo.

## C. Oración: Rosario / Coronillas / Letanías

**Hoy:** `OracionHubScreen` lista 6 tarjetas (`SECCIONES_ORACION`); solo Rosario es real,
Coronilla/Oraciones/Jaculatorias/Novenas/Favoritos son `ComingSoonScreen`; **no existen letanías**.

**Cambio (como la referencia):**
- Botón arriba a la derecha que abre un menú con **Rosario · Coronillas · Letanías**
  (`DropdownMenu` con fondo `NavySurfaceElevated`), o pestañas segmentadas; el valor elegido se guarda
  con `rememberSaveable`.
- **Rosario:** encabezado "Misterios de hoy · Dolorosos · Martes, Viernes" (usar `misteriosDelDia(hoy)`)
  y 4 tarjetas: la de hoy destacada ("Toca para comenzar a rezar") y las otras 3 con sus días.
  Tocar una abre `rosario/guia?serie={serie}`; la de hoy abre con `AUTOMATICO`.
  En `RosarioViewModel`, aplicar la serie solo si difiere de la actual (cambiarla reinicia el rezo).
- **Coronillas:** Divina Misericordia (5 decenas), San Miguel (9 salutaciones), Siete Dolores (7 dolores)
  → por ahora `ComingSoonScreen` (la coronilla real es la Fase 4 pendiente de `AGENTS.md`).
- **Letanías:** Loreto (reutilizar `textos.letanias`, ya existe en ES/LA) en una pantalla de lectura simple;
  Sagrado Corazón, San José, Santos, Santo Nombre, Preciosísima Sangre → `ComingSoonScreen` hasta tener textos.
- Favoritos, Oraciones, Jaculatorias y Novenas: sección "Más" al final de la vista o en el mismo menú.
- Sin imágenes nuevas (app offline y sin licencias de arte): ícono redondo con emoji o `Icons`, estilo
  "círculo pálido + ícono" de la segunda referencia.
- Tests: `OracionHubUiTest` actualizado (cambiar de vista, abrir Rosario con serie).

## D. Calendario: registrar misa, confesión y notas

**Hoy:** `ui/liturgia/CalendarioScreen.kt` solo muestra datos del asset; tocar un día abre el Evangelio.
Room: `UserDatabase` v2 (`Favorito`, `Tag`), `exportSchema = false`, solo `MIGRATION_1_2`.

**Esquema (commit propio):**
```kotlin
@Entity(tableName = "registros", indices = [Index("fecha")])
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String,          // ISO yyyy-MM-dd
    val tipo: String,           // "misa" | "confesion" | "nota"
    val nota: String? = null,
    val creadoEn: Long          // epoch millis
)
```
- `MIGRATION_2_3`: `CREATE TABLE registros(...)` + `CREATE INDEX index_registros_fecha ...` (copiar exacto lo que
  generaría Room; recomendable activar `exportSchema = true` con `room.schemaLocation` para validarlo).
- `RegistroDao`: `porMes(desde, hasta): Flow<List<Registro>>`, `porFecha`, `insert`, `delete`, `ultimo(tipo)`.
- `RegistrosRepository` en `DefaultAppContainer`.

**UI:**
- Pestañas arriba: **Calendario · Rutina · Notas** (Rutina se llena en E).
- Tocar un día abre una **hoja inferior** con: fecha y título litúrgico, "Ver lecturas" (va a `evangelio?fecha=`),
  "Añadir nota", **Misa** ("Registrar una Misa a la que asististe"), **Confesión**, y la lista de registros del día
  (borrar con confirmación).
- Puntos bajo el día: color del tiempo litúrgico (ya existe) + marca distinta si hay registro.
- Notas: lista cronológica de registros; resumen "Última confesión: hace N días".
- Tests: DAO con Robolectric/in-memory, migración 2→3 (`MigrationTestHelper` si se activa exportSchema),
  ViewModel (agregar/borrar), UI de la hoja.

## E. Rutinas con recordatorios

**Esquema (commit propio):**
```kotlin
@Entity(tableName = "rutinas")
data class Rutina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantilla: String,   // "misa_dominical" | "rosario_diario" | "primer_viernes" | "ofrecimiento" | "coronilla" | "personalizada"
    val titulo: String,
    val dias: Int,           // máscara L..D (bit 0 = lunes); primer viernes se trata aparte
    val hora: Int, val minuto: Int,
    val activa: Boolean = true
)
```
- `MIGRATION_3_4` con `CREATE TABLE rutinas(...)`.
- Plantillas como en la referencia: Primer Viernes, Misa Dominical, Rosario Diario, Ofrecimiento Matutino,
  Coronilla de la Divina Misericordia (15:00), Reflexión vespertina; cada una con "+ Añadir".

**Recordatorios (100 % offline, sin dependencias nuevas):**
- `AlarmManager.setAndAllowWhileIdle` (inexacto, no requiere `SCHEDULE_EXACT_ALARM`) para la próxima ocurrencia
  de cada rutina activa; función pura `proximaOcurrencia(rutina, ahora): LocalDateTime` (testeable,
  incluye la regla de primer viernes).
- `RecordatorioReceiver` publica la notificación (canal "Recordatorios") y reprograma la siguiente.
- `BootReceiver` (`RECEIVE_BOOT_COMPLETED`) reprograma todo tras reiniciar.
- Android 13+: pedir `POST_NOTIFICATIONS` al activar la primera rutina; si se niega, la rutina queda sin aviso
  y se muestra un aviso en la pestaña.
- Tocar la notificación abre la pantalla correspondiente (rosario, lecturas…) vía deep link interno.
- Tests: `proximaOcurrencia` (días, cambio de semana, primer viernes, hora ya pasada), DAO, migración 3→4.

## F. Modo claro dorado y blanco + selector en Ajustes

**Hoy:** `Theme.kt` solo tiene `DarkColorScheme`; ~24 archivos importan colores directos; no hay preferencia de tema;
"Ajustes" es un `ComingSoonScreen` con el enlace a Licencias.

**Infraestructura:**
- `data class IglesiaColors(background, surface, surfaceElevated, separator, gold, goldLight, goldSubtle,
  textPrimary, textSecondary, error, …)` + `LocalIglesiaColors` (`staticCompositionLocalOf`) + accesor
  `IglesiaTheme.colors`.
- Paletas: `OscuraNavy` (valores actuales, sin cambio visual) y `ClaraDorada` (propuesta a ajustar en pantalla):
  fondo `#F7F5F0`, superficie `#FFFFFF`, separador `#ECE7DC`, dorado `#F2A516`, dorado suave `#FDF0D5`
  (círculos de íconos), texto `#1B2433`, texto secundario `#8A8F98`.
- `IglesiaDigitalTheme(tema: TemaApp)`; barras del sistema con íconos oscuros en claro.
- Preferencia `tema_app` en `UserPreferencesRepository` ("sistema" | "claro" | "oscuro", default "sistema").

**Migración:** reemplazo mecánico archivo por archivo (`NavyBackground` → `IglesiaTheme.colors.background`, …),
un commit por área (liturgia, biblia, oración, comunes). Sin cambio visual en modo oscuro.

**Estilo de la segunda referencia (en modo claro):** tarjetas blancas con radio grande (24–28 dp) y sombra suave,
íconos dentro de un círculo dorado pálido, chips redondeados con espacio entre ellos y el activo con borde dorado.
Agregar `Radii.xxxl = 28.dp` en `Dimens.kt`.

**Ajustes reales:** convertir la ruta `ajustes` en pantalla: sección **Apariencia** (Sistema/Claro/Oscuro),
tamaño de texto, y "Licencias y créditos".

- Tests: `desdeClave` del tema con fallback, ViewModel de Ajustes, UI que cambia el tema y verifica el fondo.

## Fuera de alcance por ahora
- Liturgia de las Horas (no hay textos en el asset; solo se toma su estilo visual).
- Imágenes/ilustraciones de las referencias (licencias y peso de la app).
- Reflexión "PRO" / audio "Escuchar" de la referencia.
