package com.jacopo.mygymtracker.data.mapper

import com.jacopo.mygymtracker.data.backup.BackupBodyWeight
import com.jacopo.mygymtracker.data.backup.BackupExercise
import com.jacopo.mygymtracker.data.backup.BackupExerciseSets
import com.jacopo.mygymtracker.data.backup.BackupRoutine
import com.jacopo.mygymtracker.data.backup.BackupSet
import com.jacopo.mygymtracker.data.backup.BackupSettings
import com.jacopo.mygymtracker.data.backup.BackupWorkout
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate

fun UserSettings.toBackup() = BackupSettings(
    weightUnit = weightUnit,
    restSeconds = restSeconds,
    plates = plates,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    autoStartRest = autoStartRest,
    keepScreenOn = keepScreenOn,
    name = name,
)

fun BackupSettings.toDomain() = UserSettings(
    weightUnit = weightUnit,
    restSeconds = restSeconds,
    plates = plates,
    themeMode = themeMode,
    dynamicColor = dynamicColor,
    autoStartRest = autoStartRest,
    keepScreenOn = keepScreenOn,
    name = name,
)

fun Exercise.toBackup() = BackupExercise(
    name = name,
    category = category,
    trackingType = trackingType,
    note = note,
    isBarbell = isBarbell,
)

fun BackupExercise.toDomain() = Exercise(
    name = name,
    category = category,
    trackingType = trackingType,
    note = note,
    isBarbell = isBarbell,
)

fun Routine.toBackup() = BackupRoutine(
    id = checkNotNull(id) { "Only saved routines are backed up" },
    name = name,
    note = note,
    exercises = exercises.map { routineExercise ->
        BackupExerciseSets(
            exercise = routineExercise.exercise.name,
            restSeconds = routineExercise.restSeconds,
            sets = routineExercise.sets.map { BackupSet(it.reps, it.weightKg, it.durationSeconds, it.type) },
        )
    },
)

/** [exercises] is the imported catalog by name; every reference must already be validated. */
fun BackupRoutine.toDomain(exercises: Map<String, Exercise>) = Routine(
    name = name,
    note = note,
    exercises = this.exercises.map { backup ->
        RoutineExercise(
            exercise = exercises.getValue(backup.exercise),
            sets = backup.sets.map { PlannedSet(it.reps, it.weightKg, it.durationSeconds, it.type) },
            restSeconds = backup.restSeconds,
        )
    },
)

fun WorkoutSession.toBackup() = BackupWorkout(
    routineId = routineId,
    routineName = routineName,
    startedAtEpochMs = startedAt.toEpochMilli(),
    finishedAtEpochMs = checkNotNull(finishedAt) { "Only finished workouts are backed up" }.toEpochMilli(),
    exercises = exercises.map { workoutExercise ->
        BackupExerciseSets(
            exercise = workoutExercise.exercise.name,
            restSeconds = workoutExercise.restSeconds,
            sets = workoutExercise.sets.map { BackupSet(it.reps, it.weightKg, it.durationSeconds, it.type, it.rpe) },
        )
    },
)

/**
 * Like [BackupRoutine.toDomain]. [routineIds] maps the file's routine ids to the new ones;
 * an unknown id leaves the workout without a routine, as if the routine had been deleted.
 */
fun BackupWorkout.toDomain(exercises: Map<String, Exercise>, routineIds: Map<Int, Int>) = WorkoutSession(
    routineId = routineId?.let(routineIds::get),
    routineName = routineName,
    startedAt = Instant.ofEpochMilli(startedAtEpochMs),
    finishedAt = Instant.ofEpochMilli(finishedAtEpochMs),
    exercises = this.exercises.map { backup ->
        WorkoutExercise(
            exercise = exercises.getValue(backup.exercise),
            sets = backup.sets.map {
                LoggedSet(it.reps, it.weightKg, it.durationSeconds, isCompleted = true, type = it.type, rpe = it.rpe)
            },
            restSeconds = backup.restSeconds,
        )
    },
)

fun BodyWeight.toBackup() = BackupBodyWeight(dateEpochDay = date.toEpochDay(), weightKg = weightKg)

fun BackupBodyWeight.toDomain() = BodyWeight(date = LocalDate.ofEpochDay(dateEpochDay), weightKg = weightKg)
