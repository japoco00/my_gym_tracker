package com.jacopo.mygymtracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.toRoutineExercises
import com.jacopo.mygymtracker.resttimer.RestTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Edits are written to the database this long after the last change, so a crash loses little. */
private const val DRAFT_SAVE_DELAY_MS = 500L

class WorkoutViewModel(
    private val sessionId: Int,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val restTimer: RestTimerController,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var draftJob: Job? = null
    private var hasUnsavedEdits = false

    /** Finish or discard in progress; both end the workout, so only the first one runs. */
    private var closeJob: Job? = null

    /** The routine rewritten with the finished workout's plan, waiting for the user's answer. */
    private var routineUpdate: Routine? = null

    private var restSeconds = UserSettings.DEFAULT_REST_SECONDS

    init {
        loadWorkout()
        observeExerciseCatalog()
        observeSettings()
        observeRestTimer()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkout(sessionId)?.takeIf { it.finishedAt == null }
            if (workout == null) {
                _uiState.update { it.copy(isLoading = false, exit = WorkoutExit.CLOSED) }
                return@launch
            }
            val previousSets = workoutRepository.getLastPerformance(
                workout.exercises.mapNotNull { it.exercise.id },
            )
            _uiState.update { it.withWorkout(workout, previousSets) }
        }
    }

    private fun observeExerciseCatalog() {
        viewModelScope.launch {
            exerciseRepository.observeCatalog().collect { catalog ->
                _uiState.update { it.copy(catalogExercises = catalog) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                restSeconds = settings.restSeconds
                _uiState.update { it.copy(weightUnit = settings.weightUnit) }
            }
        }
    }

    private fun observeRestTimer() {
        viewModelScope.launch {
            restTimer.state.collect { timer -> _uiState.update { it.copy(restTimer = timer) } }
        }
    }

    fun openAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = true) }
    }

    fun closeAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = false) }
    }

    /** Adds the catalog exercises not in the workout yet, pre-filled with last time's sets. */
    fun addExercises(selected: List<Exercise>) {
        closeAddExercisesSheet()
        val ids = selected.mapNotNull { it.id }.toSet() - presentExerciseIds(_uiState.value)
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val previousSets = workoutRepository.getLastPerformance(ids)
            editWorkout { state ->
                val newExercises = selected
                    .filter { it.id in ids && it.id !in presentExerciseIds(state) }
                    .map { exercise ->
                        val lastTime = previousSets[exercise.id].orEmpty()
                        WorkoutExerciseUiState(
                            exercise = exercise,
                            sets = lastTime
                                .map { it.copy(isCompleted = false).toUiState() }
                                .ifEmpty { listOf(WorkoutSetUiState()) },
                        )
                    }
                state.copy(
                    exercises = state.exercises + newExercises,
                    previousSets = state.previousSets + previousSets,
                )
            }
        }
    }

    fun requestRemoveExercise(exerciseKey: String) {
        val exercise = _uiState.value.exercises.find { it.key == exerciseKey } ?: return
        _uiState.update {
            it.copy(dialog = WorkoutDialog.ConfirmRemoveExercise(exerciseKey, exercise.exercise.name))
        }
    }

    fun confirmRemoveExercise(exerciseKey: String) {
        editWorkout { state ->
            state.copy(
                exercises = state.exercises.filterNot { it.key == exerciseKey },
                dialog = null,
            )
        }
    }

    /** Adds a set that copies the last one, so the next set is usually one tap away. */
    fun addSet(exerciseKey: String) {
        updateExercise(exerciseKey) { exercise ->
            val lastSet = exercise.sets.lastOrNull()
            val nextSet = WorkoutSetUiState(
                reps = lastSet?.reps ?: 0,
                weightKg = lastSet?.weightKg ?: 0.0,
                durationSeconds = lastSet?.durationSeconds ?: 0,
            )
            exercise.copy(sets = exercise.sets + nextSet)
        }
    }

    fun removeSet(exerciseKey: String, setKey: String) {
        updateExercise(exerciseKey) { exercise ->
            exercise.copy(sets = exercise.sets.filterNot { it.key == setKey })
        }
    }

    fun updateSetReps(exerciseKey: String, setKey: String, reps: Int) {
        updateSet(exerciseKey, setKey) { it.copy(reps = reps.coerceAtLeast(0)) }
    }

    fun updateSetWeight(exerciseKey: String, setKey: String, weightKg: Double) {
        updateSet(exerciseKey, setKey) { it.copy(weightKg = weightKg.coerceAtLeast(0.0)) }
    }

    fun updateSetDuration(exerciseKey: String, setKey: String, seconds: Int) {
        updateSet(exerciseKey, setKey) { it.copy(durationSeconds = seconds.coerceAtLeast(0)) }
    }

    /** Ticking a set off starts the rest before the next one. */
    fun setCompleted(exerciseKey: String, setKey: String, completed: Boolean) {
        updateSet(exerciseKey, setKey) { it.copy(isCompleted = completed) }
        if (completed) restTimer.start(restSeconds)
    }

    fun adjustRest(deltaSeconds: Int) {
        restTimer.adjust(deltaSeconds)
    }

    fun skipRest() {
        restTimer.skip()
    }

    fun requestFinish() {
        val sets = _uiState.value.exercises.flatMap { it.sets }
        val dialog = if (sets.none { it.isCompleted }) {
            WorkoutDialog.NothingCompleted
        } else {
            WorkoutDialog.ConfirmFinish(uncheckedSets = sets.count { !it.isCompleted })
        }
        _uiState.update { it.copy(dialog = dialog) }
    }

    fun requestDiscard() {
        _uiState.update { it.copy(dialog = WorkoutDialog.ConfirmDiscard) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    /** Stores the workout as finished, then offers to update the routine if the plan changed. */
    fun confirmFinish() {
        if (closeJob != null) return
        _uiState.update { it.copy(dialog = null) }
        restTimer.skip()
        closeJob = viewModelScope.launch {
            draftJob?.cancelAndJoin()
            val workout = _uiState.value.toDomain(sessionId)
            val finished = withContext(NonCancellable) { workoutRepository.finishWorkout(workout) }
            hasUnsavedEdits = false
            if (finished == null) {
                _uiState.update { it.copy(exit = WorkoutExit.CLOSED) }
                return@launch
            }

            // Compared with every set, checked or not: unchecked sets still describe the plan.
            val performedPlan = workout.toRoutineExercises()
            routineUpdate = workout.routineId
                ?.let { routineRepository.getRoutine(it) }
                ?.takeIf { it.exercises != performedPlan }
                ?.copy(exercises = performedPlan)
            _uiState.update {
                if (routineUpdate != null) {
                    it.copy(dialog = WorkoutDialog.UpdateRoutine)
                } else {
                    it.copy(exit = WorkoutExit.FINISHED)
                }
            }
        }
    }

    fun confirmDiscard() {
        if (closeJob != null) return
        _uiState.update { it.copy(dialog = null) }
        restTimer.skip()
        closeJob = viewModelScope.launch {
            draftJob?.cancelAndJoin()
            hasUnsavedEdits = false
            withContext(NonCancellable) { workoutRepository.deleteWorkout(sessionId) }
            _uiState.update { it.copy(exit = WorkoutExit.CLOSED) }
        }
    }

    /** Answers the "update routine?" question. The save outlives this screen. */
    fun resolveRoutineUpdate(update: Boolean) {
        val routine = routineUpdate?.takeIf { update }
        routineUpdate = null
        if (routine != null) applicationScope.launch { routineRepository.saveRoutine(routine) }
        _uiState.update { it.copy(dialog = null, exit = WorkoutExit.FINISHED) }
    }

    /** viewModelScope is already cancelled here, so a pending draft is saved in the app scope. */
    override fun onCleared() {
        if (!hasUnsavedEdits) return
        val workout = _uiState.value.toDomain(sessionId)
        applicationScope.launch { workoutRepository.saveDraft(workout) }
    }

    private fun scheduleDraftSave() {
        hasUnsavedEdits = true
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DELAY_MS)
            val workout = _uiState.value.toDomain(sessionId)
            withContext(NonCancellable) { workoutRepository.saveDraft(workout) }
            // Not reached if a newer edit cancelled this job meanwhile: that edit is still unsaved.
            hasUnsavedEdits = false
        }
    }

    private fun presentExerciseIds(state: WorkoutUiState): Set<Int> =
        state.exercises.mapNotNull { it.exercise.id }.toSet()

    /** Applies a change to the logged workout and schedules saving it. */
    private inline fun editWorkout(crossinline transform: (WorkoutUiState) -> WorkoutUiState) {
        _uiState.update { transform(it) }
        scheduleDraftSave()
    }

    private inline fun updateExercise(
        exerciseKey: String,
        crossinline transform: (WorkoutExerciseUiState) -> WorkoutExerciseUiState,
    ) {
        editWorkout { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.key == exerciseKey) transform(exercise) else exercise
                },
            )
        }
    }

    private inline fun updateSet(
        exerciseKey: String,
        setKey: String,
        crossinline transform: (WorkoutSetUiState) -> WorkoutSetUiState,
    ) {
        updateExercise(exerciseKey) { exercise ->
            exercise.copy(
                sets = exercise.sets.map { set ->
                    if (set.key == setKey) transform(set) else set
                },
            )
        }
    }

    companion object {
        fun factory(sessionId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WorkoutViewModel(
                    sessionId = sessionId,
                    workoutRepository = container.workoutRepository,
                    routineRepository = container.routineRepository,
                    exerciseRepository = container.exerciseRepository,
                    settingsRepository = container.settingsRepository,
                    restTimer = container.restTimer,
                    applicationScope = container.applicationScope,
                )
            }
        }
    }
}
