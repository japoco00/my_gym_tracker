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
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.ui.workout.WorkoutStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property lastWorkout most recent finished workout, or `null` before the first one.
 * @property pendingDelete routine hidden from the list and waiting for the undo snackbar to close.
 * @property startConflict routine the user tried to start while [activeWorkout] is still running.
 * @property startedWorkoutId workout just started, waiting for the screen to open it.
 */
data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val lastWorkout: WorkoutSummary? = null,
    val pendingDelete: Routine? = null,
    val activeWorkout: WorkoutSession? = null,
    val startConflict: Routine? = null,
    val startedWorkoutId: Int? = null,
    val isLoading: Boolean = true,
)

class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val pendingDelete = MutableStateFlow<Routine?>(null)
    private val starter = WorkoutStarter(workoutRepository, viewModelScope) { uiState.value.activeWorkout }

    val uiState: StateFlow<RoutinesUiState> = combine(
        combine(
            routineRepository.observeRoutines(),
            workoutRepository.observeActiveWorkout(),
            workoutRepository.observeHistory(),
            ::Triple,
        ),
        pendingDelete,
        starter.conflict,
        starter.startedWorkoutId,
    ) { (routines, activeWorkout, history), pending, conflict, startedId ->
        RoutinesUiState(
            routines = routines.filterNot { it.id == pending?.id },
            lastWorkout = history.firstOrNull(),
            pendingDelete = pending,
            activeWorkout = activeWorkout,
            startConflict = conflict,
            startedWorkoutId = startedId,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutinesUiState(),
    )

    /** Starts a workout from [routine], or asks what to do if another one is still running. */
    fun startWorkout(routine: Routine) = starter.start(routine)

    /** Starts a workout with no exercises and no routine behind it, called [name]. */
    fun startEmptyWorkout(name: String) = starter.startEmpty(name)

    /** Throws away the running workout and starts the one that caused the conflict. */
    fun discardActiveAndStart() = starter.discardActiveAndStart()

    fun dismissStartConflict() = starter.dismissConflict()

    /** Called once the screen has navigated to [RoutinesUiState.startedWorkoutId]. */
    fun onWorkoutOpened() = starter.onWorkoutOpened()

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
                RoutinesViewModel(
                    routineRepository = container.routineRepository,
                    workoutRepository = container.workoutRepository,
                    applicationScope = container.applicationScope,
                )
            }
        }
    }
}
