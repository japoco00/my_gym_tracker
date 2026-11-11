package com.jacopo.mygymtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacopo.mygymtracker.ui.components.DockAction
import com.jacopo.mygymtracker.ui.components.FloatingDock
import com.jacopo.mygymtracker.ui.navigation.AppNavHost
import com.jacopo.mygymtracker.ui.navigation.LocalTabActions
import com.jacopo.mygymtracker.ui.navigation.TopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.navigateToTopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.toTopLevelDestination

/**
 * App shell: floats the tab dock over tab roots only, sliding it away when a full-screen
 * destination opens. Each screen owns its own Scaffold, insets and top bar; tab screens leave
 * [com.jacopo.mygymtracker.ui.components.FloatingDockClearance] under their content.
 */
@Composable
fun MyGymTrackerApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.toTopLevelDestination()
    // Keeps the tab highlighted while the dock slides away.
    var shownTab by remember { mutableStateOf(TopLevelDestination.HOME) }
    if (currentTab != null) shownTab = currentTab
    val tabActions = remember { mutableStateMapOf<TopLevelDestination, DockAction>() }
    val dockSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    CompositionLocalProvider(LocalTabActions provides tabActions) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AppNavHost(navController = navController)
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically(dockSpec) { it },
                exit = slideOutVertically(dockSpec) { it },
            ) {
                FloatingDock(
                    action = tabActions[shownTab],
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                ) {
                    TopLevelDestination.entries.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = tab == shownTab,
                            onClick = { navController.navigateToTopLevelDestination(tab) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = stringResource(tab.labelRes)) },
                            label = null,
                        )
                    }
                }
            }
        }
    }
}
