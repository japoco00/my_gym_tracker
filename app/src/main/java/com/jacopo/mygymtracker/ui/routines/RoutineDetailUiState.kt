package com.jacopo.mygymtracker.ui.routines

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** @property isClosed saving is done (or there was nothing to save), so the editor can close. */
data class RoutineDetailUiState(
    val id: Int? = null,
    val name: String = "",
    val note: String? = null,
    val exercises: List<RoutineExerciseUiState> = emptyList(),
    val catalogExercises: List<Exercise> = emptyList(),
    val isAddExercisesSheetOpen: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
    val isClosed: Boolean = false,
)

/**
 * An exercise inside the routine being edited.
 *
 * @property exerciseId catalog id, or `null` for a new exercise typed in this screen.
 * @property key stable UI identity, unique even when the same catalog exercise appears twice.
 */
data class RoutineExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val exerciseId: Int? = null,
    val key: String = Uuid.random().toString(),
    val name: String = "",
    val note: String? = null,
    val category: Category = Category.OTHER,
    val sets: List<PlannedSetUiState> = emptyList(),
)

data class PlannedSetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weightKg: Double = 0.0,
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
    sets = sets.map { PlannedSetUiState(reps = it.reps, weightKg = it.weightKg) },
)

/** A catalog exercise freshly added to the routine starts with three empty sets. */
fun Exercise.toUiState(): RoutineExerciseUiState = RoutineExerciseUiState(
    exerciseId = id,
    name = name,
    note = note,
    category = category,
    sets = List(3) { PlannedSetUiState() },
)

/**
 * The routine to save, or `null` when a new routine was left completely empty
 * (no name, no exercises) and there is nothing worth keeping.
 * New exercises left without a name are dropped instead of polluting the catalog.
 * A routine left without a name is saved as [untitledName].
 */
fun RoutineDetailUiState.toDomainOrNull(untitledName: String): Routine? {
    val namedExercises = exercises.filter { it.exerciseId != null || it.name.isNotBlank() }
    if (id == null && name.isBlank() && namedExercises.isEmpty()) return null
    return Routine(
        id = id,
        name = name.trim().ifEmpty { untitledName },
        note = note?.trim()?.takeIf { it.isNotEmpty() },
        exercises = namedExercises.map { it.toDomain() },
    )
}

private fun RoutineExerciseUiState.toDomain(): RoutineExercise = RoutineExercise(
    exercise = Exercise(id = exerciseId, name = name.trim(), category = category, note = note),
    sets = sets.map { PlannedSet(reps = it.reps, weightKg = it.weightKg) },
)

// endregion