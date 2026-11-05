package com.jacopo.mygymtracker.domain.model

import java.time.Instant

/**
 * A workout performed from a routine. It is in progress while [finishedAt] is `null`.
 * [routineName] is a snapshot taken at start, so history keeps it after the routine changes.
 */
data class WorkoutSession(
    val id: Int? = null,
    val routineId: Int?,
    val routineName: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
)

/** [restSeconds] is copied from the routine and can change mid-workout; `null` means the default. */
data class WorkoutExercise(
    val exercise: Exercise,
    val sets: List<LoggedSet>,
    val restSeconds: Int? = null,
)

/** A performed set; like [PlannedSet], the fields that matter depend on the exercise's [TrackingType]. */
data class LoggedSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
    val isCompleted: Boolean = false,
)

/** Starts a workout pre-filled with the routine's plan. */
fun Routine.toWorkoutSession(startedAt: Instant): WorkoutSession = WorkoutSession(
    routineId = id,
    routineName = name,
    startedAt = startedAt,
    exercises = exercises.map { routineExercise ->
        WorkoutExercise(
            exercise = routineExercise.exercise,
            sets = routineExercise.sets.map {
                LoggedSet(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds)
            },
            restSeconds = routineExercise.restSeconds,
        )
    },
)

/** Keeps only completed sets, and only the exercises that still have some. */
fun WorkoutSession.withCompletedSetsOnly(): WorkoutSession = copy(
    exercises = exercises.mapNotNull { exercise ->
        exercise.copy(sets = exercise.sets.filter { it.isCompleted }).takeIf { it.sets.isNotEmpty() }
    },
)

/** The routine plan matching what was performed, used to offer "update routine" after a workout. */
fun WorkoutSession.toRoutineExercises(): List<RoutineExercise> = exercises.map { exercise ->
    RoutineExercise(
        exercise = exercise.exercise,
        sets = exercise.sets.map {
            PlannedSet(reps = it.reps, weightKg = it.weightKg, durationSeconds = it.durationSeconds)
        },
        restSeconds = exercise.restSeconds,
    )
}