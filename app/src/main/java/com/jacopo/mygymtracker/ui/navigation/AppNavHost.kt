package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
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
    }
}
