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
     * Starts a workout pre-filled with [routine]'s plan, which needs at least one exercise.
     *
     * @return the id of the new session, or `null` if another workout is already in progress.
     */
    suspend fun startWorkout(routine: Routine, now: Instant = Instant.now()): Int? =
        database.withTransaction {
            require(routine.exercises.isNotEmpty()) { "Cannot start a workout without exercises" }
            if (workoutDao.hasActive()) return@withTransaction null
            val workout = routine.toWorkoutSession(startedAt = now)
            val sessionId = workoutDao.insertSession(workout.toEntity()).toInt()
            writeExercises(sessionId, workout.exercises)
            sessionId
        }

    /** Stores the in-progress state. Ignored if the workout was finished or discarded meanwhile. */
    suspend fun saveDraft(workout: WorkoutSession) {
        val sessionId = requireNotNull(workout.id) { "Only started workouts can be saved" }
        database.withTransaction {
            if (!workoutDao.isActive(sessionId)) return@withTransaction
            workoutDao.updateSession(workout.copy(finishedAt = null).toEntity())
            writeExercises(sessionId, workout.exercises)
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
            writeExercises(sessionId, finished.exercises)
            true
        }
        return if (wasActive) getWorkout(sessionId) else null
    }

    /** Deletes a workout, in progress (discard) or finished (history). */
    suspend fun deleteWorkout(sessionId: Int) = workoutDao.deleteSession(sessionId)

    /** Emits finished workouts, newest first. */
    fun observeHistory(): Flow<List<WorkoutSummary>> =
        workoutDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    /** The sets performed last time for each exercise. Exercises never performed are absent. */
    suspend fun getLastPerformance(exerciseIds: Collection<Int>): Map<Int, List<LoggedSet>> =
        exerciseIds.distinct()
            .associateWith { exerciseId -> workoutDao.getLastSets(exerciseId).map { it.toDomain() } }
            .filterValues { it.isNotEmpty() }

    /** Emits the completed sets of an exercise, grouped per finished workout, oldest first. */
    fun observeExerciseHistory(exerciseId: Int): Flow<List<ExerciseHistoryEntry>> =
        workoutDao.observeExerciseHistory(exerciseId).map { it.toExerciseHistory() }

    /** Rebuilds the exercises and sets of a session; list order becomes `position`. */
    private suspend fun writeExercises(sessionId: Int, exercises: List<WorkoutExercise>) {
        workoutDao.deleteWorkoutExercises(sessionId)
        exercises.forEachIndexed { position, workoutExercise ->
            val workoutExerciseId = workoutDao.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseDao.resolveCatalogId(workoutExercise.exercise),
                    position = position,
                ),
            ).toInt()
            workoutDao.insertLoggedSets(
                workoutExercise.sets.mapIndexed { setPosition, set ->
                    set.toEntity(workoutExerciseId = workoutExerciseId, position = setPosition)
                },
            )
        }
    }
}