package com.jacopo.mygymtracker

import com.jacopo.mygymtracker.data.db.AppDatabase
import com.jacopo.mygymtracker.data.repositories.ExerciseRepository
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.SetRepository

class AppContainer(database: AppDatabase) {
    val setRepository = SetRepository(database.setDao())
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), setRepository)
    val routineRepository = RoutineRepository(database.routineDao(), exerciseRepository, database)
}
