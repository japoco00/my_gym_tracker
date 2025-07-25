package com.jacopo.mygymtracker.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jacopo.mygymtracker.data.repositories.RoutineRepository

class RoutinesListVMFactory(
    private val routineRepository: RoutineRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutinesListVM::class.java)) {
            val viewModel = RoutinesListVM(
                routineRepository,
            )
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}