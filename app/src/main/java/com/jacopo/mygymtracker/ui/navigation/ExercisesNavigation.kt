package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.ui.exercises.ExercisesScreen
import com.jacopo.mygymtracker.ui.exercises.ExercisesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesRoute

fun NavGraphBuilder.exercisesDestination(
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
) {
    composable<ExercisesRoute> {
        val viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        TabActionEffect(TopLevelDestination.EXERCISES, stringResource(R.string.exercises_create), viewModel::openNewExercise)

        ExercisesScreen(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onExerciseClick = { exercise -> onOpenExerciseProgress(checkNotNull(exercise.id)) },
            onEditExercise = viewModel::openEditExercise,
            onDeleteExercise = viewModel::requestDelete,
            onSaveExercise = viewModel::saveExercise,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDialog = viewModel::dismissDialog,
        )
    }
}
