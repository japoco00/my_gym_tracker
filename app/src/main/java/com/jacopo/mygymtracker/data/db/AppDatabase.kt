package com.jacopo.mygymtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jacopo.mygymtracker.data.model.Exercise
import com.jacopo.mygymtracker.data.model.ExerciseDao
import com.jacopo.mygymtracker.data.model.ExerciseSet
import com.jacopo.mygymtracker.data.model.Routine
import com.jacopo.mygymtracker.data.model.RoutineDao
import com.jacopo.mygymtracker.data.model.SetDao

@Database(entities = [Routine::class, Exercise::class, ExerciseSet::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setDao(): SetDao
}