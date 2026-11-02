package com.jacopo.mygymtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.local.dao.RoutineDao
import com.jacopo.mygymtracker.data.local.dao.WorkoutDao
import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.LoggedSetEntity
import com.jacopo.mygymtracker.data.local.entity.PlannedSetEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        PlannedSetEntity::class,
        WorkoutSessionEntity::class,
        WorkoutExerciseEntity::class,
        LoggedSetEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        /** Builds the database. A single instance is owned by the AppContainer. */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                // Versions 1-3 were development schemas with no data worth keeping.
                // From version 4 on, every schema change MUST ship a Migration.
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                .addMigrations(MIGRATION_4_5)
                .addCallback(CatalogSeedCallback)
                .build()
    }
}