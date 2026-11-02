package com.jacopo.mygymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.progress.volumeKg
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * @property workout the finished workout, or `null` while loading.
 * @property isClosed the workout was deleted or doesn't exist: leave the screen.
 */
data class WorkoutDetailUiState(
    val workout: WorkoutSession? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isDeleteDialogOpen: Boolean = false,
    val isClosed: Boolean = false,
) {
    val durationSeconds: Long = workout?.finishedAt?.let { Duration.between(workout.startedAt, it).seconds } ?: 0
    val setCount: Int = workout?.exercises?.sumOf { it.sets.size } ?: 0
    val volumeKg: Double = workout?.exercises?.sumOf { it.sets.volumeKg() } ?: 0.0
}

class WorkoutDetailViewModel(
    private val sessionId: Int,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    private var deleteJob: Job? = null

    init {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkout(sessionId)?.takeIf { it.finishedAt != null }
            _uiState.update { it.copy(workout = workout, isClosed = workout == null) }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(weightUnit = settings.weightUnit) }
            }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(isDeleteDialogOpen = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(isDeleteDialogOpen = false) }
    }

    fun confirmDelete() {
        if (deleteJob != null) return
        _uiState.update { it.copy(isDeleteDialogOpen = false) }
        deleteJob = viewModelScope.launch {
            withContext(NonCancellable) { workoutRepository.deleteWorkout(sessionId) }
            _uiState.update { it.copy(isClosed = true) }
        }
    }

    companion object {
        fun factory(sessionId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WorkoutDetailViewModel(
                    sessionId = sessionId,
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
