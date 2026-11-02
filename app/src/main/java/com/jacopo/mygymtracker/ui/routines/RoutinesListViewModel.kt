package com.jacopo.mygymtracker.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Routine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutinesListUiState(
    val routines: List<Routine> = emptyList(),
)

class RoutinesListViewModel(
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    val uiState: StateFlow<RoutinesListUiState> =
        routineRepository.observeRoutines()
            .map { routines -> RoutinesListUiState(routines = routines) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RoutinesListUiState(),
            )

    fun deleteRoutine(routine: Routine) {
        val id = routine.id ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoutinesListViewModel(
                    routineRepository = appContainer().routineRepository,
                )
            }
        }
    }
}
