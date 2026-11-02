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
            onCreateRoutine = { navController.navigateToRoutineDetail() },
            onOpenRoutine = { routineId -> navController.navigateToRoutineDetail(routineId) },
            onOpenWorkout = { sessionId -> navController.navigateToWorkout(sessionId) },
        )

        routineDetailDestination(
            onNavigateBack = { navController.closeRoutineDetail() },
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
            onOpenExerciseProgress = { exerciseId -> navController.navigateToExerciseProgress(exerciseId) },
        )

        exercisesDestination(
            onOpenExerciseProgress = { exerciseId -> navController.navigateToExerciseProgress(exerciseId) },
        )

        exerciseProgressDestination(
            onNavigateBack = { navController.closeExerciseProgress() },
        )

        settingsDestination()
    }
}
