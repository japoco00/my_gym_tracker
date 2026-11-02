# MyGymTracker - AI Agent Guidelines

## 1. Project Overview & Tech Stack
- **Platform**: Android (Min SDK 26, Compile/Target SDK 36, JVM 17)
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3; charts with Vico (`compose-m3`)
- **Architecture**: Clean layered MVVM (`domain`, `data`, `di`, feature-organized `ui`) with Manual Dependency Injection
- **Persistence**: Room (SQLite) for routines and workouts; Preferences DataStore for user settings
- **Navigation**: Jetpack Navigation Compose with Kotlin Serialization (`kotlinx.serialization`) type-safe routes
- **Concurrency**: Kotlin Coroutines & `Flow` / `StateFlow`
- **Versions**: `gradle/libs.versions.toml` is the single source. Vico tracks recent Compose releases (it drove the BOM bump to 2025.10.01): upgrade Vico and the Compose BOM together.

---

## 2. Package Structure

```text
com.jacopo.mygymtracker
├── MyApplication.kt      # Owns AppContainer; creates notification channels
├── MainActivity.kt       # Single-activity Compose host
├── di/                   # AppContainer + CreationExtras.appContainer()
├── domain/
│   ├── model/            # Pure Kotlin business models
│   └── progress/         # Pure progress math: e1RM, volume, personal records
├── data/
│   ├── local/            # AppDatabase, catalog seed, dao/, entity/ (tables, @Relation POJOs, query rows)
│   ├── mapper/           # Entity <-> Domain extensions
│   └── repository/       # Repositories exposing Domain models & Flows (Room, DataStore)
├── resttimer/            # Rest timer: controller, foreground service, notifications
└── ui/
    ├── MyGymTrackerApp.kt  # App shell: bottom bar + AppNavHost
    ├── components/       # Shared, feature-agnostic composables
    ├── format/           # Display formatting: dates, durations, weights, enum labels
    ├── navigation/       # AppNavHost, TopLevelDestination, one *Navigation.kt per feature
    ├── <feature>/        # routines, routinedetail, workout, history, progress, exercises, settings
    └── theme/            # Material 3 Theme, Color, Typography
```

---

