package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.ui.components.LocalNavAnimatedVisibilityScope
import com.jacopo.mygymtracker.ui.components.sharedContainer
import com.jacopo.mygymtracker.ui.routines.RoutineDetailScreen
import com.jacopo.mygymtracker.ui.routines.RoutineDetailViewModel
import com.jacopo.mygymtracker.ui.routines.routineContainerKey
import kotlinx.serialization.Serializable

@Serializable
data class RoutineDetailRoute(val routineId: Int? = null)

/**
 * A saved routine grows out of its row and shrinks back into it, so the screen itself only fades.
 * A new routine has no row: it keeps the nav host's slide.
 */
fun NavGraphBuilder.routineDetailDestination(
    onNavigateBack: () -> Unit,
) {
    composable<RoutineDetailRoute>(
        enterTransition = { if (targetState.editsSavedRoutine()) fadeIn() else null },
        popExitTransition = { if (initialState.editsSavedRoutine()) fadeOut() else null },
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<RoutineDetailRoute>()
        val viewModel: RoutineDetailViewModel = viewModel(
            factory = RoutineDetailViewModel.factory(route.routineId, stringResource(R.string.routine_untitled)),
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            RoutineDetailScreen(
                uiState = uiState,
                onBack = onNavigateBack,
                onNameChange = viewModel::updateRoutineName,
                onNoteChange = viewModel::updateRoutineNote,
                onOpenAddExercises = viewModel::openAddExercisesSheet,
                onDismissAddExercises = viewModel::closeAddExercisesSheet,
                onConfirmAddExercises = viewModel::addCatalogExercises,
                onOpenNewExercise = viewModel::openNewExerciseSheet,
                onDismissNewExercise = viewModel::closeNewExerciseSheet,
                onConfirmNewExercise = viewModel::addNewExercise,
                onRemoveExercise = viewModel::removeExercise,
                onAddSet = viewModel::addSet,
                onOpenRestEditor = viewModel::openRestEditor,
                onCloseRestEditor = viewModel::closeRestEditor,
                onRestChange = viewModel::setRest,
                onOpenSetEditor = viewModel::openSetEditor,
                onCloseSetEditor = viewModel::closeSetEditor,
                onDeleteSet = viewModel::removeSet,
                onRepsChange = viewModel::updateSetReps,
                onWeightChange = viewModel::updateSetWeight,
                onDurationChange = viewModel::updateSetDuration,
                modifier = route.routineId?.let { Modifier.sharedContainer(routineContainerKey(it)) } ?: Modifier,
            )
        }
    }
}

/** The entry edits a saved routine, so it has a row in the routines list to grow out of. */
fun NavBackStackEntry.editsSavedRoutine(): Boolean =
    destination.hasRoute<RoutineDetailRoute>() && toRoute<RoutineDetailRoute>().routineId != null

fun NavController.navigateToRoutineDetail(routineId: Int? = null) {
    navigate(route = RoutineDetailRoute(routineId = routineId))
}

/** Pops the editor only, so a repeated call can't pop the screen below it. */
fun NavController.closeRoutineDetail() {
    popBackStack<RoutineDetailRoute>(inclusive = true)
}
