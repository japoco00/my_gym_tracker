package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = RoutinesRoute,
        modifier = modifier,
    ) {
        routinesDestination(
            onCreateRoutine = { navController.navigateToRoutineDetails() },
            onOpenRoutine = { routineId -> navController.navigateToRoutineDetails(routineId) },
            onOpenWorkout = { sessionId -> navController.navigateToWorkout(sessionId) },
        )

        routineDetailsDestination(
            onNavigateBack = { navController.navigateUp() },
        )

        workoutDestination(
            onNavigateBack = { navController.closeWorkout() },
            onFinished = { sessionId -> navController.openFinishedWorkout(sessionId) },
        )

        historyDestination(
            onOpenWorkoutDetail = { sessionId -> navController.navigateToWorkoutDetail(sessionId) },
        )

        workoutDetailDestination(
            onNavigateBack = { navController.closeWorkoutDetail() },
        )

        exercisesDestination()

        exerciseProgressDestination(
            onNavigateBack = { navController.closeExerciseProgress() },
        )

        settingsDestination()
    }
}
