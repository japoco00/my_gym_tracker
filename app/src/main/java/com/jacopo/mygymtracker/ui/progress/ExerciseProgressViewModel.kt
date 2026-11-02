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
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.domain.progress.personalRecords
import com.jacopo.mygymtracker.domain.progress.progressMetrics
import com.jacopo.mygymtracker.domain.progress.toSessionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * @property metrics the metrics for this exercise's tracking type; [metric] is one of them.
 * @property sessions every finished workout with this exercise, oldest first.
 * @property chartSessions the [sessions] with a positive value for [metric], i.e. the plotted ones.
 */
data class ExerciseProgressUiState(
    val exerciseName: String = "",
    val metrics: List<ProgressMetric> = TrackingType.WEIGHT_REPS.progressMetrics(),
    val metric: ProgressMetric = metrics.first(),
    val sessions: List<SessionProgress> = emptyList(),
    val records: Map<ProgressMetric, PersonalRecord> = emptyMap(),
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

    /** `null` until the user picks one, meaning the tracking type's default. */
    private val selectedMetric = MutableStateFlow<ProgressMetric?>(null)

    private val sessions = workoutRepository.observeExerciseHistory(exerciseId)
        .map { history -> history.map { it.toSessionProgress() } }

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        exerciseRepository.observeExercise(exerciseId),
        sessions,
        settingsRepository.observeSettings(),
        selectedMetric,
    ) { exercise, sessions, settings, selected ->
        val metrics = (exercise?.trackingType ?: TrackingType.WEIGHT_REPS).progressMetrics()
        ExerciseProgressUiState(
            exerciseName = exercise?.name.orEmpty(),
            metrics = metrics,
            metric = selected?.takeIf { it in metrics } ?: metrics.first(),
            sessions = sessions,
            records = sessions.personalRecords(),
            weightUnit = settings.weightUnit,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseProgressUiState(),
    )

    fun selectMetric(metric: ProgressMetric) {
        selectedMetric.value = metric
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
