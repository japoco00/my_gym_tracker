package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.RoutineDao
import com.jacopo.mygymtracker.data.model.RoutineWithExercisesWithSets
import com.jacopo.mygymtracker.model.Routine

class RoutineRepository (
    private val routineDao: RoutineDao,
){
    suspend fun getAllRoutines(): List<RoutineWithExercisesWithSets> {
        return routineDao.getAllRoutines()
    }
    suspend fun getRoutineById(id: Int): Routine {
        return routineDao.getRoutineById(id)!!.toDomainModel()
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