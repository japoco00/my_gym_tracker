package com.jacopo.mygymtracker.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.jacopo.mygymtracker.MyApplication
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository

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
}

/**
 * Resolves the [AppContainer] from ViewModel [CreationExtras].
 * Use inside `viewModelFactory { initializer { ... } }`.
 */
fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as MyApplication).container
