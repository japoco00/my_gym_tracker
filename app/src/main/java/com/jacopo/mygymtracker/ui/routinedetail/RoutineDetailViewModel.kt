package com.jacopo.mygymtracker.ui.routinedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    init {
        loadInitialRoutine()
        observeExerciseCatalog()
    }

    private fun loadInitialRoutine() {
        if (routineId == null) {
            _uiState.update { state ->
                state.copy(
                    name = "New Routine",
                    isLoading = false,
                )
            }
            return
        }
        viewModelScope.launch {
            val routine = routineRepository.getRoutine(routineId)
            _uiState.update { state ->
                routine?.toUiState(catalogExercises = state.catalogExercises)
                    ?: state.copy(name = "New Routine", isLoading = false)
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
        val newExercise = ExerciseUiState(
            name = "",
            category = Category.STRENGTH,
            sets = listOf(SetUiState()),
        )
        _uiState.update { state ->
            state.copy(exercises = state.exercises + newExercise)
        }
    }

    fun addCatalogExercises(selected: List<Exercise>) {
        _uiState.update { state ->
            val existingCatalogIds = state.exercises.mapNotNull { it.id }.toSet()
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

    fun removeExercise(exerciseKey: String) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.filterNot { it.key == exerciseKey })
        }
    }

    fun addSet(exerciseKey: String) {
        updateExercise(exerciseKey) { exercise ->
            val lastSet = exercise.sets.lastOrNull()
            val nextSet = SetUiState(
                reps = lastSet?.reps ?: 0,
                weight = lastSet?.weight ?: 0,
            )
            exercise.copy(sets = exercise.sets + nextSet)
        }
    }

    fun removeSet(exerciseKey: String, setUuid: String) {
        updateExercise(exerciseKey) { exercise ->
            exercise.copy(sets = exercise.sets.filterNot { it.uuid == setUuid })
        }
    }

    fun updateSetReps(exerciseKey: String, setUuid: String, reps: Int) {
        updateSet(exerciseKey, setUuid) { it.copy(reps = reps.coerceAtLeast(0)) }
    }

    fun updateSetWeight(exerciseKey: String, setUuid: String, weight: Int) {
        updateSet(exerciseKey, setUuid) { it.copy(weight = weight.coerceAtLeast(0)) }
    }

    fun saveRoutine(onSaved: () -> Unit = {}) {
        val snapshot = _uiState.value
        if (snapshot.isLoading) {
            onSaved()
            return
        }
        viewModelScope.launch {
            withContext(NonCancellable) {
                routineRepository.saveRoutine(snapshot.toDomain())
            }
            onSaved()
        }
    }

    private inline fun updateExercise(
        exerciseKey: String,
        crossinline transform: (ExerciseUiState) -> ExerciseUiState,
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
        setUuid: String,
        crossinline transform: (SetUiState) -> SetUiState,
    ) {
        updateExercise(exerciseKey) { exercise ->
            exercise.copy(
                sets = exercise.sets.map { set ->
                    if (set.uuid == setUuid) transform(set) else set
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
                )
            }
        }
    }
}
