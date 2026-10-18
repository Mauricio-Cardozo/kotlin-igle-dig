# AGENTS.md — Iglesia Digital (Android / Kotlin + Jetpack Compose)

## 1. Descripción del Proyecto
**Iglesia Digital** es una aplicación católica offline para Android desarrollada de forma nativa en **Kotlin** y **Jetpack Compose**.
Es el proyecto de migración directa de la aplicación de referencia desarrollada en **React Native / Expo** ubicada en `/home/mauricionc/Documentos/bible-app/AppMovil`.

El objetivo de la app es proporcionar acceso sin conexión a internet a:
- La Sagrada Biblia completa en dos versiones: *El Pueblo de Dios* y *Biblia de Navarra*.
- Las lecturas de la Misa diaria (Primera lectura, Salmo responsorial, Segunda lectura, Evangelio y comentario).
- El Misal Romano completo (Ordinario con rúbricas y roles, Propio del tiempo, Prefacios, Plegarias Eucarísticas y Guía).
- El Santoral del día con martirologio y biografías de santos.
- El Catecismo de la Iglesia Católica (CIC) y el YOUCAT.
- Herramientas interactivas de oración: Santo Rosario guiado paso a paso con seguimiento de misterios y cuentas, Coronilla de la Divina Misericordia, novenas, jaculatorias y registro de rachas de oración.

---

## 2. Arquitectura de Datos y Sistema

El diseño sigue una arquitectura desacoplada en tres niveles de persistencia:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Capa de Presentación                          │
│               Jetpack Compose + Material 3 + ViewModels                │
└───────────────┬────────────────────────┬───────────────────────┬───────┘
                │                        │                       │
                ▼                        ▼                       ▼
    ┌──────────────────────┐   ┌───────────────────┐   ┌─────────────────┐
    │  Contenido Estático  │   │  Datos de Usuario │   │  Preferencias   │
    │  SQLite Directo      │   │  Room Database    │   │  DataStore      │
    │ (IglesiaDbHelper)    │   │  (UserDatabase)   │   │  (Preferences)  │
    ├──────────────────────┤   ├───────────────────┤   ├─────────────────┤
    │ iglesia_digital.db   │   │ user.db           │   │ user_prefs.pb   │
    │ · Biblia (Pueblo/    │   │ · Favoritos       │   │ · Font scale    │
    │   Navarra)           │   │ · Notas & Colores │   │ · Notificaciones│
    │ · Misal, Lecturas    │   │ · Tags de usuario │   │ · Silencio      │
    │ · Santos, Catecismo  │   │                   │   │                 │
    │ · Tablas FTS4        │   │                   │   │                 │
    └──────────────────────┘   └───────────────────┘   └─────────────────┘
