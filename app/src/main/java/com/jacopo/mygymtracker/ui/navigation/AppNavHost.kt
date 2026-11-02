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
        )

        routineDetailsDestination(
            onNavigateBack = { navController.navigateUp() },
        )

        exercisesDestination()

        settingsDestination()
    }
}
