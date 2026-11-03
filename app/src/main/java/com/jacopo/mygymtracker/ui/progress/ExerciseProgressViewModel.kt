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
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.domain.progress.progressMetrics
import com.jacopo.mygymtracker.domain.progress.recordFor
import com.jacopo.mygymtracker.domain.progress.toSessionProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * @property metric one of [metrics], the ones for this exercise's [trackingType].
 * @property sessions every finished workout with this exercise, oldest first.
 * @property chartSessions the [sessions] with a positive value for [metric], i.e. the plotted ones.
 * @property record the workout holding the record for [metric], `null` without any positive value.
 */
data class ExerciseProgressUiState(
    val exerciseName: String = "",
    val category: Category? = null,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val metric: ProgressMetric = trackingType.progressMetrics().first(),
    val sessions: List<SessionProgress> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
) {
    val metrics: List<ProgressMetric> = trackingType.progressMetrics()
    val chartSessions: List<SessionProgress> = sessions.filter { (it.valueOf(metric) ?: 0.0) > 0 }
    val record: SessionProgress? = sessions.recordFor(metric)
}

class ExerciseProgressViewModel(
    exerciseId: Int,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** `null` until the user picks one, meaning the tracking type's default. */
    private val selectedMetric = MutableStateFlow<ProgressMetric?>(null)

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        exerciseRepository.observeExercise(exerciseId),
        workoutRepository.observeExerciseHistory(exerciseId),
        settingsRepository.observeSettings(),
        selectedMetric,
    ) { exercise, history, settings, selected ->
        val trackingType = exercise?.trackingType ?: TrackingType.WEIGHT_REPS
        val metrics = trackingType.progressMetrics()
        ExerciseProgressUiState(
            exerciseName = exercise?.name.orEmpty(),
            category = exercise?.category,
            trackingType = trackingType,
            metric = selected?.takeIf { it in metrics } ?: metrics.first(),
            sessions = history.map { it.toSessionProgress(trackingType) },
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
