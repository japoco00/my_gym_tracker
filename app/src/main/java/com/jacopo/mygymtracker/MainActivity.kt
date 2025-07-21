package com.jacopo.mygymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jacopo.mygymtracker.ui.theme.MyGymTrackerTheme
import com.jacopo.mygymtracker.ui.view.RoutineScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyGymTrackerTheme {

                Greeting(
                )

            }
        }
    }
}

@Composable
fun Greeting() {
    RoutineScreen()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyGymTrackerTheme {

    }
}