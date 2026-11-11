package com.jacopo.mygymtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.widget.WidgetDestination
import com.jacopo.mygymtracker.widget.WidgetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state for [MainActivity]: the settings that decide the theme, and the screen a widget asked for. */
class MainViewModel(
    settingsRepository: SettingsRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    /** `null` until DataStore has been read. Eager, because the activity polls it before anything collects it. */
    val settings: StateFlow<UserSettings?> = settingsRepository.observeSettings()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    private val _widgetDestination = MutableStateFlow<WidgetDestination?>(null)

    /** The screen a widget asked for, until [onWidgetDestinationOpened]. */
    val widgetDestination: StateFlow<WidgetDestination?> = _widgetDestination.asStateFlow()

    /**
     * Resolves [request] to a screen. Starting while another workout runs (the widget was out of
     * date) opens the running one instead of asking: the widget's promise was "train now".
     */
    fun onWidgetRequest(request: WidgetRequest) {
        viewModelScope.launch {
            _widgetDestination.value = when (request) {
                is WidgetRequest.Start -> {
                    val routine = routineRepository.getRoutine(request.routineId) ?: return@launch
                    val sessionId = workoutRepository.startWorkout(routine)
                        ?: workoutRepository.observeActiveWorkout().first()?.id
                        ?: return@launch
                    WidgetDestination.Workout(sessionId)
                }
                is WidgetRequest.Resume -> WidgetDestination.Workout(request.sessionId)
                WidgetRequest.CreateRoutine -> WidgetDestination.NewRoutine
            }
        }
    }

    fun onWidgetDestinationOpened() {
        _widgetDestination.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                MainViewModel(container.settingsRepository, container.routineRepository, container.workoutRepository)
            }
        }
    }
}
