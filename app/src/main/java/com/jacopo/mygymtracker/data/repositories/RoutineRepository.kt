package com.jacopo.mygymtracker.data.repositories

import androidx.room.withTransaction
import com.jacopo.mygymtracker.data.daos.RoutineDao
import com.jacopo.mygymtracker.data.db.AppDatabase
import com.jacopo.mygymtracker.data.model.RoutineFullEntry
import com.jacopo.mygymtracker.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.jacopo.mygymtracker.data.model.RoutineEntry as DataRoutine

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseRepository: ExerciseRepository,
    private val database: AppDatabase
) {
    suspend fun insert(routine: Routine): Long {
        return routineDao.insert(routine.toDataModel())
    }

    suspend fun update(routine: Routine) {
        routineDao.update(routine.toDataModel())
    }

    suspend fun updateFull(routine: Routine) {
        database.withTransaction {
            val routineData = routine.toDataModel()
            val id = routineDao.insert(routineData).toInt()
            for (exercise in routine.exercises) {
                exerciseRepository.updateFull(exercise, id)
            }
        }
    }

    suspend fun delete(routine: Routine) {
        routineDao.delete(routine.toDataModel())
    }

    fun getAllRoutines(): Flow<List<RoutineFullEntry>> {
        return routineDao.getAllRoutines()
    }

    fun getRoutineById(id: Int): Flow<Routine?> {
        return routineDao.getRoutineById(id).map { it?.toDomainModel() }
    }
}

fun RoutineFullEntry.toDomainModel(): Routine {
    return Routine(
        id = routineEntry.id,
        name = routineEntry.name,
        note = routineEntry.note,
        exercises = exercises.map { it.toDomainModel() }
    )
}

fun Routine.toDataModel(): DataRoutine {
    return DataRoutine(
        id = id ?: 0, // Assuming id is nullable, use 0 for new routines
        name = name,
        note = note
    )
}

