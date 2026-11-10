package com.jacopo.mygymtracker.data.repository

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.WorkoutDao
import com.jacopo.mygymtracker.data.local.entity.WorkoutExerciseEntity
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.data.mapper.toExerciseHistory
import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.domain.model.toWorkoutSession
import com.jacopo.mygymtracker.domain.model.withCompletedSetsOnly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Single source of truth for workouts: the one in progress and the finished history. */
class WorkoutRepository(
    private val database: AppDatabase,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
) {
    /** Emits the workout in progress, or `null` when there is none. */
    fun observeActiveWorkout(): Flow<WorkoutSession?> =
        workoutDao.observeActive().map { it?.toDomain() }

    suspend fun getWorkout(sessionId: Int): WorkoutSession? =
        workoutDao.getWithExercises(sessionId)?.toDomain()

    /**
     * Starts a workout pre-filled with [routine]'s plan. A routine without exercises (never saved,
     * `id == null`) gives an empty workout the user fills as they go.
     *
     * @return the id of the new session, or `null` if another workout is already in progress.
     */
    suspend fun startWorkout(routine: Routine, now: Instant = Instant.now()): Int? =
        database.withTransaction {
            if (workoutDao.hasActive()) return@withTransaction null
            val workout = routine.toWorkoutSession(startedAt = now)
            val sessionId = workoutDao.insertSession(workout.toEntity()).toInt()
            workoutDao.writeExercises(sessionId, workout.exercises, exerciseDao)
            sessionId
        }

    /** Stores the in-progress state. Ignored if the workout was finished or discarded meanwhile. */
    suspend fun saveDraft(workout: WorkoutSession) {
        val sessionId = requireNotNull(workout.id) { "Only started workouts can be saved" }
        database.withTransaction {
            if (!workoutDao.isActive(sessionId)) return@withTransaction
            workoutDao.updateSession(workout.copy(finishedAt = null).toEntity())
            workoutDao.writeExercises(sessionId, workout.exercises, exerciseDao)
        }
    }

    /**
     * Finishes [workout], dropping unchecked sets and exercises left without completed sets.
     *
     * @return the finished workout as stored, with catalog ids resolved,
     * or `null` if it was finished or discarded meanwhile.
     */
    suspend fun finishWorkout(workout: WorkoutSession, now: Instant = Instant.now()): WorkoutSession? {
        val sessionId = requireNotNull(workout.id) { "Only started workouts can be finished" }
        val finished = workout.withCompletedSetsOnly().copy(finishedAt = now)
        require(finished.exercises.isNotEmpty()) { "A workout needs at least one completed set" }
        val wasActive = database.withTransaction {
            if (!workoutDao.isActive(sessionId)) return@withTransaction false
            workoutDao.updateSession(finished.toEntity())
            workoutDao.writeExercises(sessionId, finished.exercises, exerciseDao)
            true
        }
        return if (wasActive) getWorkout(sessionId) else null
    }

    /**
     * Replaces the exercises and sets of a finished workout; its times and routine stay as they were.
     * Every set counts as completed, and exercises left without sets are dropped.
     *
     * @return the workout as stored, or `null` if it was deleted or isn't finished.
     */
    suspend fun updateFinishedWorkout(sessionId: Int, exercises: List<WorkoutExercise>): WorkoutSession? {
        val kept = exercises
            .filter { it.sets.isNotEmpty() }
            .map { exercise -> exercise.copy(sets = exercise.sets.map { it.copy(isCompleted = true) }) }
        require(kept.isNotEmpty()) { "A workout needs at least one set" }
        val wasFinished = database.withTransaction {
            if (!workoutDao.isFinished(sessionId)) return@withTransaction false
            workoutDao.writeExercises(sessionId, kept, exerciseDao)
            true
        }
        return if (wasFinished) getWorkout(sessionId) else null
    }

    /** Deletes a workout, in progress (discard) or finished (history). */
    suspend fun deleteWorkout(sessionId: Int) = workoutDao.deleteSession(sessionId)

    /** Emits finished workouts, newest first. */
    fun observeHistory(): Flow<List<WorkoutSummary>> =
        workoutDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    /** Emits finished workouts with all their sets, oldest first. */
    fun observeFinishedWorkouts(): Flow<List<WorkoutSession>> =
        workoutDao.observeFinishedWithExercises().map { rows -> rows.map { it.toDomain() } }

    /** The sets performed last time for each exercise. Exercises never performed are absent. */
    suspend fun getLastPerformance(exerciseIds: Collection<Int>): Map<Int, List<LoggedSet>> =
        exerciseIds.distinct()
            .associateWith { exerciseId -> workoutDao.getLastSets(exerciseId).map { it.toDomain() } }
            .filterValues { it.isNotEmpty() }

    /** Emits the completed sets of an exercise, grouped per finished workout, oldest first. */
    fun observeExerciseHistory(exerciseId: Int): Flow<List<ExerciseHistoryEntry>> =
        workoutDao.observeExerciseHistory(exerciseId).map { it.toExerciseHistory() }

    /** The completed sets of each exercise from finished workouts performed before [before]: the records to beat. */
    suspend fun getEarlierSets(exerciseIds: Collection<Int>, before: Instant): Map<Int, List<LoggedSet>> =
        exerciseIds.distinct().associateWith { exerciseId ->
            observeExerciseHistory(exerciseId).first()
                .filter { it.performedAt < before }
                .flatMap { it.sets }
        }
}

/**
 * Rebuilds the exercises and logged sets of a session; list order becomes `position`.
 * Call inside a transaction.
 */
internal suspend fun WorkoutDao.writeExercises(
    sessionId: Int,
    exercises: List<WorkoutExercise>,
    exerciseDao: ExerciseDao,
) {
    deleteWorkoutExercises(sessionId)
    exercises.forEachIndexed { position, workoutExercise ->
        val workoutExerciseId = insertWorkoutExercise(
            WorkoutExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseDao.resolveCatalogId(workoutExercise.exercise),
                position = position,
                restSeconds = workoutExercise.restSeconds,
            ),
        ).toInt()
        insertLoggedSets(
            workoutExercise.sets.mapIndexed { setPosition, set ->
                set.toEntity(workoutExerciseId = workoutExerciseId, position = setPosition)
            },
        )
    }
}