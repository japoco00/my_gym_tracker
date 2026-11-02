package com.jacopo.mygymtracker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacopo.mygymtracker.ui.navigation.AppNavHost
import com.jacopo.mygymtracker.ui.navigation.TopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.navigateToTopLevelDestination
import com.jacopo.mygymtracker.ui.navigation.toTopLevelDestination

/**
 * App shell: shows the bottom bar only on tab roots.
 * Each screen owns its own Scaffold and top bar; the shell consumes the insets the bar covers.
 */
@Composable
fun MyGymTrackerApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.toTopLevelDestination()

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
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
