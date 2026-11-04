package com.jacopo.mygymtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacopo.mygymtracker.ui.navigation.AppNavHost
import com.jacopo.mygymtracker.ui.navigation.TopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.navigateToTopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.toTopLevelDestination

/**
 * App shell: shows the bottom bar only on tab roots, sliding it away when a full-screen
 * destination opens. Each screen owns its own Scaffold and top bar; the shell consumes the insets
 * the bar covers.
 */
@Composable
fun MyGymTrackerApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.toTopLevelDestination()
    // Keeps the tab highlighted while the bar slides away.
    var shownTab by remember { mutableStateOf(TopLevelDestination.ROUTINES) }
    if (currentTab != null) shownTab = currentTab
    val barSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()

    Scaffold(
        bottomBar = {
            // Top-anchored expand/shrink looks like a slide, but resizes the content smoothly
            // instead of snapping it at the end.
            AnimatedVisibility(
                visible = currentTab != null,
                enter = expandVertically(barSpec, expandFrom = Alignment.Top),
                exit = shrinkVertically(barSpec, shrinkTowards = Alignment.Top),
            ) {
                ShortNavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = tab == shownTab,
                            onClick = { navController.navigateToTopLevelDestination(tab) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}
