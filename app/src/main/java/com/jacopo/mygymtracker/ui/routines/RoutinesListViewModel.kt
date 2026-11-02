package com.jacopo.mygymtracker.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property pendingDelete routine hidden from the list and waiting for the undo snackbar to close.
 * @property startConflict routine the user tried to start while [activeWorkout] is still running.
 */
data class RoutinesListUiState(
    val routines: List<Routine> = emptyList(),
    val pendingDelete: Routine? = null,
    val activeWorkout: WorkoutSession? = null,
    val startConflict: Routine? = null,
    val isLoading: Boolean = true,
)

class RoutinesListViewModel(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val pendingDelete = MutableStateFlow<Routine?>(null)
    private val startConflict = MutableStateFlow<Routine?>(null)
    private var startJob: Job? = null

    val uiState: StateFlow<RoutinesListUiState> = combine(
        routineRepository.observeRoutines(),
        workoutRepository.observeActiveWorkout(),
        pendingDelete,
        startConflict,
    ) { routines, activeWorkout, pending, conflict ->
        RoutinesListUiState(
            routines = routines.filterNot { it.id == pending?.id },
            pendingDelete = pending,
            activeWorkout = activeWorkout,
            startConflict = conflict,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutinesListUiState(),
    )

    /** Starts a workout from [routine], or asks what to do if another one is still running. */
    fun startWorkout(routine: Routine, onStarted: (sessionId: Int) -> Unit) {
        if (startJob?.isActive == true) return
        if (uiState.value.activeWorkout != null) {
            startConflict.value = routine
            return
        }
        startJob = viewModelScope.launch { onStarted(workoutRepository.startWorkout(routine)) }
    }

    /** Throws away the running workout and starts the one that caused the conflict. */
    fun discardActiveAndStart(onStarted: (sessionId: Int) -> Unit) {
        val routine = startConflict.value ?: return
        if (startJob?.isActive == true) return
        startConflict.value = null
        startJob = viewModelScope.launch {
            uiState.value.activeWorkout?.id?.let { workoutRepository.deleteWorkout(it) }
            onStarted(workoutRepository.startWorkout(routine))
        }
    }

    fun dismissStartConflict() {
        startConflict.value = null
    }

    /** Hides [routine] at once; it is deleted for real when the undo window closes. */
    fun deleteRoutine(routine: Routine) {
        pendingDelete.getAndUpdate { routine }?.let(::commitDelete)
    }

    fun undoDelete(routineId: Int) {
        pendingDelete.update { pending -> pending?.takeUnless { it.id == routineId } }
    }

    fun confirmDelete(routineId: Int) {
        val pending = pendingDelete.value?.takeIf { it.id == routineId } ?: return
        pendingDelete.value = null
        commitDelete(pending)
    }

    override fun onCleared() {
        pendingDelete.value?.let(::commitDelete)
    }

    /** Runs in the application scope so the delete survives this ViewModel being cleared. */
    private fun commitDelete(routine: Routine) {
        val id = routine.id ?: return
        applicationScope.launch { routineRepository.deleteRoutine(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                RoutinesListViewModel(
                    routineRepository = container.routineRepository,
                    workoutRepository = container.workoutRepository,
                    applicationScope = container.applicationScope,
                )
            }
        }
    }
}
