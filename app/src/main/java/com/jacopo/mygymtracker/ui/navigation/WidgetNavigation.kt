package com.jacopo.mygymtracker.ui.navigation

import androidx.navigation.NavController
import com.jacopo.mygymtracker.widget.WidgetDestination

/** Opens what a widget asked for on top of the current screen, so Back returns to it. */
fun NavController.navigateFromWidget(destination: WidgetDestination) {
    when (destination) {
        is WidgetDestination.Workout -> navigateToWorkout(destination.sessionId)
        WidgetDestination.NewRoutine -> navigateToRoutineDetail()
    }
}
