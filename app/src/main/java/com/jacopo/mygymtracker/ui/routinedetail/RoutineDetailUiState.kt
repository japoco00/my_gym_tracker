package com.jacopo.mygymtracker.ui.routinedetail

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseSet
import com.jacopo.mygymtracker.domain.model.Routine
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

data class ExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val draftId: String = Uuid.random().toString(),
    val name: String = "",
    val note: String? = null,
    val category: Category = Category.STRENGTH,
    val sets: List<SetUiState> = emptyList(),
) {
    /** Stable identifier across recompositions for both persisted and draft exercises. */
    val key: String
        get() = id?.let { "id_$it" } ?: "draft_$draftId"
}

data class SetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Int? = null,
    val uuid: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weight: Int = 0,
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

fun Exercise.toUiState(): ExerciseUiState = ExerciseUiState(
    id = id,
    name = name,
    note = note,
    category = category,
    sets = if (sets.isEmpty()) {
        listOf(SetUiState(), SetUiState(), SetUiState())
    } else {
        sets.map { it.toUiState() }
    },
)

fun ExerciseSet.toUiState(): SetUiState = SetUiState(
    id = id,
    reps = reps,
    weight = weight,
)

fun RoutineDetailUiState.toDomain(): Routine = Routine(
    id = id,
    name = name.trim().ifEmpty { "Untitled Routine" },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    exercises = exercises.map { exercise ->
        Exercise(
            id = exercise.id,
            name = exercise.name.trim().ifEmpty { "Unnamed Exercise" },
            note = exercise.note,
            category = exercise.category,
            sets = exercise.sets.map { set ->
                ExerciseSet(
                    id = set.id,
                    reps = set.reps,
                    weight = set.weight,
                )
            },
        )
    },
)

// endregion
