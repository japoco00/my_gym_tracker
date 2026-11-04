package com.jacopo.mygymtracker.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.components.EditingSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val SAVE_DELAY_MS = 500L

/**
 * Edits are saved shortly after they happen, so leaving the screen needs no Save button
 * and keeps predictive back working.
 *
 * @param untitledName the localized name for a routine left without one.
 */
class RoutineDetailViewModel(
    private val routineId: Int?,
    private val untitledName: String,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private val saveLock = Mutex()
    private var saveJob: Job? = null
    private var hasUnsavedEdits = false

    init {
        loadInitialRoutine()
        observeExerciseCatalog()
        observeWeightUnit()
    }

    private fun loadInitialRoutine() {
        viewModelScope.launch {
            val routine = routineId?.let { routineRepository.getRoutine(it) }
            _uiState.update { state ->
                routine?.let(state::withRoutine) ?: state.copy(isLoading = false)
            }
        }
    }

    private fun observeWeightUnit() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(weightUnit = settings.weightUnit) }
            }
        }
    }

    private fun observeExerciseCatalog() {
        viewModelScope.launch {
            exerciseRepository.observeCatalog().collect { catalog ->
                _uiState.update { it.copy(catalogExercises = catalog) }
            }
        }
    }

    fun updateRoutineName(name: String) {
        editRoutine { it.copy(name = name) }
    }

    fun updateRoutineNote(note: String) {
        editRoutine { it.copy(note = note.ifBlank { null }) }
    }

    fun openAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = true) }
    }

    fun closeAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = false) }
    }

    fun openNewExerciseSheet() {
        _uiState.update { it.copy(isNewExerciseSheetOpen = true) }
    }

    fun closeNewExerciseSheet() {
        _uiState.update { it.copy(isNewExerciseSheetOpen = false) }
    }

    /** Adds an exercise that isn't in the catalog yet; saving the routine adds it there. */
    fun addNewExercise(name: String, category: Category, trackingType: TrackingType) {
        val exercise = Exercise(name = name.trim(), category = category, trackingType = trackingType)
        editRoutine { state ->
            state.copy(exercises = state.exercises + exercise.toUiState(), isNewExerciseSheetOpen = false)
        }
    }

    fun addCatalogExercises(selected: List<Exercise>) {
        editRoutine { state ->
            // By name: an exercise typed here has no id, but autosave already put it in the catalog.
            val existingNames = state.exercises.map { it.name.lowercase() }.toSet()
            val toAdd = selected
                .filter { it.name.lowercase() !in existingNames }
                .map { it.toUiState() }
            state.copy(
                exercises = state.exercises + toAdd,
                isAddExercisesSheetOpen = false,
            )
        }
    }

    fun removeExercise(exerciseKey: String) {
        editRoutine { state ->
            state.copy(
                exercises = state.exercises.filterNot { it.key == exerciseKey },
                editingSet = state.editingSet?.takeIf { it.exerciseKey != exerciseKey },
            )
        }
    }

    fun addSet(exerciseKey: String) {
        updateExercise(exerciseKey) { exercise ->
            val lastSet = exercise.sets.lastOrNull()
            val nextSet = PlannedSetUiState(
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

    /** viewModelScope is already cancelled here, so pending edits are saved in the app scope. */
    override fun onCleared() {
        if (hasUnsavedEdits) applicationScope.launch { save() }
    }

    /** Applies a change to the routine and schedules saving it. */
    private inline fun editRoutine(crossinline transform: (RoutineDetailUiState) -> RoutineDetailUiState) {
        _uiState.update { transform(it) }
        scheduleSave()
    }

    private fun scheduleSave() {
        hasUnsavedEdits = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DELAY_MS)
            withContext(NonCancellable) { save() }
            // Not reached if a newer edit cancelled this job meanwhile: that edit is still unsaved.
            hasUnsavedEdits = false
        }
    }

    /**
     * Writes the current state. A new routine left blank is not kept: it's never inserted,
     * or deleted if an earlier save already did. The lock keeps saves in order, so the
     * id of a freshly inserted routine is known before the next save, which then updates it.
     */
    private suspend fun save() = saveLock.withLock {
        val state = _uiState.value
        val savedId = if (routineId == null && state.isBlank) {
            state.id?.let { routineRepository.deleteRoutine(it) }
            null
        } else {
            routineRepository.saveRoutine(state.toDomain(untitledName))
        }
        _uiState.update { it.copy(id = savedId) }
    }

    private inline fun updateExercise(
        exerciseKey: String,
        crossinline transform: (RoutineExerciseUiState) -> RoutineExerciseUiState,
    ) {
        editRoutine { state ->
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
        crossinline transform: (PlannedSetUiState) -> PlannedSetUiState,
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
        fun factory(routineId: Int?, untitledName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                RoutineDetailViewModel(
                    routineId = routineId,
                    untitledName = untitledName,
                    routineRepository = container.routineRepository,
                    exerciseRepository = container.exerciseRepository,
                    settingsRepository = container.settingsRepository,
                    applicationScope = container.applicationScope,
                )
            }
        }
    }
}
