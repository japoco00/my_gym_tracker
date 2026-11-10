# MyGymTracker - AI Agent Guidelines

## 1. Project Overview & Tech Stack
- **Platform**: Android (Min SDK 26, Compile SDK 37, Target SDK 36, JVM 17)
- **Language**: Kotlin 2.4 via AGP 9 built-in Kotlin (no `kotlin-android` plugin)
- **UI Toolkit**: Jetpack Compose with Material 3 (Expressive APIs); charts with Vico (`compose-m3`)
- **Architecture**: Clean layered MVVM (`domain`, `data`, `di`, feature-organized `ui`) with Manual Dependency Injection
- **Persistence**: Room (SQLite) for routines and workouts; Preferences DataStore for user settings
- **Navigation**: Jetpack Navigation Compose with Kotlin Serialization (`kotlinx.serialization`) type-safe routes
- **Concurrency**: Kotlin Coroutines & `Flow` / `StateFlow`
- **Versions**: `gradle/libs.versions.toml` is the single source.
  - `material3` is pinned above the Compose BOM (1.5 beta) for the Expressive components; it requires Compile SDK 37. Drop the pin once the BOM ships a material3 with those APIs.
  - Vico tracks recent Compose and Kotlin releases: upgrade Vico, the Compose BOM and Kotlin together, keeping the BOM at least as new as the one Vico requires.

---

## 2. Package Structure

