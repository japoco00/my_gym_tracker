package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.ExerciseDao
import com.jacopo.mygymtracker.data.model.ExerciseEntry

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
) {
    suspend fun insert (exerciseEntry: ExerciseEntry) : Int {
        return exerciseDao.insert(exerciseEntry).toInt()
    }
}