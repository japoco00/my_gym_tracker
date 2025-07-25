package com.jacopo.mygymtracker.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jacopo.mygymtracker.data.repositories.RoutineRepository

class RoutineViewModelFactory(
    private val routineId: Int? = null,
    private val routineRepository: RoutineRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineViewVM::class.java)) {
            val viewModel = RoutineViewVM(
                routineRepository,
            )
            viewModel.loadRoutine(routineId ?: -1)
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
