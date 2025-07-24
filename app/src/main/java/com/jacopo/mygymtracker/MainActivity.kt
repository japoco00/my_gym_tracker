package com.jacopo.mygymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.jacopo.mygymtracker.ui.theme.MyGymTrackerTheme
import com.jacopo.mygymtracker.ui.screen.RoutineScreen
import com.jacopo.mygymtracker.ui.screen.RoutinesListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            MyGymTrackerTheme {
                val appContainer =
                    (LocalContext.current.applicationContext as MyApplication).appContainer
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    MainScreen(
                        appContainer
                    )
                }


            }
        }
    }
}

@Composable
fun MainScreen(appContainer: AppContainer) {
//    RoutineScreen(appContainer = appContainer)
    RoutinesListScreen(appContainer)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyGymTrackerTheme {

    }
}