package com.jacopo.mygymtracker.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jacopo.mygymtracker.data.repositories.ExerciseRepository
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.SetRepository

class RoutineViewModelFactory(
    private val routineId: Int? = null,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val setRepository: SetRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineViewVM::class.java)) {
            val viewModel = RoutineViewVM(
                routineRepository,
                exerciseRepository,
                setRepository
            )
            viewModel.loadRoutine(routineId ?: -1)
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
