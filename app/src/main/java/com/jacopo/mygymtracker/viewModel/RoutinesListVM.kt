package com.jacopo.mygymtracker.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.data.repositories.toDomainModel
import com.jacopo.mygymtracker.model.Routine
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesListVM(
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    init {
        Log.d("RoutinesListVM", "RoutinesListVM created")
    }
    var routines = routineRepository.getAllRoutines()
        .map { list -> list.map { it.toDomainModel() } }
        .onEach { newList ->
            Log.d("RoutineVM", "Nuovo stato routines: ${newList.map { it.name }}")
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            Log.d("RoutinesListVM", "Deleting routine: ${routine.name}")
            routineRepository.delete(routine)
        }
    }
}