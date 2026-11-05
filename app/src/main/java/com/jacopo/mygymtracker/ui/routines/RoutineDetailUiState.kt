package com.jacopo.mygymtracker.ui.routines

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.ui.components.EditingSet
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * @property id null until a new routine is first saved.
 * @property editingSet the set open in the editor sheet, or null.
 * @property editingRestKey the key of the exercise whose rest is being edited, or null.
 */
data class RoutineDetailUiState(
    val id: Int? = null,
    val name: String = "",
    val note: String? = null,
    val exercises: List<RoutineExerciseUiState> = emptyList(),
    val catalogExercises: List<Exercise> = emptyList(),
    val isAddExercisesSheetOpen: Boolean = false,
    val isNewExerciseSheetOpen: Boolean = false,
    val editingSet: EditingSet? = null,
    val editingRestKey: String? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = UserSettings.DEFAULT_REST_SECONDS,
    val isLoading: Boolean = true,
) {
    val setCount: Int get() = exercises.sumOf { it.sets.size }

    /** No name and no exercises: a new routine like this is not worth keeping. */
    val isBlank: Boolean get() = name.isBlank() && exercises.isEmpty()

    /**
     * A new exercise can't reuse a catalog name, or one already typed here:
     * saving would silently link it to that exercise instead.
     */
    fun isNameTaken(name: String): Boolean {
        val trimmed = name.trim()
        return catalogExercises.any { it.name.equals(trimmed, ignoreCase = true) } ||
            exercises.any { it.exerciseId == null && it.name.equals(trimmed, ignoreCase = true) }
    }
}

/**
 * An exercise inside the routine being edited.
 *
 * @property exerciseId catalog id, or `null` for a new exercise typed in this screen.
 * @property key stable UI identity, unique even when the same catalog exercise appears twice.
 * @property restSeconds rest after each set in this routine; null uses the default from the settings.
 */
data class RoutineExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val exerciseId: Int? = null,
    val key: String = Uuid.random().toString(),
    val name: String = "",
    val note: String? = null,
    val category: Category = Category.OTHER,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val isBarbell: Boolean = false,
    val sets: List<PlannedSetUiState> = emptyList(),
    val restSeconds: Int? = null,
)

data class PlannedSetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
)

// region Mappers

/** Fills the editor with [routine], keeping screen-level state such as the catalog and unit. */
fun RoutineDetailUiState.withRoutine(routine: Routine): RoutineDetailUiState = copy(
    id = routine.id,
    name = routine.name,
    note = routine.note,
    exercises = routine.exercises.map { it.toUiState() },
    isLoading = false,
)

fun RoutineExercise.toUiState(): RoutineExerciseUiState = RoutineExerciseUiState(
    exerciseId = exercise.id,
    name = exercise.name,
    note = exercise.note,
    category = exercise.category,
    trackingType = exercise.trackingType,
    isBarbell = exercise.isBarbell,
    sets = sets.map {
        PlannedSetUiState(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds)
    },
    restSeconds = restSeconds,
)

/** A catalog exercise freshly added to the routine starts with three empty sets. */
fun Exercise.toUiState(): RoutineExerciseUiState = RoutineExerciseUiState(
    exerciseId = id,
    name = name,
    note = note,
    category = category,
    trackingType = trackingType,
    isBarbell = isBarbell,
    sets = List(3) { PlannedSetUiState() },
)

/** The routine to save; one left without a name is saved as [untitledName]. */
fun RoutineDetailUiState.toDomain(untitledName: String): Routine = Routine(
    id = id,
    name = name.trim().ifEmpty { untitledName },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    exercises = exercises.map { it.toDomain() },
)

private fun RoutineExerciseUiState.toDomain(): RoutineExercise = RoutineExercise(
    exercise = Exercise(
        id = exerciseId,
        name = name.trim(),
        category = category,
        trackingType = trackingType,
        note = note,
        isBarbell = isBarbell,
    ),
    sets = sets.map { PlannedSet(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds) },
    restSeconds = restSeconds,
)

// endregion