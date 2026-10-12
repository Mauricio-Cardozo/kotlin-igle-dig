# CLAUDE.md — Iglesia Digital (Android / Kotlin + Jetpack Compose)

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
    │ · Tablas FTS5        │   │                   │   │                 │
    └──────────────────────┘   └───────────────────┘   └─────────────────┘
```

### 2.1. SQLite Directo para Contenido Estático (`IglesiaDatabaseHelper`)
- **Archivo**: `app/src/main/assets/iglesia_digital.db` (~17 MB en su versión completa funcional).
- **Acceso**: `android.database.sqlite.SQLiteDatabase` vía `IglesiaDatabaseHelper`.
- **Propósito**: Base de datos precompilada que se copia a la carpeta interna de la app (`context.getDatabasePath(...)`) en el primer inicio.
- **Búsqueda rápida**: Índices virtuales `FTS5` (`biblia_pueblo_dios_fts`, `biblia_navarra_fts`, `misal_propio_fts`, etc.) con fallback automático a consultas `LIKE` si FTS5 no está disponible.
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
  - `font_size_multiplier`: Multiplicador de escala de fuente (0.8f a 1.5f, pasos de 0.1f, defecto 1.0f).
  - Preferencias de notificaciones y modo silencio.

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
  - Paleta litúrgica coherente definida en `Color.kt` (`NavyBackground` `#0A1128`, `NavySurface` `#101A38`, `GoldPrimary` `#C9A84C`, `GoldLight` `#E8C97A`).
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

### Comandos de Compilación (una vez restaurado el Gradle Wrapper)
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
| **Fase 1: Fundación y Configuración Base** | Setup de arquitectura, SQLite helper, Room, DataStore, DI y tema Compose. | 🟡 **80%** | Arquitectura funcional creada; pendiente restaurar Gradle Wrapper, corregir signing de debug, sanear dependencias no usadas y migrar paquete a `com.iglesiadigital.app`. |
| **Fase 2: Sagrada Biblia** | Selector de libros (AT/NT), selector de capítulos, lector de versículos con ajuste de tipografía, búsqueda FTS5 y favoritos/subrayados. | 🟡 **70%** | Flujo completo de Pueblo de Dios y UI listos; pendiente reemplazar la DB truncada (3.7 MB -> 17 MB) y agregar selector para Biblia de Navarra. |
| **Fase 3: Liturgia y Misal Romano** | Pantalla principal (Home), tiempo y color litúrgico, lecturas del día, ordinario de la misa, prefacios, plegarias eucarísticas y propio. | 🔴 **0%** | Actualmente `ComingSoonScreen`. Requiere replicar lógica de tiempo litúrgico (`seasons.ts`) y consultas a tablas `lecturas`, `misal_*`. |
| **Fase 4: Oración y Devocionario** | Hub de oración, Santo Rosario interactivo con cuentas y misterios guiados, Coronilla de la Divina Misericordia, novenas, jaculatorias y rachas. | 🔴 **0%** | Actualmente `ComingSoonScreen`. Se requiere migrar runners interactivos y datasets estáticos (`prayers.ts`, `rosario-steps.ts`, etc.). |
| **Fase 5: Calendario Litúrgico y Santos** | Vista de calendario mensual litúrgico, santos del día, buscador de santos y fichas biográficas. | 🔴 **0%** | Actualmente `ComingSoonScreen`. Requiere consultas sobre `santos` y `misal_santos`. |
| **Fase 6: Favoritos, Ajustes y Utilidades** | Pantalla de favoritos con filtrado por tipo y tags, pantalla de ajustes, importación/exportación JSON y diagnóstico de base de datos. | 🔴 **10%** | Entidades Room y DataStore implementados; faltan pantallas Compose completas y lógica de backup/restore. |
