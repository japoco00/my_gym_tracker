package com.jacopo.mygymtracker.data.repository

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.RoutineDao
import com.jacopo.mygymtracker.data.local.dao.SetDao
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseCrossRef
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.domain.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Single source of truth for routines, including their linked exercises and planned sets. */
class RoutineRepository(
    private val database: AppDatabase,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
    private val setDao: SetDao,
) {
    /** Emits all routines, fully populated, whenever a routine, link, or set changes. */
    fun observeRoutines(): Flow<List<Routine>> =
        combine(
            routineDao.observeRoutinesWithExercises(),
            setDao.observeAll(),
        ) { routines, sets ->
            val setsByRoutine = sets.groupBy { it.routineId }
            routines.map { it.toDomain(setsByRoutine[it.routine.routineId].orEmpty()) }
        }

    /** Returns a fully populated routine, or `null` if it does not exist. */
    suspend fun getRoutine(routineId: Int): Routine? = database.withTransaction {
        routineDao.getRoutineWithExercises(routineId)
            ?.toDomain(setDao.getForRoutine(routineId))
    }

    /**
     * Inserts or updates [routine] together with its exercise links and sets, atomically.
     * New (unsaved) exercises are added to the catalog. Links and sets are rebuilt from
     * scratch so that exercises/sets removed in the UI are removed from the database too.
     *
     * @return the id of the saved routine.
     */
    suspend fun saveRoutine(routine: Routine): Int = database.withTransaction {
        val routineId = routine.id
            ?.also { routineDao.update(routine.toEntity()) }
            ?: routineDao.insert(routine.toEntity()).toInt()

        routineDao.deleteCrossRefsForRoutine(routineId)
        setDao.deleteForRoutine(routineId)

        routine.exercises.forEach { exercise ->
            val exerciseId = exercise.id ?: exerciseDao.insert(exercise.toEntity()).toInt()
            routineDao.insertCrossRef(RoutineExerciseCrossRef(routineId = routineId, exerciseId = exerciseId))
            setDao.insertAll(exercise.sets.map { it.toEntity(exerciseId = exerciseId, routineId = routineId) })
        }
        routineId
    }

    /** Deletes a routine. Its links and sets are removed by cascading foreign keys. */
    suspend fun deleteRoutine(routineId: Int) = routineDao.deleteById(routineId)
}
