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
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** @property workouts finished workouts, newest first. */
data class HistoryUiState(
    val workouts: List<WorkoutSummary> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutRepository.observeHistory(),
        settingsRepository.observeSettings(),
    ) { workouts, settings ->
        HistoryUiState(workouts = workouts, weightUnit = settings.weightUnit, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HistoryViewModel(
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
