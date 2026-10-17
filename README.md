# ✝️ Iglesia Digital (Android)

Aplicación católica **100 % offline** para Android, escrita de forma nativa en **Kotlin + Jetpack Compose**.
Es el port directo de la app de referencia en **React Native / Expo** ([`bible-app`](https://github.com/Mauricio-Cardozo/Biblia-app)); comparte **la misma base de datos SQLite** (`iglesia_digital.db`) y la misma identidad visual Navy/Gold.

> **Estado:** en desarrollo activo. Biblia y Liturgia ya son funcionales; el bloque devocional (Oración, Misal, Ajustes) está pendiente. Ver [Roadmap](#-roadmap).

---

## ✨ Funcionalidades

### ✅ Implementado

- **Sagrada Biblia** — navegación en 3 niveles (Libros → Capítulos → Lectura de versículos)
  - **Dos traducciones**: *El Pueblo de Dios* y *Biblia de Navarra*, con **selector persistente** (DataStore).
  - **Búsqueda de texto completo** (FTS4 `unicode61 "remove_diacritics=1"` con *fallback* a `LIKE`): ignora acentos y mayúsculas («jesus» encuentra «Jesús»), encuentra prefijos («amor» → «amores») y pagina de a 50 resultados con «Ver más».
  - **Favoritos y resaltados** versionados (`biblia-pueblo-<id>` / `biblia-navarra-<id>`) sobre Room.
  - **Tamaño de letra** ajustable (0.8×–1.5×) y **compartir versículo** vía `Intent`.
- **Home / Liturgia** — Hero con saludo contextual, tiempo y **color litúrgico**, evangelio destacado, **santo del día** y **rachas** 🔥.
- **Evangelio del Día** — lecturas completas por fecha (`YYYY-MM-DD`), invocable desde el calendario.
- **Calendario litúrgico** — grilla mensual con indicador de días con lectura; toca un día → Evangelio.
- **Detalle del Santo** — biografía y, si corresponde, el **Propio de la misa** del día (`misal_santos`).

### 🔴 Pendiente

- **Oración / Devocionario** — Rosario guiado, Coronilla, novenas y jaculatorias.
- **Misal Romano** — Ordinario, Prefacios, Plegarias Eucarísticas, Guía y Propio del tiempo.
- **Favoritos** — pantalla de listado con filtros, notas y etiquetas (las entidades Room ya existen).
- **Ajustes** — notificaciones diarias, tamaño de letra, respaldo/restauración (export/import JSON).
- **Catecismo (CIC) y YOUCAT** — datos presentes en la DB; falta la UI.

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje |
| Jetpack Compose (BOM) | 2024.09.00 | UI declarativa + Material 3 |
| Android Gradle Plugin | 9.1.1 | Build (Gradle 9.3.1) |
| Room | 2.7.0 | Favoritos, notas y etiquetas del usuario |
| DataStore Preferences | 1.1.7 | Preferencias (versión bíblica, escala de fuente) |
| SQLite (asset) | — | Contenido estático de solo lectura |
| Navigation Compose | 2.8.9 | Grafo de navegación |
| Kotlin Coroutines / Flow | 1.10.2 | Asincronía e I/O en `Dispatchers.IO` |
| KSP | 2.3.5 | Procesamiento de anotaciones (Room) |
| JUnit + Robolectric | 4.13.2 / 4.16.1 | Tests unitarios en JVM |
| Compose UI Test | — | Tests instrumentados en dispositivo |

`minSdk 26` · `targetSdk 36` · `compileSdk 36` · `applicationId com.iglesiadigital.app`

---

## 🏗️ Arquitectura

Tres niveles de persistencia claramente separados (MVVM + inyección de dependencias manual):

```
┌──────────────────────────────────────────────────────────────────┐
│            Presentación — Jetpack Compose + ViewModels           │
└──────────────┬─────────────────────┬────────────────────┬────────┘
               ▼                     ▼                    ▼
    ┌────────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │ Contenido estático │ │  Datos de usuario│ │  Preferencias    │
    │ SQLite (asset)     │ │  Room            │ │  DataStore       │
    │ IglesiaDatabaseHelp│ │  UserDatabase    │ │  UserPreferences │
    ├────────────────────┤ ├──────────────────┤ ├──────────────────┤
    │ Biblia (2 versiones│ │ Favorito         │ │ biblia_version   │
    │ Misal, Lecturas,   │ │ Tag              │ │ font_size_mult.  │
    │ Santos, Catecismo  │ │ (notas + color)  │ │                  │
    │ + FTS4 por versión │ │ Flow<> reactivo  │ │ Flow<>           │
    └────────────────────┘ └──────────────────┘ └──────────────────┘
```

- **Contenido** — `iglesia_digital.db` (~17 MB) se copia desde `assets/` al almacenamiento interno en el primer arranque. Se versiona con un `CURRENT_CONTENT_VERSION` para forzar una recopia cuando el asset cambia.
- **Usuario** — `user.db` gestionado por Room, con consultas `Flow<List<T>>` que refrescan la UI al instante.
- **Preferencias** — `user_preferences.preferences_pb` vía DataStore.
- **DI** — contenedor manual `AppContainer` / `DefaultAppContainer`, instanciado en `IglesiaApplication`; los ViewModels se crean con `ViewModelProvider.Factory`.

---

## 🗄️ Base de Datos

`app/src/main/assets/iglesia_digital.db` (SQLite, `user_version = 10`). Contenido actual:

| Tabla | Filas | Contenido |
|---|---:|---|
| `biblia_pueblo_dios` | 35.852 | Biblia El Pueblo de Dios |
| `biblia_navarra` | 35.617 | Biblia de Navarra |
| `santos` | 889 | Biografías del santoral |
| `lecturas` | 367 | Leccionario diario (2026-01-01 → 2027-01-02) |
| `misal_propio` | 157 | Propio del Tiempo |
| `misal_ordinario` | 202 | Ordinario de la Misa |
| `misal_prefacios` | 67 | Prefacios |
| `misal_plegarias` | 4 | Plegarias Eucarísticas |
| `misal_santos` | 207 | Misas propias de santos |
| `novenas` / `novena_dias` | 18 / 162 | Devociones de 9 días |
| `youcat` | 526 | Catecismo YOUCAT |
| `catecismo_cic` | — | Catecismo de la Iglesia Católica |
| `leccionario`, `misal_guia` | — | Apoyo |

> **FTS4, no FTS5.** El SQLite del framework Android **no incluye el módulo FTS5**; usa FTS4/FTS3. La app crea un índice FTS4 (*external content*) por cada versión bíblica (`biblia_pueblo_dios_fts`, `biblia_navarra_fts`) al arrancar si no existe — con el tokenizador `unicode61 "remove_diacritics=1"`, así que la búsqueda ignora acentos — y lo recrea si una instalación vieja lo tiene con otro tokenizador. Cae a `LIKE` si el índice no está disponible. La app React Native sí usa FTS5 porque `expo-sqlite` empaqueta su propio SQLite.

---

## 🚀 Compilación y ejecución

### Requisitos previos

- **JDK 17+** (probado con JDK 21/27)
- **Android SDK** con Build-Tools y Platform 36 (`local.properties` → `sdk.dir=...`)

### Comandos

```bash
# APK de debug
./gradlew assembleDebug

# APK de release
./gradlew assembleRelease

# Limpiar
./gradlew clean
```

El wrapper de Gradle está incluido (`./gradlew`), no hace falta instalar Gradle.

---

## 🧪 Tests

```bash
# Unitarios (JVM + Robolectric) — 49 tests, 11 suites
./gradlew testDebugUnitTest

# Instrumentados (requieren dispositivo/emulador)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lintDebug
```

Los tests unitarios cubren: versiones bíblicas, búsqueda, migración de favoritos, lógica litúrgica (`seasons`), rachas, fechas y utilidades. Los instrumentados verifican en dispositivo el FTS4 real (acentos, prefijos, paginación y reconstrucción del índice), la versión Navarra, el Calendario, el flujo Home → Calendario → Evangelio, y la cobertura del leccionario (días hasta 2027-01-02, Segunda lectura separada y degradación fuera de rango).

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/example/
├── IglesiaApplication.kt          # Application: crea el AppContainer
├── MainActivity.kt                # Activity única (Compose)
├── AppContainer.kt                # DI manual (repositorios + preferencias)
├── data/
│   ├── db/
│   │   ├── IglesiaDatabaseHelper.kt  # Asset DB + FTS4 por versión
│   │   ├── BibliaModels.kt           # BibliaVersion, LibroBiblia, VersiculoBiblia
│   │   └── LiturgiaModels.kt         # Lectura, Santo, Misal*, DiaLectura
│   ├── repository/
│   │   ├── BibliaRepository.kt       # Consultas bíblicas por versión
│   │   └── LiturgiaRepository.kt     # Lecturas, santos, calendario, misal santos
│   └── user/
│       ├── UserDatabase.kt           # Room (Favorito, Tag) + migraciones
│       ├── UserDaos.kt
│       ├── UserEntities.kt
│       ├── UserPreferencesRepository.kt      # DataStore (versión, fuente)
│       └── StreaksPreferencesRepository.kt   # Rachas
├── ui/
│   ├── biblia/                       # LibrosScreen, CapitulosScreen, LecturaScreen
│   ├── liturgia/                     # LiturgiaScreen (Home), EvangelioScreen,
│   │                                 # CalendarioScreen, SantoDetalleScreen, Hero*,
│   │                                 # SantoCard, StreakCard
│   ├── navigation/                   # MainScreen (NavHost + bottom bar), Destinations
│   ├── common/                       # ComingSoonScreen, StateScreens
│   └── theme/                        # Color, Theme, Type
└── util/                             # Seasons, Streaks, DateUtils, SantosDeduplication

app/src/test/                        # Tests unitarios (JVM + Robolectric)
app/src/androidTest/                 # Tests instrumentados (dispositivo)
app/src/main/assets/iglesia_digital.db
tools/lecturas/                      # Scrapeo y completado del leccionario
verificacion/                        # Informe de paridad RN ↔ Kotlin
```

---

## 🎨 Diseño

Paleta **Navy / Gold** basada en los colores litúrgicos:

| Token | Hex | Uso |
|---|---|---|
| `NavyBackground` | `#0A1128` | Fondo principal |
| `NavySurface` | `#101A38` | Cards y headers |
| `GoldPrimary` | `#C9A84C` | Acentos, números de versículo |
| `GoldLight` | `#E8C97A` | Texto destacado |

Definidos en `ui/theme/Color.kt`. El color del **badge litúrgico** varía según el tiempo (verde, morado, rojo, blanco/dorado).

---

## 🗺️ Roadmap

| Fase | Descripción | Estado |
|---|---|:---:|
| **1. Fundación** | Arquitectura, SQLite helper, Room, DataStore, DI y tema Compose | ✅ *(pendiente: renombrar paquete a `com.iglesiadigital.app`)* |
| **2. Sagrada Biblia** | 3 niveles, búsqueda FTS4, favoritos y **selector Pueblo/Navarra** | ✅ |
| **3. Liturgia y Misal** | Home, lecturas, Evangelio, Calendario y Santos | 🟡 *(falta el Misal: ordinario, prefacios, plegarias, guía)* |
| **4. Oración y Devocionario** | Rosario, Coronilla, novenas, jaculatorias y rachas | 🔴 |
| **5. Calendario y Santos** | Calendario mensual, santos del día y fichas biográficas | ✅ |
| **6. Favoritos, Ajustes y Utilidades** | Pantalla de favoritos, ajustes, export/import y diagnóstico | 🔴 |

> La app React Native está recortando el Alcance de Catecismo y Misal; el port alineará su alcance con esa decisión.

---

## 🤝 Contribuir

### Convención de commits (Conventional Commits, en español)

```
<tipo>(<ámbito>): <resumen imperativo, ≤72 chars>

Por qué: <el problema o la razón del cambio>

- <cambio concreto 1>
- <cambio concreto 2>

Refs #<issue>
```

Tipos: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`, `ci`, `delete`, `redesign`.
Ámbitos: `biblia`, `liturgia`, `misal`, `oracion`, `catecismo`, `ui`, `db`, `data`, `nav`, `build`.

Cada commit debe **compilar** y dejar los tests en verde. Cambios de esquema (Room/SQLite) van en su propio commit.

---

## 🙏 Créditos

- **Biblia del Pueblo de Dios** — texto bíblico en español latinoamericano.
- **Biblia de Navarra** — © Ediciones Universidad de Navarra; texto alojado por EWTN.
- **Catecismo de la Iglesia Católica** — © Libreria Editrice Vaticana.
- **Misal Romano (México)** — © LiturgiaPapal.org / Conferencia del Episcopado Mexicano.
- **Lecturas de la Misa** — textos litúrgicos según Vatican News (vaticannews.va) y el leccionario de curas.com.ar; rellenos puntuales desde dominicos.org.

Desarrollado con ❤️ por [Mauricio](https://github.com/Mauricio-Cardozo).

---

## 📄 Licencia

MIT — ver [`LICENSE.md`](./LICENSE.md). Los textos bíblicos y catequéticos pertenecen a sus respectivos propietarios.
