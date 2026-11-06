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
import com.jacopo.mygymtracker.domain.model.toRoutineExercises
import com.jacopo.mygymtracker.resttimer.RestTimerController
import com.jacopo.mygymtracker.ui.components.EditingSet
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

    /**
     * Routine to save if the user agrees: the workout's own routine rewritten with what was done,
     * or a new routine when the workout was started empty.
     */
    private var pendingRoutine: Routine? = null

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
            val exerciseIds = workout.exercises.mapNotNull { it.exercise.id }
            val previousSets = workoutRepository.getLastPerformance(exerciseIds)
            val earlierSets = workoutRepository.getEarlierSets(exerciseIds, before = workout.startedAt)
            _uiState.update { it.withWorkout(workout, previousSets, earlierSets) }
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
                _uiState.update {
                    it.copy(
                        weightUnit = settings.weightUnit,
                        defaultRestSeconds = settings.restSeconds,
                        plates = settings.currentPlates,
                        autoStartRest = settings.autoStartRest,
                        keepScreenOn = settings.keepScreenOn,
                    )
                }
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
            val earlierSets = workoutRepository.getEarlierSets(ids, before = _uiState.value.startedAt)
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
                    earlierSets = state.earlierSets + earlierSets,
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
                editingSet = state.editingSet?.takeIf { it.exerciseKey != exerciseKey },
                editingRestKey = state.editingRestKey?.takeIf { it != exerciseKey },
            )
        }
    }

    fun openRestEditor(exerciseKey: String) {
        _uiState.update { it.copy(editingRestKey = exerciseKey) }
    }

    fun closeRestEditor() {
        _uiState.update { it.copy(editingRestKey = null) }
    }

    /** Sets the rest after each set of an exercise; null goes back to the default. */
    fun setRest(exerciseKey: String, seconds: Int?) {
        updateExercise(exerciseKey) { it.copy(restSeconds = seconds) }
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
        _uiState.update { state ->
            state.copy(editingSet = state.editingSet?.takeIf { it.setKey != setKey })
        }
    }

    fun openSetEditor(exerciseKey: String, setKey: String) {
        _uiState.update { it.copy(editingSet = EditingSet(exerciseKey, setKey)) }
    }

    fun closeSetEditor() {
        _uiState.update { it.copy(editingSet = null) }
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

    /** Ticking a set off starts the rest before the next one, unless auto-start is off in the settings. */
    fun setCompleted(exerciseKey: String, setKey: String, completed: Boolean) {
        updateSet(exerciseKey, setKey) { it.copy(isCompleted = completed) }
        if (completed && _uiState.value.autoStartRest) startRest(exerciseKey)
    }

    /** Starts the rest of an exercise: its own, or the default. */
    fun startRest(exerciseKey: String) {
        val state = _uiState.value
        val exercise = state.exercises.find { it.key == exerciseKey } ?: return
        restTimer.start(exercise.restSeconds ?: state.defaultRestSeconds)
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

    /**
     * Stores the workout as finished, then offers to update the routine if the plan changed,
     * or to save it as a new routine if it was started empty.
     */
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
            val routineId = workout.routineId
            pendingRoutine = if (routineId == null) {
                Routine(name = workout.routineName, exercises = performedPlan)
            } else {
                routineRepository.getRoutine(routineId)
                    ?.takeIf { it.exercises != performedPlan }
                    ?.copy(exercises = performedPlan)
            }
            val dialog = when {
                pendingRoutine == null -> null
                routineId == null -> WorkoutDialog.SaveAsRoutine
                else -> WorkoutDialog.UpdateRoutine
            }
            _uiState.update {
                if (dialog != null) it.copy(dialog = dialog) else it.copy(exit = WorkoutExit.FINISHED)
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

    /** Answers the "update routine?" or "save as routine?" question. The save outlives this screen. */
    fun resolvePendingRoutine(save: Boolean) {
        val routine = pendingRoutine?.takeIf { save }
        pendingRoutine = null
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