```text
com.jacopo.mygymtracker
├── MyApplication.kt      # Owns AppContainer; creates notification channels
├── MainActivity.kt       # Single-activity Compose host; applies the theme from MainViewModel
├── MainViewModel.kt      # App-wide settings the activity needs (theme)
├── di/                   # AppContainer + CreationExtras.appContainer()
├── domain/
│   ├── history/          # Pure history math: workouts grouped by week, week streaks
│   ├── home/             # Pure Home logic: greeting, up-next routine, stats for the tiles
│   ├── model/            # Pure Kotlin business models
│   ├── plates/           # Pure plate math: which plates to load on each side
│   ├── progress/         # Pure progress math: e1RM, volume, personal records
│   └── warmup/           # Pure warm-up math: ramp-up sets towards a working weight
├── data/
│   ├── backup/           # @Serializable DTOs: the backup file format
│   ├── local/            # AppDatabase, catalog seed, dao/, entity/ (tables, @Relation POJOs, query rows)
│   ├── mapper/           # Entity <-> Domain and Backup <-> Domain extensions
│   └── repository/       # Repositories exposing Domain models & Flows (Room, DataStore)
├── resttimer/            # Rest timer: controller, foreground service, notifications
└── ui/
    ├── MyGymTrackerApp.kt  # App shell: bottom bar + AppNavHost
    ├── components/       # Shared, feature-agnostic composables
    ├── format/           # Display formatting: dates, durations, weights, enum labels
    ├── navigation/       # AppNavHost, TopLevelDestination, one *Navigation.kt per feature
    ├── <feature>/        # home, routines, workout, history, progress, bodyweight, exercises, settings (list + detail screens together)
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
- **Rest per exercise**: `routine_exercises.restSeconds` and `workout_exercises.restSeconds` are nullable; `null` means the settings default. A workout copies the routine's value at start; workout edits reach the routine only through "update routine" after finishing.
- **Warm-up sets never count**: they're left out of volume, personal records and progress charts. In Kotlin go through `domain/progress` (`volumeKg()`, the record and progress functions); SQL aggregates such as `WorkoutDao`'s history volume must skip `type = 'WARM_UP'` themselves.
- **Writing exercise lists**: Replace a routine's or a workout's exercises only through the `RoutineDao.writeExercises` / `WorkoutDao.writeExercises` extensions, shared by the repositories and the backup import.
- **Ordering**: Ordered children store a `position`. `@Relation` results come back unordered, so mappers sort by `position`; on save, list order becomes `position`.
- **Indexing**: Index every foreign key column (`indices = [Index("...")]`).

### 4.3 Schema & Migrations
- Version 4 is the baseline; versions 1-3 are dropped destructively (development data only).
- Every entity change from version 4 on: bump the version in [`AppDatabase`](class://com.jacopo.mygymtracker.data.local.AppDatabase), ship a `Migration` (or `@AutoMigration`) with a `MigrationsTest` case, and commit the exported schema JSON in `app/schemas/`. User history must survive app updates.
- The default exercise catalog is seeded once by `CatalogSeedCallback.onCreate`.

### 4.4 Settings & Units
- `SettingsRepository` wraps Preferences DataStore and exposes `Flow<UserSettings>`.
- Weights are stored in kilograms everywhere. Convert only at the UI edge through `WeightUnit` (`format`, `fromKg`, `toKg`).
- Owned plates are stored per unit (`UserSettings.plates`). `domain/plates/platesPerSide` splits a weight greedily into plates for each side (weight / 2, no bar weight); the set editor shows it for exercises flagged `isBarbell`.
- Enums persisted by name (`WeightUnit`, `ThemeMode` in settings; `SetType` in the database and SQL): never rename an entry.
- Theme: `MainActivity` reads `ThemeMode` and dynamic colour from `MainViewModel` and holds the first frame until settings load, so the app never draws in the wrong theme. It also re-applies `enableEdgeToEdge` so the system bar icons follow the app theme, not the system's.
- Workout settings are read by `WorkoutViewModel` into `WorkoutUiState`: `autoStartRest` (off: each exercise shows a start button) and `keepScreenOn` (`Modifier.keepScreenOn()` on the workout screen, no wake lock).

### 4.5 Backup
- `BackupRepository` exports settings, the catalog, routines and finished workouts to one JSON `BackupFile`, and imports it back. Import validates the whole file, then replaces everything in one transaction; it refuses while a workout is in progress.
- The `@Serializable` DTOs in `data/backup/` are the file format, mapped in `data/mapper/BackupMappers.kt`. Exercises are referenced by name, routines by a file-local id.
- When you add a persisted field (entity column or setting), add it to the DTOs too, or backups silently drop it, and fill it in `BackupRepositoryTest.fill()` so the round-trip test covers it. When the DTOs change, bump `BackupFile.CURRENT_FORMAT_VERSION` and give new fields a default so older files still import.
- The JSON is written with `encodeDefaults = true`: every value is in the file, so changing a default later never changes what a restore brings back.

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
- **One-off events** (navigate, show a snackbar) are state too: the ViewModel sets a value, a `LaunchedEffect` keyed on it acts, then calls back to clear it (`onWorkoutOpened`, `onBackupMessageShown`).

### 5.2 Compose Conventions
- **Feature Co-location**: Group each screen's `*Screen.kt`, `*ViewModel.kt`, `*UiState` and feature-specific components inside `ui/<feature>/`. Place reusable widgets shared by multiple features in `ui/components/`.
- **Stateless Screens**: `*Screen` composables accept `uiState`, event callbacks, and `modifier: Modifier = Modifier`, never a ViewModel instance.
- **Strings**: All user-visible text lives in `res/values/strings.xml`, read with `stringResource` / `pluralStringResource`. Formatting of dates, durations and weights goes through `ui/format/`.
- **Scaffolds**: Each screen owns its `Scaffold` and top bar, built with the shared `ui/components/AppHeader` (big collapsing title, `Pill` facts, rounded `surfaceContainer` block). The one exception is Home, whose poster greeting is the header. `MyGymTrackerApp` shows the bottom `ShortNavigationBar` only on tab roots (`TopLevelDestination`), so detail screens are full screen.
- **Shared building blocks** (`ui/components/`): every screen follows the workout screen's Material 3 Expressive style. Reuse these instead of writing a feature-local variant:
  - `AppHeader`: a `String` title, or a composable `(expanded) -> Unit` title for custom content such as the routine editor's editable name. Expanded titles use `Typography.headerTitle`.
  - `Pill` for header facts (`Pill(icon, text)` for the usual icon + text), `SectionLabel` for list section titles, `ShapeBadge` for a short value + label inside a `MaterialShapes` shape in `AppHeader`'s actions (stays visible when collapsed). Animate only the shape through its `shapeModifier`.
  - `BackButton` leaves a detail screen; `EmptyStateText` is the grey line of an empty screen or list.
  - `PrTag`: the personal-record star tag, shared by the live workout and history.
  - `SetRow` + `SetNumber`: the one set row for workouts, history and routines; tapping an editable row opens `SetEditorSheet`.
  - `ExerciseTitle` heads every exercise section (name over a subtitle, trailing slot for its menu); `AddSetButton` and `AddExercisesButton` are the add actions under sets and at the end of the list.
  - `AppBottomSheet` wraps every modal sheet: it opens fully expanded, and its `hideThen` slides the sheet away before running an action.
  - `SetEditorSheet`: the only set editor (`RepsField` / `WeightField` / `DurationField` with steppers). Passing `isCompleted = null` gives plan mode (routines: no completion toggle, no previous values). Pass `plates` for barbell exercises to show the plates per side. Pass `onRpeChange` only for logged sets (workouts, history), never for a plan.
  - `RestChip` + `RestEditorSheet` show and edit an exercise's rest (`null` = the settings default) in the routine editor and the workout.
  - `ExerciseEditorSheet` creates or edits a catalog exercise. Pass `isNameTaken` so duplicate names can't be saved.
  - `AddExercisesSheet` picks catalog exercises; `DestructiveOverflowMenu` ends with one destructive action, other `items` above it; `SwipeToDeleteContainer` wraps swipe-to-delete rows.
  - `DateLineChart`: the one line chart over dates (exercise progress, body weight).
  - `ActiveWorkoutCard` (resume the workout in progress) and `StartConflictDialog` (starting while one is in progress), on Home and Routines. Their ViewModels share the start logic through `ui/workout/WorkoutStarter`.
- **Editors save as you go**: the routine editor has no Save button. Its ViewModel debounces each edit into a save and flushes pending edits in `onCleared` on the app scope, so Back (and predictive back) never loses work.
- **Motion**: Take specs from `MaterialTheme.motionScheme` (`*SpatialSpec` for position/size, `*EffectsSpec` for alpha/color) instead of hand-written tweens; a hand-timed sequence like the finish celebration is the exception, and so are endless decorative loops (Home's morphing shapes), since springs can't repeat. Pair state changes with `LocalHapticFeedback` using the M3 types (`ToggleOn`/`ToggleOff`, `Confirm`, `GestureThresholdActivate`), and fire them from the user's action, not from restored state.
- **Previews**: Include private `@Preview` composables wrapped in `AppTheme` with realistic sample data.

---

## 6. Navigation Patterns
- Use **type-safe routes** with `kotlinx.serialization` (e.g., `@Serializable data object RoutinesRoute`, `@Serializable data class RoutineDetailRoute(val routineId: Int? = null)`). Use nullable parameters (`Int? = null`) rather than magic sentinel integers like `-1`.
- Each `ui/navigation/<Feature>Navigation.kt` provides:
  - Route declaration (`@Serializable`).
  - `NavGraphBuilder.<feature>Destination(...)`: creates the ViewModel, collects state, holds Activity Result launchers (e.g. the backup file pickers, so the screen stays previewable) and exposes navigation as lambdas wired in `AppNavHost`.
  - `NavController.navigateTo<Feature>(...)`.
- **Tabs**: Home (the start destination), Routines, History, Exercises. Settings is a pushed screen opened from Home's avatar. Switch tabs only through `navigateToTopLevelDestination`, which keeps one copy per tab and saves/restores each tab's back stack.
- **Closing screens**: Close a screen with `popBackStack<Route>(inclusive = true)`, so a double Back or a close racing with Back can't pop the screen below. This relies on **acyclic routes**: at most one entry per route type on the back stack. Keep links pointing forward (e.g. Workout detail → Progress); a link back to an earlier route type needs a different close strategy first.
- **Transitions**: `AppNavHost` owns the defaults: pushes slide horizontally with a fade, tab switches only fade. A destination overrides them through `composable<Route>(enterTransition = …)`; return `null` to fall back to the defaults.
- **Shared bounds**: `AppNavHost` wraps the `NavHost` in `SharedTransitionLayout` and provides `LocalSharedTransitionScope`. A destination taking part provides `LocalNavAnimatedVisibilityScope` and only fades, while both ends apply `Modifier.sharedContainer(key)` with the same key (a saved routine's row grows into its editor). Both locals live in `ui/components/`, so screens never import `ui/navigation`; without them the modifier does nothing, which keeps previews working.

---

## 7. Rest Timer
- [`RestTimerController`](class://com.jacopo.mygymtracker.resttimer.RestTimerController) is the single source of truth: an app-scoped `StateFlow` countdown based on wall-clock end time, living in `AppContainer`.
- `RestTimerService` is a `specialUse` foreground service that only mirrors the controller in a notification (chronometer, +15 s and Skip actions) and holds a partial wake lock until the rest ends. It stops itself when the rest is over.
- Notification channels are created in `MyApplication`; `POST_NOTIFICATIONS` is requested when a workout opens. Without it the timer still runs in-app.
- The rest-over sound and vibration belong to its notification channel: Settings > "Rest alert" opens the channel's system page (`RestTimerNotifications.restOverSettingsIntent`). Don't add an in-app sound setting; the channel's would override it.

---

## 8. Testing
- JVM unit tests live in `app/src/test`. CI (`.github/workflows/ci.yml`) runs unit tests, lint and a debug build on every pull request; run its Gradle command locally before pushing.
- **Domain logic** (`domain/`): plain JUnit, no Android.
- **Room repositories**: Robolectric (`@RunWith(AndroidJUnit4::class)`) against an in-memory database from `createTestDatabase()` (no catalog seed, so each test owns its data).
- **Migrations**: `MigrationsTest` (Robolectric + `MigrationTestHelper`) creates the old version from `app/schemas/`, inserts rows, runs `runMigrationsAndValidate` and checks the data survived.
- **`SettingsRepository`**: plain JUnit with `InMemoryPreferencesDataStore`.
- **Time-based code** (`RestTimerController`): `runTest` with virtual time (`advanceTimeBy`).
- **ViewModels**: Robolectric with real repositories over `createTestDatabase()`. `MainDispatcherRule` puts `viewModelScope` on `runTest`'s virtual clock. Create them through a `ViewModelStore`, whose `clear()` plays leaving the screen (`onCleared`). Room writes on its own threads, outside the test scheduler: wait for results with `uiState.first { … }` or a Room flow's `first { … }`.
- Add or update a test with every repository query, domain rule, controller or ViewModel behaviour change.

---

## 9. Language & Code Style
- **English Only**: All code, identifiers, KDoc, commit messages, and comments MUST be written in English. Translate any Italian comments encountered.
- Prefer idiomatic Kotlin: immutable data classes, trailing commas, expression bodies where clear, and `Flow` operators (`combine`, `map`, `stateIn`).
