package com.jacopo.mygymtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.RoutineDao
import com.jacopo.mygymtracker.data.local.dao.SetDao
import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseCrossRef
import com.jacopo.mygymtracker.data.local.entity.SetEntity

@Database(
    entities = [
        RoutineEntity::class,
        ExerciseEntity::class,
        SetEntity::class,
        RoutineExerciseCrossRef::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setDao(): SetDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        /** Builds the database. A single instance is owned by the AppContainer. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                // Development only: wipes all data on schema changes.
                // Replace with real migrations before the first release.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
