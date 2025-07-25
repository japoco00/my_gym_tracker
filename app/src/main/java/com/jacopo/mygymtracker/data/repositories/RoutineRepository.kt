package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.RoutineDao
import com.jacopo.mygymtracker.data.model.RoutineWithExercisesWithSets
import com.jacopo.mygymtracker.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.jacopo.mygymtracker.data.model.Routine as DataRoutine


class RoutineRepository(
    private val routineDao: RoutineDao,
) {
    suspend fun insert(routine: Routine): Long {
        return routineDao.insert(routine.toDataModel())
    }

    suspend fun delete(routine: Routine) {
        routineDao.delete(routine.toDataModel())
    }

    fun getAllRoutines(): Flow<List<RoutineWithExercisesWithSets>> {
        return routineDao.getAllRoutines()
    }

    fun getRoutineById(id: Int): Flow<Routine?> {
        return routineDao.getRoutineById(id).map { it?.toDomainModel() }
    }
}


fun RoutineWithExercisesWithSets.toDomainModel(): Routine {
    return Routine(
        id = routine.id,
        name = routine.name,
        note = routine.note,
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

