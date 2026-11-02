package com.jacopo.mygymtracker.ui.routinedetail

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class RoutineDetailUiState(
    val id: Int? = null,
    val name: String = "",
    val note: String? = null,
    val exercises: List<ExerciseUiState> = emptyList(),
    val catalogExercises: List<Exercise> = emptyList(),
    val isAddExercisesSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
)

/**
 * An exercise inside the routine being edited.
 *
 * @property exerciseId catalog id, or `null` for a new exercise typed in this screen.
 * @property key stable UI identity, unique even when the same catalog exercise appears twice.
 */
data class ExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val exerciseId: Int? = null,
    val key: String = Uuid.random().toString(),
    val name: String = "",
    val note: String? = null,
    val category: Category = Category.OTHER,
    val sets: List<SetUiState> = emptyList(),
)

data class SetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val uuid: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weightKg: Double = 0.0,
)

// region Mappers

fun Routine.toUiState(
    catalogExercises: List<Exercise> = emptyList(),
): RoutineDetailUiState = RoutineDetailUiState(
    id = id,
    name = name,
    note = note,
    exercises = exercises.map { it.toUiState() },
    catalogExercises = catalogExercises,
    isLoading = false,
)

fun RoutineExercise.toUiState(): ExerciseUiState = ExerciseUiState(
    exerciseId = exercise.id,
    name = exercise.name,
    note = exercise.note,
    category = exercise.category,
    sets = sets.map { SetUiState(reps = it.reps, weightKg = it.weightKg) },
)

/** A catalog exercise freshly added to the routine starts with three empty sets. */
fun Exercise.toUiState(): ExerciseUiState = ExerciseUiState(
    exerciseId = id,
    name = name,
    note = note,
    category = category,
    sets = List(3) { SetUiState() },
)

/** New exercises left without a name are dropped instead of polluting the catalog. */
fun RoutineDetailUiState.toDomain(): Routine = Routine(
    id = id,
    name = name.trim().ifEmpty { "Untitled Routine" },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    exercises = exercises
        .filter { it.exerciseId != null || it.name.isNotBlank() }
        .map { exercise ->
            RoutineExercise(
                exercise = Exercise(
                    id = exercise.exerciseId,
                    name = exercise.name.trim(),
                    category = exercise.category,
                    note = exercise.note,
                ),
                sets = exercise.sets.map { PlannedSet(reps = it.reps, weightKg = it.weightKg) },
            )
        },
)

// endregion