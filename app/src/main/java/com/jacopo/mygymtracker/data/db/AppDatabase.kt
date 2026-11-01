package com.jacopo.mygymtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jacopo.mygymtracker.data.daos.ExerciseDao
import com.jacopo.mygymtracker.data.daos.RoutineDao
import com.jacopo.mygymtracker.data.daos.SetDao
import com.jacopo.mygymtracker.data.model.ExerciseEntry
import com.jacopo.mygymtracker.data.model.RoutineEntry
import com.jacopo.mygymtracker.data.model.RoutineExerciseCrossRef
import com.jacopo.mygymtracker.data.model.SetEntry

@Database(entities = [RoutineEntry::class, ExerciseEntry::class, SetEntry::class, RoutineExerciseCrossRef::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setDao(): SetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // nome del file del db
                )
                    //.fallbackToDestructiveMigration() // opzionale in sviluppo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}