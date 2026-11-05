package com.jacopo.mygymtracker.data.repository

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.RoutineDao
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseEntity
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single source of truth for routines, including their ordered exercises and planned sets. */
class RoutineRepository(
    private val database: AppDatabase,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
) {
    /** Emits all routines, fully populated, whenever a routine, link, set, or catalog entry changes. */
    fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeAllWithExercises().map { routines -> routines.map { it.toDomain() } }

    /** Returns a fully populated routine, or `null` if it does not exist. */
    suspend fun getRoutine(routineId: Int): Routine? =
        routineDao.getWithExercises(routineId)?.toDomain()

    /**
     * Inserts or updates [routine] together with its ordered exercises and sets, atomically.
     * Links and sets are rebuilt from scratch, so the list order becomes the stored `position`.
     * Catalog exercises are only referenced, never renamed; see [resolveCatalogId].
     *
     * @return the id of the saved routine.
     */
    suspend fun saveRoutine(routine: Routine): Int = database.withTransaction {
        val routineId = routine.id
            ?.also { routineDao.update(routine.toEntity()) }
            ?: routineDao.insert(routine.toEntity()).toInt()
        routineDao.writeExercises(routineId, routine.exercises, exerciseDao)
        routineId
    }

    /** Deletes a routine. Its links and sets cascade; past workouts keep their name snapshot. */
    suspend fun deleteRoutine(routineId: Int) = routineDao.deleteById(routineId)
}

/**
 * Rebuilds the exercise links and planned sets of a routine; list order becomes `position`.
 * Call inside a transaction.
 */
internal suspend fun RoutineDao.writeExercises(
    routineId: Int,
    exercises: List<RoutineExercise>,
    exerciseDao: ExerciseDao,
) {
    deleteRoutineExercises(routineId)
    exercises.forEachIndexed { position, routineExercise ->
        val routineExerciseId = insertRoutineExercise(
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = exerciseDao.resolveCatalogId(routineExercise.exercise),
                position = position,
                restSeconds = routineExercise.restSeconds,
            ),
        ).toInt()
        insertPlannedSets(
            routineExercise.sets.mapIndexed { setPosition, set ->
                set.toEntity(routineExerciseId = routineExerciseId, position = setPosition)
            },
        )
    }
}