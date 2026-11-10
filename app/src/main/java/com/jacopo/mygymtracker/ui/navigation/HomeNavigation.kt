package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.ui.home.HomeScreen
import com.jacopo.mygymtracker.ui.home.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeDestination(
    onOpenSettings: () -> Unit,
    onCreateRoutine: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenWorkout: (sessionId: Int) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkoutDetail: (sessionId: Int) -> Unit,
    onOpenBodyWeight: () -> Unit,
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
) {
    composable<HomeRoute> {
        val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.startedWorkoutId) {
            uiState.startedWorkoutId?.let { sessionId ->
                onOpenWorkout(sessionId)
                viewModel.onWorkoutOpened()
            }
        }

        val emptyWorkoutName = stringResource(R.string.workout_default_name)
        HomeScreen(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onStartWorkout = viewModel::startWorkout,
            onStartEmptyWorkout = { viewModel.startEmptyWorkout(emptyWorkoutName) },
            onCreateRoutine = onCreateRoutine,
            onOpenRoutines = onOpenRoutines,
            onResumeWorkout = onOpenWorkout,
            onDiscardActiveAndStart = viewModel::discardActiveAndStart,
            onDismissStartConflict = viewModel::dismissStartConflict,
            onOpenHistory = onOpenHistory,
            onOpenWorkoutDetail = onOpenWorkoutDetail,
            onOpenBodyWeight = onOpenBodyWeight,
            onOpenExerciseProgress = onOpenExerciseProgress,
        )
    }
}
