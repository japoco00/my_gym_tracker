package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.SetDao
import com.jacopo.mygymtracker.data.model.SetEntry

class SetRepository(
    private val setDao: SetDao
) {
    suspend fun update(setEntry: SetEntry) {
        setDao.insert(setEntry)
    }

    suspend fun getAllSetsByExerciseIdAndRoutineId(
        exerciseId: Int, routineId: Int
    ): List<SetEntry> {
        return setDao.getAllSetsByExerciseIdAndRoutineId(exerciseId, routineId)
    }
}
