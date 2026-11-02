package com.jacopo.mygymtracker.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jacopo.mygymtracker.R

/** Tabs of the bottom navigation bar. Every other screen opens full-screen on top of them. */
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    ROUTINES(RoutinesRoute, R.drawable.ic_fitness_center, R.string.nav_routines),
    HISTORY(HistoryRoute, R.drawable.ic_history, R.string.nav_history),
    EXERCISES(ExercisesRoute, R.drawable.ic_list, R.string.nav_exercises),
    SETTINGS(SettingsRoute, R.drawable.ic_settings, R.string.nav_settings),
}

/** The tab whose root screen is [this] destination, or `null` for full-screen destinations. */
fun NavDestination?.toTopLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { tab ->
        this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

/** Switches tab keeping a single copy of each tab and restoring its previous state. */
fun NavController.navigateToTopLevelDestination(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
