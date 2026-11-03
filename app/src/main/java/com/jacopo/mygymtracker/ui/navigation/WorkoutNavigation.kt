package com.jacopo.mygymtracker.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.ui.workout.WorkoutExit
import com.jacopo.mygymtracker.ui.workout.WorkoutScreen
import com.jacopo.mygymtracker.ui.workout.WorkoutViewModel
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutRoute(val sessionId: Int)

/** Leaving the screen keeps the workout running; it ends only through Finish or Discard. */
fun NavGraphBuilder.workoutDestination(
    onNavigateBack: () -> Unit,
    onFinished: (sessionId: Int) -> Unit,
) {
    composable<WorkoutRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<WorkoutRoute>()
        val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory(route.sessionId))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.exit) {
            when (uiState.exit) {
                WorkoutExit.FINISHED -> onFinished(route.sessionId)
                WorkoutExit.CLOSED -> onNavigateBack()
                null -> Unit
            }
        }
        RequestNotificationPermission()

        WorkoutScreen(
            uiState = uiState,
            onBack = onNavigateBack,
            onFinish = viewModel::requestFinish,
            onDiscard = viewModel::requestDiscard,
            onOpenAddExercises = viewModel::openAddExercisesSheet,
            onDismissAddExercises = viewModel::closeAddExercisesSheet,
            onConfirmAddExercises = viewModel::addExercises,
            onRemoveExercise = viewModel::requestRemoveExercise,
            onAddSet = viewModel::addSet,
            onOpenSetEditor = viewModel::openSetEditor,
            onCloseSetEditor = viewModel::closeSetEditor,
            onDeleteSet = viewModel::removeSet,
            onRepsChange = viewModel::updateSetReps,
            onWeightChange = viewModel::updateSetWeight,
            onDurationChange = viewModel::updateSetDuration,
            onSetCompletedChange = viewModel::setCompleted,
            onAdjustRest = viewModel::adjustRest,
            onSkipRest = viewModel::skipRest,
            onConfirmFinish = viewModel::confirmFinish,
            onConfirmDiscard = viewModel::confirmDiscard,
            onConfirmRemoveExercise = viewModel::confirmRemoveExercise,
            onResolveRoutineUpdate = viewModel::resolveRoutineUpdate,
            onDismissDialog = viewModel::dismissDialog,
        )
    }
}

/**
 * The rest timer alerts through notifications, which need a runtime permission on Android 13+.
 * Asked once per visit; if denied, the in-app timer still works.
 */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!asked && !granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

fun NavController.navigateToWorkout(sessionId: Int) {
    navigate(route = WorkoutRoute(sessionId = sessionId)) { launchSingleTop = true }
}

/** Replaces the workout screen with its summary, so Back returns to where the workout was started. */
fun NavController.openFinishedWorkout(sessionId: Int) {
    navigate(route = WorkoutDetailRoute(sessionId = sessionId)) {
        popUpTo<WorkoutRoute> { inclusive = true }
        launchSingleTop = true
    }
}

/** Pops the workout screen only, so a repeated call can't pop the screen below it. */
fun NavController.closeWorkout() {
    popBackStack<WorkoutRoute>(inclusive = true)
}