## 3. Dependency Injection Policy
- **Manual DI via `AppContainer`**: All app-scoped singletons (database, repositories, settings DataStore, `RestTimerController`) are lazily created inside [`AppContainer`](class://com.jacopo.mygymtracker.di.AppContainer), held by [`MyApplication`](class://com.jacopo.mygymtracker.MyApplication). Its `applicationScope` runs work that must outlive a screen.
- **No external DI frameworks**: keep all wiring in `AppContainer`; do NOT introduce Hilt, Dagger, Koin, or similar DI libraries.
- **ViewModel Creation**: Each ViewModel's `companion object` exposes a `Factory`, or `factory(id)` when it needs a route argument, built with `viewModelFactory { initializer { ... } }` and resolving dependencies via `CreationExtras.appContainer()`. Repositories reach ViewModels only through the container.

---

## 4. Data & Persistence Guidelines

### 4.1 Strict Three-Tier Model Separation
1. **`data/local/entity/*.kt`**: Room entities (`@Entity` with explicit `tableName`), `@Relation` POJOs and query result rows. One type per file.
2. **`domain/`**: Pure, immutable Kotlin models and logic (`val` properties, `List<T>`). Imports stay within Kotlin and `java.*`: no `android.*`, `androidx.room.*` or `androidx.compose.*`.
3. **`data/mapper/EntityMappers.kt`**: Bidirectional extension functions (`toEntity()`, `toDomain()`) live in the `data` layer so `domain` never depends on `data`.
4. **`ui/<feature>/`**: Immutable presentation state (`*UiState`) consumed by Compose screens.

### 4.2 Relational Model & Catalog Rules
- **Exercise Catalog**: [`ExerciseEntity`](class://com.jacopo.mygymtracker.data.local.entity.ExerciseEntity) is the global exercise catalog, unique by name ignoring case. Routines and workouts reference it; it never holds a `routineId`.
- **Ownership cascades**: `routines → routine_exercises → planned_sets` and `workout_sessions → workout_exercises → logged_sets` use `onDelete = CASCADE`.
- **Catalog references RESTRICT**: `routine_exercises.exerciseId` and `workout_exercises.exerciseId` block deleting an exercise still in use; `ExerciseRepository` checks first so the UI can explain why.
- **History outlives routines**: `workout_sessions.routineId` is `SET_NULL` and `routineName` is a snapshot taken at start.
- **Ordering**: Ordered children store a `position`. `@Relation` results come back unordered, so mappers sort by `position`; on save, list order becomes `position`.
- **Indexing**: Index every foreign key column (`indices = [Index("...")]`).

### 4.3 Schema & Migrations
- Version 4 is the baseline; versions 1-3 are dropped destructively (development data only).
- Every entity change from version 4 on: bump the version in [`AppDatabase`](class://com.jacopo.mygymtracker.data.local.AppDatabase), ship a `Migration` (or `@AutoMigration`), and commit the exported schema JSON in `app/schemas/`. User history must survive app updates.
- The default exercise catalog is seeded once by `CatalogSeedCallback.onCreate`.

### 4.4 Settings & Units
- `SettingsRepository` wraps Preferences DataStore and exposes `Flow<UserSettings>`.
- Weights are stored in kilograms everywhere. Convert only at the UI edge through `WeightUnit` (`format`, `fromKg`, `toKg`).

---

## 5. UI & State Architecture

### 5.1 Unidirectional Data Flow & Immutability
- All UI state classes must be **immutable `data class`es** (`val` properties and read-only `List<T>`).
- ViewModels expose `StateFlow<UiState>`, collected in navigation destinations via `collectAsStateWithLifecycle()`. Derive it from repository flows with `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`; keep screen-only state in a private `MutableStateFlow` changed via `update { it.copy(...) }`.
- Pass plain values down and event lambdas up; Composables never receive `MutableState`:
  ```kotlin
  TextField(
      value = uiState.name,
      onValueChange = onNameChange,
  )
  ```

### 5.2 Compose Conventions
- **Feature Co-location**: Group each screen's `*Screen.kt`, `*ViewModel.kt`, `*UiState` and feature-specific components inside `ui/<feature>/`. Place reusable widgets shared by multiple features in `ui/components/`.
- **Stateless Screens**: `*Screen` composables accept `uiState`, event callbacks, and `modifier: Modifier = Modifier`, never a ViewModel instance.
- **Strings**: All user-visible text lives in `res/values/strings.xml`, read with `stringResource` / `pluralStringResource`. Formatting of dates, durations and weights goes through `ui/format/`.
- **Scaffolds**: Each screen owns its `Scaffold` and top bar. `MyGymTrackerApp` shows the bottom `NavigationBar` only on tab roots (`TopLevelDestination`), so detail screens are full screen.
- **Previews**: Include private `@Preview` composables wrapped in `AppTheme` with realistic sample data.

---

## 6. Navigation Patterns
- Use **type-safe routes** with `kotlinx.serialization` (e.g., `@Serializable data object RoutinesRoute`, `@Serializable data class RoutineDetailRoute(val routineId: Int? = null)`). Use nullable parameters (`Int? = null`) rather than magic sentinel integers like `-1`.
- Each `ui/navigation/<Feature>Navigation.kt` provides:
  - Route declaration (`@Serializable`).
  - `NavGraphBuilder.<feature>Destination(...)`: creates the ViewModel, collects state, and exposes navigation as lambdas wired in `AppNavHost`.
  - `NavController.navigateTo<Feature>(...)`.
- **Tabs**: Switch tabs only through `navigateToTopLevelDestination`, which keeps one copy per tab and saves/restores each tab's back stack.
- **Closing screens**: Close a screen with `popBackStack<Route>(inclusive = true)`, so a double Back or a close racing with Back can't pop the screen below. This relies on **acyclic routes**: at most one entry per route type on the back stack. Keep links pointing forward (e.g. Workout detail → Progress); a link back to an earlier route type needs a different close strategy first.

---

## 7. Rest Timer
- [`RestTimerController`](class://com.jacopo.mygymtracker.resttimer.RestTimerController) is the single source of truth: an app-scoped `StateFlow` countdown based on wall-clock end time, living in `AppContainer`.
- `RestTimerService` is a `specialUse` foreground service that only mirrors the controller in a notification (chronometer, +15 s and Skip actions) and holds a partial wake lock until the rest ends. It stops itself when the rest is over.
- Notification channels are created in `MyApplication`; `POST_NOTIFICATIONS` is requested when a workout opens. Without it the timer still runs in-app.

---

## 8. Testing
- JVM unit tests live in `app/src/test`; run them with `./gradlew :app:testDebugUnitTest`.
- **Domain logic** (`domain/`): plain JUnit, no Android.
- **Room repositories**: Robolectric (`@RunWith(AndroidJUnit4::class)`) against an in-memory database from `createTestDatabase()` (no catalog seed, so each test owns its data).
- **`SettingsRepository`**: plain JUnit with an in-memory fake `DataStore`.
- **Time-based code** (`RestTimerController`): `runTest` with virtual time (`advanceTimeBy`).
- Add or update a test with every repository query, domain rule or controller behaviour change.

---

## 9. Language & Code Style
- **English Only**: All code, identifiers, KDoc, commit messages, and comments MUST be written in English. Translate any Italian comments encountered.
- Prefer idiomatic Kotlin: immutable data classes, trailing commas, expression bodies where clear, and `Flow` operators (`combine`, `map`, `stateIn`).
