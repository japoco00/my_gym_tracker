package com.jacopo.mygymtracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.domain.progress.personalRecords
import com.jacopo.mygymtracker.domain.progress.toSessionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * @property sessions every finished workout with this exercise, oldest first.
 * @property chartSessions the [sessions] with a positive value for [metric], i.e. the plotted ones.
 */
data class ExerciseProgressUiState(
    val exerciseName: String = "",
    val sessions: List<SessionProgress> = emptyList(),
    val records: Map<ProgressMetric, PersonalRecord> = emptyMap(),
    val metric: ProgressMetric = ProgressMetric.ESTIMATED_ONE_REP_MAX,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
) {
    val chartSessions: List<SessionProgress> = sessions.filter { (it.valueOf(metric) ?: 0.0) > 0 }
}

class ExerciseProgressViewModel(
    exerciseId: Int,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val metric = MutableStateFlow(ProgressMetric.ESTIMATED_ONE_REP_MAX)

    private val sessions = workoutRepository.observeExerciseHistory(exerciseId)
        .map { history -> history.map { it.toSessionProgress() } }

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        exerciseRepository.observeExercise(exerciseId),
        sessions,
        settingsRepository.observeSettings(),
        metric,
    ) { exercise, sessions, settings, metric ->
        ExerciseProgressUiState(
            exerciseName = exercise?.name.orEmpty(),
            sessions = sessions,
            records = sessions.personalRecords(),
            metric = metric,
            weightUnit = settings.weightUnit,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseProgressUiState(),
    )

    fun selectMetric(metric: ProgressMetric) {
        this.metric.value = metric
    }

    companion object {
        fun factory(exerciseId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ExerciseProgressViewModel(
                    exerciseId = exerciseId,
                    exerciseRepository = container.exerciseRepository,
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
