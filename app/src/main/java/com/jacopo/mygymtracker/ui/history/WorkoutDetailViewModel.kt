package com.jacopo.mygymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.progress.prSetIndex
import com.jacopo.mygymtracker.ui.components.EditingSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutDetailViewModel(
    private val sessionId: Int,
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    private var deleteJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkout(sessionId)?.takeIf { it.finishedAt != null }
            val prSetIndexes = workout?.let { prSetIndexes(it) }.orEmpty()
            _uiState.update { it.copy(workout = workout, prSetIndexes = prSetIndexes, isClosed = workout == null) }
        }
        viewModelScope.launch {
            exerciseRepository.observeCatalog().collect { catalog ->
                _uiState.update { it.copy(catalogExercises = catalog) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(weightUnit = settings.weightUnit, plates = settings.currentPlates) }
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

    fun startEditing() {
        val workout = _uiState.value.workout ?: return
        _uiState.update { it.copy(editedExercises = workout.exercises.map { exercise -> exercise.toEditableUiState() }) }
    }

    /** Leaves edit mode, dropping the unsaved changes. */
    fun cancelEditing() {
        _uiState.update { it.copy(editedExercises = null, editingSet = null, isAddExercisesSheetOpen = false) }
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
        val ids = selected.mapNotNull { it.id }.toSet() - editedExerciseIds(_uiState.value)
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val lastTime = workoutRepository.getLastPerformance(ids)
            _uiState.update { state ->
                val edited = state.editedExercises ?: return@update state
                val added = selected
                    .filter { it.id in ids && it.id !in editedExerciseIds(state) }
                    .map { exercise ->
                        val sets = lastTime[exercise.id].orEmpty().ifEmpty { listOf(LoggedSet()) }
                        WorkoutExercise(exercise, sets).toEditableUiState()
                    }
                state.copy(editedExercises = edited + added)
            }
        }
    }

    fun openSetEditor(exerciseKey: String, setKey: String) {
        _uiState.update { it.copy(editingSet = EditingSet(exerciseKey, setKey)) }
    }

    fun closeSetEditor() {
        _uiState.update { it.copy(editingSet = null) }
    }

    /** Adds a set that copies the last one, like during a workout. */
    fun addSet(exerciseKey: String) {
        updateExercise(exerciseKey) { exercise ->
            val lastSet = exercise.sets.lastOrNull()
            val nextSet = EditableSetUiState(
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
        _uiState.update { state -> state.copy(editingSet = state.editingSet?.takeIf { it.setKey != setKey }) }
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

    /** Stores the edits; exercises left without sets are dropped. */
    fun saveEdits() {
        val state = _uiState.value
        val edited = state.editedExercises ?: return
        if (!state.canSave || saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            val saved = withContext(NonCancellable) {
                workoutRepository.updateFinishedWorkout(sessionId, edited.map { it.toDomain() })
            }
            val prSetIndexes = saved?.let { prSetIndexes(it) }.orEmpty()
            _uiState.update {
                it.copy(
                    workout = saved,
                    prSetIndexes = prSetIndexes,
                    editedExercises = null,
                    editingSet = null,
                    isClosed = saved == null,
                )
            }
        }
    }

    /** Per exercise of [workout], the set that beat everything done before this workout, if any. */
    private suspend fun prSetIndexes(workout: WorkoutSession): List<Int?> {
        val earlierSets = workoutRepository.getEarlierSets(
            workout.exercises.mapNotNull { it.exercise.id },
            before = workout.startedAt,
        )
        return workout.exercises.map { exercise ->
            prSetIndex(exercise.sets, earlierSets[exercise.exercise.id].orEmpty(), exercise.exercise.trackingType)
        }
    }

    private fun editedExerciseIds(state: WorkoutDetailUiState): Set<Int> =
        state.editedExercises.orEmpty().mapNotNull { it.exercise.id }.toSet()

    private inline fun updateExercise(
        exerciseKey: String,
        crossinline transform: (EditableExerciseUiState) -> EditableExerciseUiState,
    ) {
        _uiState.update { state ->
            state.copy(
                editedExercises = state.editedExercises?.map { exercise ->
                    if (exercise.key == exerciseKey) transform(exercise) else exercise
                },
            )
        }
    }

    private inline fun updateSet(
        exerciseKey: String,
        setKey: String,
        crossinline transform: (EditableSetUiState) -> EditableSetUiState,
    ) {
        updateExercise(exerciseKey) { exercise ->
            exercise.copy(sets = exercise.sets.map { set -> if (set.key == setKey) transform(set) else set })
        }
    }

    companion object {
        fun factory(sessionId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WorkoutDetailViewModel(
                    sessionId = sessionId,
                    workoutRepository = container.workoutRepository,
                    exerciseRepository = container.exerciseRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
