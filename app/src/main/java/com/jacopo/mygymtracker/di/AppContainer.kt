package com.jacopo.mygymtracker.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.jacopo.mygymtracker.MyApplication
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository

/**
 * Manual dependency container. Owns every app-scoped singleton.
 * Dependencies are created lazily on first access.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase by lazy { AppDatabase.create(context) }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(
            database = database,
            routineDao = database.routineDao(),
            exerciseDao = database.exerciseDao(),
        )
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(exerciseDao = database.exerciseDao())
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(
            database = database,
            workoutDao = database.workoutDao(),
            exerciseDao = database.exerciseDao(),
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") },
        )
    }
}

/**
 * Resolves the [AppContainer] from ViewModel [CreationExtras].
 * Use inside `viewModelFactory { initializer { ... } }`.
 */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as MyApplication).container