```

### 2.1. SQLite Directo para Contenido Estático (`IglesiaDatabaseHelper`)
- **Archivo**: `app/src/main/assets/iglesia_digital.db` (~17 MB en su versión completa funcional).
- **Acceso**: `android.database.sqlite.SQLiteDatabase` vía `IglesiaDatabaseHelper`.
- **Propósito**: Base de datos precompilada que se copia a la carpeta interna de la app (`context.getDatabasePath(...)`) en el primer inicio.
- **Búsqueda rápida**: Índices virtuales **FTS4** por versión bíblica (`biblia_pueblo_dios_fts`, `biblia_navarra_fts`) con tokenizador `unicode61 "remove_diacritics=1"` (insensible a acentos), creados al arrancar si faltan y **recreados** si existen con un tokenizador viejo (su SQL se inspecciona en `sqlite_master`). El chequeo es contra `sqlite_master`, no `PRAGMA user_version`, para no pisar el `user_version` del asset. Con *fallback* automático a `LIKE` si el índice no está disponible.
- **Índices de consulta**: `idx_biblia_pueblo_dios_lcv` e `idx_biblia_navarra_lcv` sobre `(libro, capitulo, versiculo)`, creados con `CREATE INDEX IF NOT EXISTS` al inicializar (también en instalaciones existentes, sin recopiar el asset).
- **FTS4, no FTS5**: el SQLite del framework Android no incluye el módulo FTS5; sólo FTS4/FTS3. La app React Native puede usar FTS5 porque `expo-sqlite` empaqueta su propio SQLite. Consecuencia: FTS4 no tiene columna `rank` (bm25), así que la búsqueda ordena por `id`.
- **Razón técnica**: Evita el overhead de generación de código y esquemas de Room en una base de datos de contenido masiva y de solo lectura que ya cuenta con un esquema SQLite maduro.

### 2.2. Room para Datos del Usuario (`UserDatabase`)
- **Archivo**: `user.db` gestionado íntegramente por Room.
- **Entidades**:
  - `Favorito`: Guarda versículos, citas del CIC, evangelios y preguntas de YouCat. Contiene notas personales, tags y colores de resaltado.
  - `Tag`: Etiquetas creadas por el usuario para organizar favoritos.
- **Reactividad**: DAOs exponen consultas basadas en `Flow<List<T>>` para actualización inmediata en Compose.

### 2.3. DataStore Preferences para Configuración (`UserPreferencesRepository`)
- **Archivo**: `user_preferences.preferences_pb` vía `androidx.datastore.preferences`.
- **Claves gestionadas**:
  - `biblia_version`: Versión bíblica activa (`pueblo` / `navarra`), defecto `pueblo`.
  - `font_size_multiplier`: Multiplicador de escala de fuente (0.8f a 1.5f, pasos de 0.1f, defecto 1.0f).
  - (Pendiente) Preferencias de notificaciones y modo silencio.

### 2.4. Inyección de Dependencias
- Inyección manual limpia mediante contenedor: `AppContainer` y `DefaultAppContainer` instanciados en `IglesiaApplication`.
- Los ViewModels se instancian a través de `ViewModelProvider.Factory` recibiendo las dependencias requeridas desde el `AppContainer`.

---

## 3. Convenciones de Código

- **Estilo y Lenguaje**:
  - Kotlin 2.x idiomatico.
  - Corrutinas: Todas las operaciones de I/O a SQLite, Room o DataStore deben ejecutarse estrictamente en `Dispatchers.IO`.
- **Compose**:
  - Los componentes de interfaz (`Screen`, `Component`) deben aplicar *State Hoisting*. Los componentes no deben recibir directamente el ViewModel a menos que sean pantallas raíz de navegación.
  - Uso de `testTag` descriptivos en elementos interactivos para pruebas automáticas.
  - Paleta litúrgica coherente definida en `Color.kt` (`NavyBackground` `#0D1B2A`, `NavySurface` `#1A2D45`, `NavySurfaceElevated` `#243B55`, `GoldPrimary` `#C9A84C`, `GoldLight` `#E8C97A`, `GoldSubtle` `#8B6914`). Tipografía del sistema (sin serifas) y escala de espaciado/radios en `Dimens.kt` (`Spacing`/`Radii`), en paridad con la RN.
- **Navegación**:
  - Rutas dinámicas en `NavigationDestinations.kt` deben utilizar codificación URL (`android.net.Uri.encode` / `decode`) para parámetros como nombres de libros con espacios o caracteres especiales (ej. "1 Corintios", "Génesis").
- **Terminología**:
  - Nombres de dominio en español para entidades litúrgicas, libros bíblicos y devociones, asegurando coincidencia exacta con la app de referencia en React Native.

---

## 3.5. Límites del Agente

> **Regla inquebrantable:** No aceptes licencias, instales software ni modifiques nada fuera
> de este repositorio sin preguntarme explícitamente primero. Si falta una herramienta
> (SDK, JDK, emulador, etc.), detente y avísame con instrucciones claras de lo que necesito
> instalar. Nunca fabriques directorios, archivos de licencia ni carpetas de SDK falsos.

---

## 4. Cómo Compilar y Correr los Tests

### Requisitos Previos
- **JDK**: Java 17 o superior (compatible con AGP 9.x y Kotlin 2.2+).
- **Android SDK**: Build-Tools y Platform SDK instalado (API 34/36).

### Comandos de Compilación
- **Compilar APK Debug**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Compilar APK Release**:
  ```bash
  ./gradlew assembleRelease
  ```
