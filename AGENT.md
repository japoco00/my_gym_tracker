# MyGymTracker - AI Agent Guidelines

## 1. Project Overview & Tech Stack
- **Platform**: Android (Min SDK 26, Compile/Target SDK 36, JVM 17)
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3
- **Architecture**: Clean layered MVVM (`domain`, `data`, `di`, feature-organized `ui`) with Manual Dependency Injection
- **Database**: Room Database with SQLite
- **Navigation**: Jetpack Navigation Compose with Kotlin Serialization (`kotlinx.serialization`) type-safe routes
- **Concurrency**: Kotlin Coroutines & `Flow` / `StateFlow`

---

## 2. Package Structure

```text
com.jacopo.mygymtracker
├── MyApplication.kt               # Application entry point owning AppContainer
├── MainActivity.kt                # Single-activity Compose host
├── di/
│   └── AppContainer.kt            # Manual DI container + CreationExtras.appContainer()
├── domain/
│   └── model/                     # Pure Kotlin business models (no Android/Room imports)
│       ├── Routine.kt
│       ├── Exercise.kt
│       └── Category.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/                   # Room @Dao interfaces
│   │   └── entity/                # Room @Entity tables, CrossRefs, and @Relation POJOs
│   ├── mapper/
│   │   └── EntityMappers.kt       # Entity <-> Domain conversion extensions
│   └── repository/                # Repositories exposing Domain models & Flows
│       ├── RoutineRepository.kt
│       └── ExerciseRepository.kt
└── ui/
    ├── components/                # Shared, feature-agnostic Compose components
    ├── navigation/                # AppNavHost + type-safe route definitions
    ├── routines/                  # Feature: Routines list (Screen, ViewModel, UiState, Card)
    ├── routinedetail/             # Feature: Routine editor (Screen, ViewModel, UiState, Modal)
    └── theme/                     # Material 3 Theme, Color, Typography
```

---

## 3. Dependency Injection Policy
- **Manual DI via `AppContainer`**: All app-scoped singletons (database, repositories) are lazily created inside [`AppContainer`](class://com.jacopo.mygymtracker.di.AppContainer), held by [`MyApplication`](class://com.jacopo.mygymtracker.MyApplication).
- **No external DI frameworks**: Do NOT introduce Hilt, Dagger, Koin, or similar DI libraries.
- **ViewModel Creation**: Define a `Factory` inside each ViewModel's `companion object` using `viewModelFactory { initializer { ... } }` and resolve dependencies via `CreationExtras.appContainer()`. Do not pass repositories through Composable or Navigation parameters.

---

## 4. Data & Persistence Guidelines (Room)

### 4.1 Strict Three-Tier Model Separation
1. **`data/local/entity/*Entity.kt`**: Room entities only (`@Entity` with explicit `tableName`). One entity/relation per file.
2. **`domain/model/*.kt`**: Pure, immutable Kotlin domain models (`val` properties, `List<T>`). Never import `androidx.room.*` or `androidx.compose.*` in `domain/`.
3. **`data/mapper/EntityMappers.kt`**: Bidirectional extension functions (`toEntity()`, `toDomain()`) live in the `data` layer so `domain` never depends on `data`.
4. **`ui/<feature>/*UiState.kt`**: Immutable presentation state models consumed by Compose screens.

### 4.2 Relational Model & Catalog Rules
- **Exercise Catalog**: [`ExerciseEntity`](class://com.jacopo.mygymtracker.data.local.entity.ExerciseEntity) is a reusable, global exercise catalog. It must NEVER hold a `routineId`.
- **Routines & Exercises**: Linked via [`RoutineExerciseCrossRef`](class://com.jacopo.mygymtracker.data.local.entity.RoutineExerciseCrossRef) (`routineId`, `exerciseId`).
- **Foreign Keys & Indexing**: Always declare explicit Room `ForeignKey` constraints (`onDelete = ForeignKey.CASCADE` where appropriate) and index every foreign key column (`indices = [Index("...")]`).
- **Database Migrations**: Bump the schema version in [`AppDatabase`](class://com.jacopo.mygymtracker.data.local.AppDatabase) when entities change. `.fallbackToDestructiveMigration(dropAllTables = true)` is used during active development.

---

## 5. UI & State Architecture

### 5.1 Unidirectional Data Flow & Immutability
- All UI state classes must be **immutable `data class`es** (`val` properties and read-only `List<T>`).
- ViewModels expose `StateFlow<UiState>` collected in navigation destinations via `collectAsStateWithLifecycle()`.
- Mutate state exclusively via `_uiState.update { it.copy(...) }` inside ViewModel intent functions.
- Never pass `MutableState` into Composables; always pass plain values down and event lambdas up:
  ```kotlin
  TextField(
      value = uiState.name,
      onValueChange = onNameChange,
  )
  ```

### 5.2 Compose Conventions
- **Feature Co-location**: Group each screen's `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt`, and feature-specific components inside `ui/<feature>/`. Place reusable widgets shared by multiple features in `ui/components/`.
- **Stateless Screens**: `*Screen` composables accept `uiState`, event callbacks, and `modifier: Modifier = Modifier` — never a ViewModel instance.
- **Previews**: Include private `@Preview` composables wrapped in `AppTheme` with realistic sample data.

---

## 6. Navigation Patterns
- Use **type-safe routes** with `kotlinx.serialization` (e.g., `@Serializable data object RoutinesRoute`, `@Serializable data class RoutineDetailsRoute(val routineId: Int? = null)`). Use nullable parameters (`Int? = null`) rather than magic sentinel integers like `-1`.
- Each destination file in `ui/navigation/` provides:
  - Route declaration (`@Serializable`).
  - `NavGraphBuilder.<feature>Destination(...)` wiring the ViewModel and lifecycle-aware state collection.
  - `NavController.navigateTo<Feature>(...)`.

---

## 7. Language & Code Style
- **English Only**: All code, identifiers, KDoc, commit messages, and comments MUST be written in English. Translate any Italian comments encountered.
- Prefer idiomatic Kotlin: immutable data classes, trailing commas, expression bodies where clear, and `Flow` operators (`combine`, `map`, `stateIn`).
