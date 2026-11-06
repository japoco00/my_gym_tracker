package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.jacopo.mygymtracker.ui.components.LocalSharedTransitionScope

/**
 * Pushing a screen slides it in from the right while the old one drifts left; popping reverses it,
 * and predictive back scrubs the same motion. Switching tab is a plain fade: tabs are siblings.
 * Destinations can override this, e.g. a saved routine grows out of its row instead.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = RoutinesRoute,
                enterTransition = {
                    if (isTabSwitch()) fadeIn(fade) else slideInHorizontally(slide) { it / 4 } + fadeIn(fade)
                },
                exitTransition = {
                    if (isTabSwitch()) fadeOut(fade) else slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade)
                },
                popEnterTransition = {
                    if (isTabSwitch()) fadeIn(fade) else slideInHorizontally(slide) { -it / 4 } + fadeIn(fade)
                },
                popExitTransition = {
                    if (isTabSwitch()) fadeOut(fade) else slideOutHorizontally(slide) { it / 4 } + fadeOut(fade)
                },
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
                    onOpenBodyWeight = { navController.navigateToBodyWeight() },
                )

                bodyWeightDestination(
                    onNavigateBack = { navController.closeBodyWeight() },
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
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.toTopLevelDestination() != null &&
        targetState.destination.toTopLevelDestination() != null