- **Limpiar proyecto**:
  ```bash
  ./gradlew clean
  ```

### Ejecución de Pruebas
- **Pruebas Unitarias Locales (JUnit + Robolectric)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Pruebas Instrumentadas en Dispositivo/Emulador**:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```
- **Verificación de Linter**:
  ```bash
  ./gradlew lintDebug
  ```

---

## 5. Plan de Migración por Fases y Estado

| Fase | Descripción | Estado | Detalle |
| :--- | :--- | :---: | :--- |
| **Fase 1: Fundación y Configuración Base** | Setup de arquitectura, SQLite helper, Room, DataStore, DI y tema Compose. | 🟢 **90%** | Arquitectura funcional; Gradle Wrapper incluido y firmas resueltas. Pendiente: renombrar paquete `com.example` → `com.iglesiadigital.app` y sanear dependencias no usadas (Firebase/Camera/Retrofit comentadas). |
| **Fase 2: Sagrada Biblia** | Selector de libros (AT/NT), selector de capítulos, lector de versículos con tipografía, búsqueda y favoritos/subrayados. | 🟢 **100%** | Flujo completo para **dos versiones** (Pueblo de Dios y Navarra), selector persistido, búsqueda FTS4 con fallback `LIKE` y **favoritos versionados** (`biblia-<version>-<id>`). |
| **Fase 3: Liturgia y Misal Romano** | Home, tiempo y color litúrgico, lecturas del día, Evangelio, Calendario, Santos, Ordinario, Prefacios, Plegarias y Propio. | 🟡 **60%** | Hecho: Home, lecturas del día, Evangelio por fecha, Calendario y detalle de Santo. Falta el Misal (Ordinario, Prefacios, Plegarias, Guía, Propio). |
| **Fase 4: Oración y Devocionario** | Hub de oración, Rosario interactivo, Coronilla, novenas, jaculatorias y rachas de oración. | 🔴 **0%** | `ComingSoonScreen`. Requiere migrar los runners interactivos y datasets estáticos del RN (`prayers.ts`, `rosario-steps.ts`, etc.). |
| **Fase 5: Calendario Litúrgico y Santos** | Calendario mensual, santos del día y fichas biográficas. | 🟢 **100%** | `CalendarioScreen` (grilla mensual + días con lectura) y `SantoDetalleScreen` (biografía + Propio de `misal_santos`). |
| **Fase 6: Favoritos, Ajustes y Utilidades** | Pantalla de favoritos con filtros/tags, ajustes, import/export JSON y diagnóstico. | 🔴 **10%** | Entidades Room y DataStore listos; faltan las pantallas Compose y la lógica de backup/restore. |

---

## 6. Decisiones de Diseño (por qué)

- **FTS4 en vez de FTS5**: el SQLite del framework Android no trae FTS5. `expo-sqlite` (RN) sí, por eso la app de referencia usa FTS5. Se optó por FTS4 *external content* con `rebuild` y *fallback* a `LIKE` para no romper la búsqueda nunca.
- **Chequeo por `sqlite_master`, no `PRAGMA user_version`**: el asset ya viene con `user_version = 10` (migraciones de la app RN). Usar `user_version` como marcador de "FTS inicializado" haría que el índice nunca se creara en un asset que ya lo trae seteado.
- **Tokenizador `unicode61 "remove_diacritics=1"` y recreación del índice**: el tokenizador `simple` (default de FTS4) no quita diacríticos: «jesus» no encontraba «Jesús». `unicode61` con `remove_diacritics=1` existe desde SQLite 3.7.13 (cubre minSdk 26; `remove_diacritics=2` es de 3.27, demasiado nuevo). Como el índice ya existe en los teléfonos que corrieron versiones anteriores, `ensureFtsIndex` lo detecta por su SQL en `sqlite_master` (sin `remove_diacritics=1` = viejo), lo DROPea y lo recrea con `rebuild`: no hace falta subir `CURRENT_CONTENT_VERSION` ni recopiar los 17 MB. El `DROP` de un FTS4 *external content* no toca la tabla de contenido.
- **Búsqueda por prefijo (`"amor*"`)**: cada token del usuario se consulta como prefijo para que «amor» encuentre «amores». En FTS4 la estrella debe ir **dentro** de las comillas (fuera es ignorada). Se limpian comillas y asteriscos del input para que el usuario no pueda inyectar sintaxis.
- **Room para datos de usuario y SQLite crudo para contenido**: el contenido es masivo, de solo lectura y con esquema maduro (no conviene el codegen de Room); los datos del usuario son pequeños, relacionales (favoritos ↔ tags) y se benefician de `Flow` reactivo.
- **Versión de Biblia en DataStore como estado local del ViewModel, sin Context global**: mismo criterio que el RN (que usa AsyncStorage local, no un Context). Evita infraestructura multi-versión especulativa.
- **Favoritos versionados (`biblia-pueblo-<id>`)**: un mismo `id` numérico existe en ambas tablas bíblicas, así que la clave del favorito debe incluir la versión. Migración Room 1→2 convierte las claves antiguas `biblia-<id>` → `biblia-pueblo-<id>`.
- **`namespace = com.example` vs `applicationId = com.iglesiadigital.app`**: el namespace del código sigue en `com.example` (plantilla de AI Studio); el `applicationId` real ya es `com.iglesiadigital.app`. Renombrar el paquete queda como tarea de Fase 1.
- **Lecturas mantenidas con `tools/lecturas/`**: la tabla `lecturas` se regenera con herramientas propias — Vatican News (título, 1ª, 2ª, evangelio y palabras del Papa; ventana móvil de ~77 días) + curas.com.ar (salmo y aleluya del leccionario perpetuo, en páginas por celebración: dominicales, feriales con sección por día (lunes–sábado, o «DÍA N» en las ferias de Navidad) y de fiestas; `completar_leccionario.py` resuelve cada fecha por palabra clave del título (fiestas y solemnidades, con ciclo dominical A/B/C), por fecha fija (octava de Navidad) o por calendario litúrgico —temporada y semana del cómputo de Pascua/Adviento/ceniza, con ciclo ferial impar/par— y lee la sección del día; `--dry-run` muestra qué se completaría y `--scrape` refresca la tabla guardada) + dominicos.org como relleno puntual de fechas que Vatican News todavía no publica y de campos vacíos de fechas existentes (`--completar` / `--faltantes`). Los salmos y aleluyas con contenido nunca se pisan; los días sin fuente quedan con el bloque vacío (la UI lo oculta): la octava de Pascua (y la Vigilia pascual) no tienen página ferial, en Cuaresma y Semana Santa la aclamación no dice «Aleluya», y las memorias con lecturas propias sin página en curas (p. ej. María Madre de la Iglesia o el Corazón Inmaculado) quedan sin rellenar. Al actualizar el asset hay que subir `CURRENT_CONTENT_VERSION` en `IglesiaDatabaseHelper.kt` para forzar la recopia en instalaciones existentes; `user_version` no se toca (pertenece al esquema heredado de la app RN).
- **Paridad visual con la RN (pulido de UI)**: tab bar flotante en píldora con ocultado al scroll (solo en las pantallas de tabs, como la RN, donde Evangelio/Santo/Rosario/Ajustes quedan fuera del navegador), modo inmersivo (status/navigation bar ocultas, transitorias al swipe) con un tope de 12 dp aportado por `MainScreen`, tipografía sans del sistema y radios/escala en `Dimens.kt`. El orden litúrgico de las secciones del Evangelio se mantiene (la RN muestra la 2ª lectura antes del salmo; no se copia ese error).

---

## 7. Convención de Commits

Conventional Commits en español, con cuerpo que explica el **por qué**:

```
<tipo>(<ámbito>): <resumen imperativo, ≤72 chars>

Por qué: <problema o razón del cambio>

- <cambio concreto>
```

- Tipos: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`, `ci`, `delete`, `redesign`.
- Ámbitos: `biblia`, `liturgia`, `misal`, `oracion`, `catecismo`, `ui`, `db`, `data`, `nav`, `build`.
- Cada commit compila y deja los tests en verde. Los cambios de esquema (Room/SQLite) van en su propio commit.
