package com.jacopo.mygymtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.BodyWeightRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.home.Greeting
import com.jacopo.mygymtracker.domain.home.HomeSituation
import com.jacopo.mygymtracker.domain.home.HomeStats
import com.jacopo.mygymtracker.domain.home.greeting
import com.jacopo.mygymtracker.domain.home.homeSituation
import com.jacopo.mygymtracker.domain.home.homeStats
import com.jacopo.mygymtracker.domain.home.timeOfDay
import com.jacopo.mygymtracker.domain.home.upNext
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.domain.progress.latestRecords
import com.jacopo.mygymtracker.ui.workout.WorkoutStarter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.random.Random

private const val RECORDS_SHOWN = 3
private const val BODY_WEIGHT_WEEKS = 12L

/**
 * @property greeting the title, `null` while loading.
 * @property upNext the routine the Start button starts, `null` without a routine to train.
 * @property lastWorkout the most recent finished workout.
 * @property stats `null` before the first finished workout.
 * @property records the latest personal records, newest first.
 * @property latestBodyWeight the last logged body weight, `null` when none.
 * @property bodyWeightTrend the body weights logged in the last 12 weeks, oldest first.
 * @property startConflict routine the user tried to start while [activeWorkout] is still running.
 * @property startedWorkoutId workout just started, waiting for the screen to open it.
 */
data class HomeUiState(
    val greeting: Greeting? = null,
    val situation: HomeSituation = HomeSituation.New,
    val today: LocalDate = LocalDate.ofEpochDay(0),
    val name: String = "",
    val upNext: Routine? = null,
    val lastWorkout: WorkoutSummary? = null,
    val stats: HomeStats? = null,
    val records: List<PersonalRecord> = emptyList(),
    val latestBodyWeight: BodyWeight? = null,
    val bodyWeightTrend: List<BodyWeight> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val startConflict: Routine? = null,
    val startedWorkoutId: Int? = null,
    val isLoading: Boolean = true,
) {
    val activeWorkout: WorkoutSession? get() = (situation as? HomeSituation.Active)?.workout
}

/** [seed] picks the greeting once, so it doesn't change every time the data does. */
class HomeViewModel(
    routineRepository: RoutineRepository,
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    bodyWeightRepository: BodyWeightRepository,
    clock: Clock = Clock.systemDefaultZone(),
    firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
    seed: Int = Random.nextInt(),
) : ViewModel() {

    private val starter = WorkoutStarter(workoutRepository, viewModelScope) { uiState.value.activeWorkout }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            routineRepository.observeRoutines(),
            workoutRepository.observeActiveWorkout(),
            workoutRepository.observeHistory(),
            ::Triple,
        ),
        combine(
            workoutRepository.observeFinishedWorkouts().map { latestRecords(it, RECORDS_SHOWN) },
            bodyWeightRepository.observeAll(),
            settingsRepository.observeSettings(),
            ::Triple,
        ),
        starter.conflict,
        starter.startedWorkoutId,
    ) { (routines, activeWorkout, history), (records, bodyWeights, settings), conflict, startedId ->
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val stats = homeStats(history, today, clock.zone, firstDayOfWeek)
        val upNext = upNext(routines, history.firstNotNullOfOrNull { it.routineId })
        val lastWorkout = history.firstOrNull()
        val situation = homeSituation(activeWorkout, lastWorkout?.startedAt?.atZone(clock.zone)?.toLocalDate(), today)
        HomeUiState(
            greeting = greeting(
                situation = situation,
                timeOfDay = now.toLocalTime().timeOfDay(),
                name = settings.name,
                upNext = upNext?.name,
                weekStreak = stats?.weekStreak ?: 0,
                seed = seed,
            ),
            situation = situation,
            today = today,
            name = settings.name,
            upNext = upNext,
            lastWorkout = lastWorkout,
            stats = stats,
            records = records,
            latestBodyWeight = bodyWeights.lastOrNull(),
            bodyWeightTrend = bodyWeights.takeLastWhile { it.date > today.minusWeeks(BODY_WEIGHT_WEEKS) },
            weightUnit = settings.weightUnit,
            startConflict = conflict,
            startedWorkoutId = startedId,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Starts a workout from [routine], or asks what to do if another one is still running. */
    fun startWorkout(routine: Routine) = starter.start(routine)

    /** Starts a workout with no exercises and no routine behind it, called [name]. */
    fun startEmptyWorkout(name: String) = starter.startEmpty(name)

    /** Throws away the running workout and starts the one that caused the conflict. */
    fun discardActiveAndStart() = starter.discardActiveAndStart()

    fun dismissStartConflict() = starter.dismissConflict()

    /** Called once the screen has navigated to [HomeUiState.startedWorkoutId]. */
    fun onWorkoutOpened() = starter.onWorkoutOpened()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(
                    routineRepository = container.routineRepository,
                    workoutRepository = container.workoutRepository,
                    settingsRepository = container.settingsRepository,
                    bodyWeightRepository = container.bodyWeightRepository,
                )
            }
        }
    }
}
