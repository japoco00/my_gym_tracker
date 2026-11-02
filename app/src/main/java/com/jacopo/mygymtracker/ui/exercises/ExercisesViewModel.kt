package com.jacopo.mygymtracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.di.appContainer
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dialog shown on top of the exercise list. */
sealed interface ExerciseDialog {
    /** Editor for a new ([exercise] `null`) or existing exercise; [rejectedName] was refused as a duplicate. */
    data class Edit(val exercise: Exercise?, val rejectedName: String? = null) : ExerciseDialog

    data class ConfirmDelete(val exercise: Exercise) : ExerciseDialog

    /** Explains why [exercise] cannot be deleted. */
    data class InUse(val exercise: Exercise) : ExerciseDialog
}

data class ExercisesUiState(
    val catalog: List<Exercise> = emptyList(),
    val query: String = "",
    val categoryFilter: Category? = null,
    val dialog: ExerciseDialog? = null,
    val isLoading: Boolean = true,
) {
    val visibleExercises: List<Exercise> = catalog.filter {
        (categoryFilter == null || it.category == categoryFilter) &&
            it.name.contains(query.trim(), ignoreCase = true)
    }
}

class ExercisesViewModel(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    // Updated synchronously so the search field never lags behind typing.
    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            exerciseRepository.observeCatalog().collect { catalog ->
                _uiState.update { it.copy(catalog = catalog, isLoading = false) }
            }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun onCategoryFilterChange(category: Category?) = _uiState.update { it.copy(categoryFilter = category) }

    fun openNewExercise() = showDialog(ExerciseDialog.Edit(exercise = null))

    fun openEditExercise(exercise: Exercise) = showDialog(ExerciseDialog.Edit(exercise))

    fun dismissDialog() = showDialog(null)

    fun saveExercise(name: String, category: Category) {
        val edit = _uiState.value.dialog as? ExerciseDialog.Edit ?: return
        if (saveJob?.isActive == true) return
        val exercise = edit.exercise?.copy(name = name, category = category)
            ?: Exercise(name = name, category = category)
        saveJob = viewModelScope.launch {
            val saved = exerciseRepository.saveExercise(exercise)
            showDialog(if (saved) null else edit.copy(rejectedName = name.trim()))
        }
    }

    /** Asks for confirmation, or explains why the exercise cannot be deleted. */
    fun requestDelete(exercise: Exercise) {
        val id = exercise.id ?: return
        viewModelScope.launch {
            val inUse = exerciseRepository.isInUse(id)
            showDialog(if (inUse) ExerciseDialog.InUse(exercise) else ExerciseDialog.ConfirmDelete(exercise))
        }
    }

    fun confirmDelete(exercise: Exercise) {
        val id = exercise.id ?: return
        viewModelScope.launch {
            val deleted = exerciseRepository.deleteExercise(id)
            showDialog(if (deleted) null else ExerciseDialog.InUse(exercise))
        }
    }

    private fun showDialog(dialog: ExerciseDialog?) = _uiState.update { it.copy(dialog = dialog) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExercisesViewModel(exerciseRepository = appContainer().exerciseRepository)
            }
        }
    }
}
