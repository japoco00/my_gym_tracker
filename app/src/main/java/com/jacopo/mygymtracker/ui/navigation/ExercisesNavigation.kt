package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.exercises.ExercisesScreen
import com.jacopo.mygymtracker.ui.exercises.ExercisesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesRoute

fun NavGraphBuilder.exercisesDestination() {
    composable<ExercisesRoute> {
        val viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ExercisesScreen(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onExerciseClick = viewModel::openEditExercise,
            onCreateExercise = viewModel::openNewExercise,
            onDeleteExercise = viewModel::requestDelete,
            onSaveExercise = viewModel::saveExercise,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDialog = viewModel::dismissDialog,
        )
    }
}
