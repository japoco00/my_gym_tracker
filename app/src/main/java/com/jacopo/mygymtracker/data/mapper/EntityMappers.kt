package com.jacopo.mygymtracker.data.mapper

import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.ExerciseHistoryRow
import com.jacopo.mygymtracker.data.local.entity.LoggedSetEntity
import com.jacopo.mygymtracker.data.local.entity.PlannedSetEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseWithDetails
import com.jacopo.mygymtracker.data.local.entity.RoutineWithExercises
import com.jacopo.mygymtracker.data.local.entity.WorkoutExerciseWithDetails
import com.jacopo.mygymtracker.data.local.entity.WorkoutSessionEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutSessionWithExercises
import com.jacopo.mygymtracker.data.local.entity.WorkoutSummaryRow
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import java.time.Instant

// region Exercise

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    exerciseId = id ?: 0,
    name = name,
    category = category,
    trackingType = trackingType,
    note = note,
)

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = exerciseId,
    name = name,
    category = category,
    trackingType = trackingType,
    note = note,
)

// endregion

// region Routine

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    routineId = id ?: 0,
    name = name,
    note = note,
)

fun RoutineWithExercises.toDomain(): Routine = Routine(
    id = routine.routineId,
    name = routine.name,
    note = routine.note,
    exercises = exercises
        .sortedBy { it.routineExercise.position }
        .map { it.toDomain() },
)

fun RoutineExerciseWithDetails.toDomain(): RoutineExercise = RoutineExercise(
    exercise = exercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() },
)

fun PlannedSet.toEntity(routineExerciseId: Int, position: Int): PlannedSetEntity = PlannedSetEntity(
    routineExerciseId = routineExerciseId,
    position = position,
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
)

fun PlannedSetEntity.toDomain(): PlannedSet = PlannedSet(
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
)

// endregion

// region Workout

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    sessionId = id ?: 0,
    routineId = routineId,
    routineName = routineName,
    startedAtEpochMs = startedAt.toEpochMilli(),
    finishedAtEpochMs = finishedAt?.toEpochMilli(),
)

fun WorkoutSessionWithExercises.toDomain(): WorkoutSession = WorkoutSession(
    id = session.sessionId,
    routineId = session.routineId,
    routineName = session.routineName,
    startedAt = Instant.ofEpochMilli(session.startedAtEpochMs),
    finishedAt = session.finishedAtEpochMs?.let(Instant::ofEpochMilli),
    exercises = exercises
        .sortedBy { it.workoutExercise.position }
        .map { it.toDomain() },
)

fun WorkoutExerciseWithDetails.toDomain(): WorkoutExercise = WorkoutExercise(
    exercise = exercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() },
)

fun LoggedSet.toEntity(workoutExerciseId: Int, position: Int): LoggedSetEntity = LoggedSetEntity(
    workoutExerciseId = workoutExerciseId,
    position = position,
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    isCompleted = isCompleted,
)

fun LoggedSetEntity.toDomain(): LoggedSet = LoggedSet(
    reps = reps,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    isCompleted = isCompleted,
)

fun WorkoutSummaryRow.toDomain(): WorkoutSummary = WorkoutSummary(
    id = sessionId,
    routineName = routineName,
    startedAt = Instant.ofEpochMilli(startedAtEpochMs),
    finishedAt = Instant.ofEpochMilli(finishedAtEpochMs),
    setCount = setCount,
    volumeKg = volumeKg,
)

/** Groups rows, already ordered by session, into one entry per session. */
fun List<ExerciseHistoryRow>.toExerciseHistory(): List<ExerciseHistoryEntry> =
    groupBy { it.sessionId }.map { (sessionId, rows) ->
        ExerciseHistoryEntry(
            sessionId = sessionId,
            performedAt = Instant.ofEpochMilli(rows.first().startedAtEpochMs),
            sets = rows.map {
                LoggedSet(
                    reps = it.reps,
                    weightKg = it.weightKg,
                    durationSeconds = it.durationSeconds,
                    isCompleted = true,
                )
            },
        )
    }

// endregion