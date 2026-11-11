package com.jacopo.mygymtracker.widget

import android.content.Context
import android.content.Intent
import com.jacopo.mygymtracker.MainActivity

/** What a widget's button asks the app to do. */
sealed interface WidgetRequest {
    data class Start(val routineId: Int) : WidgetRequest
    data class Resume(val sessionId: Int) : WidgetRequest
    data object CreateRoutine : WidgetRequest
}

/** The screen a [WidgetRequest] resolves to. */
sealed interface WidgetDestination {
    data class Workout(val sessionId: Int) : WidgetDestination
    data object NewRoutine : WidgetDestination
}

private const val ACTION_START = "com.jacopo.mygymtracker.widget.START"
private const val ACTION_RESUME = "com.jacopo.mygymtracker.widget.RESUME"
private const val ACTION_CREATE_ROUTINE = "com.jacopo.mygymtracker.widget.CREATE_ROUTINE"
private const val EXTRA_ROUTINE_ID = "routineId"
private const val EXTRA_SESSION_ID = "sessionId"

/**
 * Opens [MainActivity] carrying [request]; `null` just opens the app. CLEAR_TOP drops anything
 * above the activity, like a file picker, so the request lands on the app.
 */
fun Context.widgetIntent(request: WidgetRequest? = null): Intent {
    val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return when (request) {
        null -> intent.setAction(Intent.ACTION_MAIN)
        is WidgetRequest.Start -> intent.setAction(ACTION_START).putExtra(EXTRA_ROUTINE_ID, request.routineId)
        is WidgetRequest.Resume -> intent.setAction(ACTION_RESUME).putExtra(EXTRA_SESSION_ID, request.sessionId)
        WidgetRequest.CreateRoutine -> intent.setAction(ACTION_CREATE_ROUTINE)
    }
}

/** The request this intent carries, `null` for any other intent or when Recents relaunches an old one. */
fun Intent.widgetRequest(): WidgetRequest? {
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
    return when (action) {
        ACTION_START -> WidgetRequest.Start(getIntExtra(EXTRA_ROUTINE_ID, 0))
        ACTION_RESUME -> WidgetRequest.Resume(getIntExtra(EXTRA_SESSION_ID, 0))
        ACTION_CREATE_ROUTINE -> WidgetRequest.CreateRoutine
        else -> null
    }
}
