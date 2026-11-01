package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.RoutineDao
import com.jacopo.mygymtracker.data.db.AppDatabase
import com.jacopo.mygymtracker.data.model.RoutineEntry
import com.jacopo.mygymtracker.data.model.RoutineWithExercisesEntry
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseRepository: ExerciseRepository,
    private val database: AppDatabase
) {
    suspend fun insert(routineEntry: RoutineEntry): Long {
        return routineDao.insert(routineEntry)
    }

    suspend fun update(routineEntry: RoutineEntry) {
        routineDao.update(routineEntry)
    }

    suspend fun delete(routineEntry: RoutineEntry) {
        routineDao.delete(routineEntry)
    }

    fun getAllRoutinesWithExercises(): Flow<List<RoutineWithExercisesEntry>> {
        return routineDao.getAllRoutinesWithExercises()
    }

    fun getRoutineById(routineId: Int) : Flow<RoutineEntry> {
        return routineDao.getRoutineById(routineId)
    }

}

