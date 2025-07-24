package com.jacopo.mygymtracker.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.toDomainModel
import com.jacopo.mygymtracker.model.Routine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesListVM(
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    var routines = routineRepository.getAllRoutines()
        .map { list -> list.map { it.toDomainModel() } }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadRoutines() {
        viewModelScope.launch {
            routineRepository.getAllRoutines().collect { routineList ->
                routines.value
            }
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            routineRepository.delete(routine)
        }
    }

    fun addEmptyRoutine() {
        val newRoutine = Routine(
            id = 0, // Assuming 0 is a placeholder for a new routine
            name = "New Routine",
            note = null,
            exercises = emptyList() // Assuming no exercises initially
        )
        viewModelScope.launch {
            routineRepository.insert(newRoutine)
        }
    }
}