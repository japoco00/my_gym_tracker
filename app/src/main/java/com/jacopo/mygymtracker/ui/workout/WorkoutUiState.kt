package com.jacopo.mygymtracker.ui.workout

import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.progress.prSetIndex
import com.jacopo.mygymtracker.resttimer.RestTimer
import com.jacopo.mygymtracker.ui.components.EditingSet
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * @property previousSets sets performed last time, by catalog exercise id, shown as a reference.
 * @property earlierSets sets of every finished workout before this one, by catalog exercise id: the records to beat.
 * @property restTimer the running rest between sets, or null.
 * @property editingSet the set open in the editor sheet, or null.
 * @property editingRestKey the key of the exercise whose rest is being edited, or null.
 * @property plates the plates owned in [weightUnit], for the set editor of barbell exercises.
 * @property exit set once the screen should close, saying why.
 */
data class WorkoutUiState(
    val routineId: Int? = null,
    val routineName: String = "",
    val startedAt: Instant = Instant.EPOCH,
    val exercises: List<WorkoutExerciseUiState> = emptyList(),
    val previousSets: Map<Int, List<LoggedSet>> = emptyMap(),
    val earlierSets: Map<Int, List<LoggedSet>> = emptyMap(),
    val catalogExercises: List<Exercise> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = UserSettings.DEFAULT_REST_SECONDS,
    val plates: List<Double> = WeightUnit.KG.defaultPlates,
    val restTimer: RestTimer? = null,
    val dialog: WorkoutDialog? = null,
    val isAddExercisesSheetOpen: Boolean = false,
    val editingSet: EditingSet? = null,
    val editingRestKey: String? = null,
    val isLoading: Boolean = true,
    val exit: WorkoutExit? = null,
) {
    val totalSets: Int = exercises.sumOf { it.sets.size }
    val completedSets: Int = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }

    /** Ticked sets only, so it matches the volume History shows once the workout is saved. */
    val completedVolumeKg: Double = exercises.sumOf { exercise ->
        exercise.sets.filter { it.isCompleted }.sumOf { it.reps * it.weightKg }
    }

    /** Keys of the ticked sets that are personal records, at most one per exercise, as History will star them. */
    val prSetKeys: Set<String> = exercises.mapNotNullTo(HashSet()) { exercise ->
        val ticked = exercise.sets.filter { it.isCompleted }
        val earlier = earlierSets[exercise.exercise.id].orEmpty()
        prSetIndex(ticked.map { it.toLoggedSet() }, earlier, exercise.exercise.trackingType)?.let { ticked[it].key }
    }
}

enum class WorkoutExit {
    /** Saved to history: show its summary. */
    FINISHED,

    /** Discarded, or no longer in progress: just go back. */
    CLOSED,
}

/**
 * An exercise of the workout. [key] is a stable UI identity for list keys and callbacks.
 * [restSeconds] is the rest after each of its sets; null uses the default from the settings.
 */
data class WorkoutExerciseUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val exercise: Exercise,
    val sets: List<WorkoutSetUiState> = emptyList(),
    val restSeconds: Int? = null,
)

data class WorkoutSetUiState @OptIn(ExperimentalUuidApi::class) constructor(
    val key: String = Uuid.random().toString(),
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
    val isCompleted: Boolean = false,
)

sealed interface WorkoutDialog {
    /** Finishing drops the [uncheckedSets] that were never completed. */
    data class ConfirmFinish(val uncheckedSets: Int) : WorkoutDialog

    /** Finish was requested without any completed set: nothing would be saved. */
    data object NothingCompleted : WorkoutDialog

    data object ConfirmDiscard : WorkoutDialog

    data class ConfirmRemoveExercise(val exerciseKey: String, val name: String) : WorkoutDialog

    /** The workout is finished and differs from its routine's plan. */
    data object UpdateRoutine : WorkoutDialog
}

// region Mappers

fun WorkoutUiState.withWorkout(
    workout: WorkoutSession,
    previousSets: Map<Int, List<LoggedSet>>,
    earlierSets: Map<Int, List<LoggedSet>>,
): WorkoutUiState = copy(
    routineId = workout.routineId,
    routineName = workout.routineName,
    startedAt = workout.startedAt,
    exercises = workout.exercises.map { it.toUiState() },
    previousSets = previousSets,
    earlierSets = earlierSets,
    isLoading = false,
)

private fun WorkoutExercise.toUiState(): WorkoutExerciseUiState = WorkoutExerciseUiState(
    exercise = exercise,
    sets = sets.map { it.toUiState() },
    restSeconds = restSeconds,
)

fun LoggedSet.toUiState(): WorkoutSetUiState = WorkoutSetUiState(
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    isCompleted = isCompleted,
)

fun WorkoutUiState.toDomain(sessionId: Int): WorkoutSession = WorkoutSession(
    id = sessionId,
    routineId = routineId,
    routineName = routineName,
    startedAt = startedAt,
    exercises = exercises.map { exercise ->
        WorkoutExercise(
            exercise = exercise.exercise,
            sets = exercise.sets.map { it.toLoggedSet() },
            restSeconds = exercise.restSeconds,
        )
    },
)

private fun WorkoutSetUiState.toLoggedSet(): LoggedSet = LoggedSet(
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    isCompleted = isCompleted,
)

// endregion
