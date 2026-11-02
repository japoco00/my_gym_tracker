package com.jacopo.mygymtracker.ui.history

import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.progress.volumeKg
import java.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * @property workout the finished workout as stored, or `null` while loading.
 * @property editedExercises the unsaved changes while editing, `null` otherwise.
 * @property isClosed the workout was deleted or doesn't exist: leave the screen.
 */
data class WorkoutDetailUiState(
    val workout: WorkoutSession? = null,
    val editedExercises: List<EditableExerciseUiState>? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isDeleteDialogOpen: Boolean = false,
    val isClosed: Boolean = false,
) {
    val isEditing: Boolean = editedExercises != null

    /** A workout keeps at least one set: deleting all of them means deleting the workout. */
    val canSave: Boolean = editedExercises.orEmpty().any { it.sets.isNotEmpty() }

    val durationSeconds: Long = workout?.finishedAt?.let { Duration.between(workout.startedAt, it).seconds } ?: 0
    val setCount: Int = workout?.exercises?.sumOf { it.sets.size } ?: 0
    val volumeKg: Double = workout?.exercises?.sumOf { it.sets.volumeKg() } ?: 0.0
}

/** An exercise being edited. [key] is a stable UI identity for list keys and callbacks. */
data class EditableExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val exercise: Exercise,
    val sets: List<EditableSetUiState> = emptyList(),
)

data class EditableSetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
)

// region Mappers

fun WorkoutExercise.toEditableUiState(): EditableExerciseUiState = EditableExerciseUiState(
    exercise = exercise,
    sets = sets.map { EditableSetUiState(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds) },
)

fun EditableExerciseUiState.toDomain(): WorkoutExercise = WorkoutExercise(
    exercise = exercise,
    sets = sets.map {
        LoggedSet(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds, isCompleted = true)
    },
)

// endregion
