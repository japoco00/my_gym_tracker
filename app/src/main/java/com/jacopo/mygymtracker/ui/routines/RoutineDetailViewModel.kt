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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineDetailViewModel(
    private val routineId: Int?,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null

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
        _uiState.update { it.copy(name = name) }
    }

    fun updateRoutineNote(note: String) {
        _uiState.update { it.copy(note = note.ifBlank { null }) }
    }

    fun openAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = true) }
    }

    fun closeAddExercisesSheet() {
        _uiState.update { it.copy(isAddExercisesSheetOpen = false) }
    }

    fun addEmptyExercise() {
        val newExercise = RoutineExerciseUiState(
            name = "",
            category = Category.OTHER,
            sets = listOf(PlannedSetUiState()),
        )
        _uiState.update { state ->
            state.copy(exercises = state.exercises + newExercise)
        }
    }

    fun addCatalogExercises(selected: List<Exercise>) {
        _uiState.update { state ->
            val existingCatalogIds = state.exercises.mapNotNull { it.exerciseId }.toSet()
            val toAdd = selected
                .filter { it.id == null || it.id !in existingCatalogIds }
                .map { it.toUiState() }
            state.copy(
                exercises = state.exercises + toAdd,
                isAddExercisesSheetOpen = false,
            )
        }
    }

    fun updateExerciseName(exerciseKey: String, name: String) {
        updateExercise(exerciseKey) { it.copy(name = name) }
    }

    fun updateExerciseCategory(exerciseKey: String, category: Category) {
        updateExercise(exerciseKey) { it.copy(category = category) }
    }

    fun updateExerciseTrackingType(exerciseKey: String, trackingType: TrackingType) {
        updateExercise(exerciseKey) { it.copy(trackingType = trackingType) }
    }

    fun removeExercise(exerciseKey: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.filterNot { it.key == exerciseKey })
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

    /**
     * Saves the routine, then closes the editor. An empty new routine is not saved at all.
     * [untitledName] is the localized name for a routine left without one.
     */
    fun saveRoutine(untitledName: String) {
        // A second back press while saving must not insert the new routine twice.
        if (saveJob != null || _uiState.value.isClosed) return
        val routine = _uiState.value.takeUnless { it.isLoading }?.toDomainOrNull(untitledName)
        if (routine == null) {
            _uiState.update { it.copy(isClosed = true) }
            return
        }
        saveJob = viewModelScope.launch {
            withContext(NonCancellable) {
                routineRepository.saveRoutine(routine)
            }
            _uiState.update { it.copy(isClosed = true) }
        }
    }

    private inline fun updateExercise(
        exerciseKey: String,
        crossinline transform: (RoutineExerciseUiState) -> RoutineExerciseUiState,
    ) {
        _uiState.update { state ->
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
        fun factory(routineId: Int?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                RoutineDetailViewModel(
                    routineId = routineId,
                    routineRepository = container.routineRepository,
                    exerciseRepository = container.exerciseRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
