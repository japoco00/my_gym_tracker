package com.jacopo.mygymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.jacopo.mygymtracker.ui.navigation.Routines
import com.jacopo.mygymtracker.ui.navigation.navigateToRoutineDetails
import com.jacopo.mygymtracker.ui.navigation.routineDetailsDestination
import com.jacopo.mygymtracker.ui.navigation.routinesDestination
import com.jacopo.mygymtracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @OptIn(ExperimentalMaterial3Api::class) setContent {
            AppTheme {
                val navController = rememberNavController()
                val appContainer =
                    (LocalContext.current.applicationContext as MyApplication).appContainer

                Scaffold(
                    topBar = { TopAppBar(title = { Text("My Gym Tracker") }) },
                    bottomBar = {},
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        MyNavHost(navController, appContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun MyNavHost(navController: NavHostController, appContainer: AppContainer) {
    NavHost(navController = navController, startDestination = Routines) {
        val routineRepository = appContainer.routineRepository

        routinesDestination(
            routineRepository,
            onNavigateToRoutineDetails = { routineId ->
                navController.navigateToRoutineDetails(routineId)
            })

        routineDetailsDestination(
            routineRepository,
            onNavigateBack = {
                navController.navigateUp()
            })
    }
}



