package com.jacopo.mygymtracker.ui.workout

import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Starts workouts for a screen's ViewModel. Only one workout can run at a time, so starting while
 * [activeWorkout] returns one raises a [conflict] for the user to settle instead.
 *
 * @param scope usually the owner's `viewModelScope`.
 */
class WorkoutStarter(
    private val workoutRepository: WorkoutRepository,
    private val scope: CoroutineScope,
    private val activeWorkout: () -> WorkoutSession?,
) {
    private val _conflict = MutableStateFlow<Routine?>(null)
    private val _startedWorkoutId = MutableStateFlow<Int?>(null)
    private var job: Job? = null

    /** Routine the user tried to start while another workout is still running. */
    val conflict: StateFlow<Routine?> = _conflict.asStateFlow()

    /** Workout just started, waiting for the screen to open it. */
    val startedWorkoutId: StateFlow<Int?> = _startedWorkoutId.asStateFlow()

    /** Starts a workout from [routine], or raises a [conflict] if another one is still running. */
    fun start(routine: Routine) {
        if (job?.isActive == true) return
        if (activeWorkout() != null) {
            _conflict.value = routine
            return
        }
        job = scope.launch { startNow(routine) }
    }

    /** Starts a workout with no exercises and no routine behind it, called [name]. */
    fun startEmpty(name: String) {
        start(Routine(name = name))
    }

    /** Throws away the running workout and starts the one that caused the [conflict]. */
    fun discardActiveAndStart() {
        val routine = _conflict.value ?: return
        if (job?.isActive == true) return
        _conflict.value = null
        job = scope.launch {
            activeWorkout()?.id?.let { workoutRepository.deleteWorkout(it) }
            startNow(routine)
        }
    }

    fun dismissConflict() {
        _conflict.value = null
    }

    /** Called once the screen has navigated to [startedWorkoutId]. */
    fun onWorkoutOpened() {
        _startedWorkoutId.value = null
    }

    /** Raises a [conflict] instead of starting if another workout became active meanwhile. */
    private suspend fun startNow(routine: Routine) {
        val sessionId = workoutRepository.startWorkout(routine)
        if (sessionId == null) {
            _conflict.value = routine
        } else {
            _startedWorkoutId.value = sessionId
        }
    }
}
