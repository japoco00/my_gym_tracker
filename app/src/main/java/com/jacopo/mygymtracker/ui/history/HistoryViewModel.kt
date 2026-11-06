package com.jacopo.mygymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.BodyWeightRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.history.TrainingWeek
import com.jacopo.mygymtracker.domain.history.groupByWeek
import com.jacopo.mygymtracker.domain.history.weekStart
import com.jacopo.mygymtracker.domain.history.weekStreak
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * @property weeks finished workouts grouped by week, newest first.
 * @property streakWeeks weeks in a row with a workout, up to this one.
 * @property weekDays the days of the current week, for the header strip.
 * @property trainedDays the days of [weekDays] with a workout.
 * @property latestBodyWeight the last logged body weight, `null` when none.
 */
data class HistoryUiState(
    val weeks: List<TrainingWeek> = emptyList(),
    val streakWeeks: Int = 0,
    val today: LocalDate = LocalDate.ofEpochDay(0),
    val weekDays: List<LocalDate> = emptyList(),
    val trainedDays: Set<LocalDate> = emptySet(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val latestBodyWeight: BodyWeight? = null,
    val isLoading: Boolean = true,
) {
    val workoutCount: Int = weeks.sumOf { it.workouts.size }
}

class HistoryViewModel(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    bodyWeightRepository: BodyWeightRepository,
    clock: Clock = Clock.systemDefaultZone(),
    firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutRepository.observeHistory(),
        settingsRepository.observeSettings(),
        bodyWeightRepository.observeAll(),
    ) { workouts, settings, bodyWeights ->
        val today = LocalDate.now(clock)
        val weeks = groupByWeek(workouts, clock.zone, firstDayOfWeek)
        val thisWeek = today.weekStart(firstDayOfWeek)
        HistoryUiState(
            weeks = weeks,
            streakWeeks = weekStreak(weeks, today, firstDayOfWeek),
            today = today,
            weekDays = (0L..6L).map(thisWeek::plusDays),
            trainedDays = weeks.firstOrNull { it.start == thisWeek }?.workouts.orEmpty()
                .mapTo(HashSet()) { it.startedAt.atZone(clock.zone).toLocalDate() },
            weightUnit = settings.weightUnit,
            latestBodyWeight = bodyWeights.lastOrNull(),
            isLoading = false,
        )
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
                    bodyWeightRepository = container.bodyWeightRepository,
                )
            }
        }
    }
}
