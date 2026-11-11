package com.jacopo.mygymtracker

import android.app.Application
import com.jacopo.mygymtracker.di.AppContainer
import com.jacopo.mygymtracker.resttimer.RestTimerNotifications
import com.jacopo.mygymtracker.widget.updateWidgetsOnChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MyApplication : Application() {

    /** Centralized dependency container, shared across the whole app. */
    lateinit var container: AppContainer
        private set

    private var widgetUpdates: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RestTimerNotifications.createChannels(this)
    }

    /**
     * Keeps the home screen widgets in sync until the process dies. Started by the UI, the only
     * writer of their data, rather than here: unit tests create the app too and shouldn't open
     * its database. Main thread only.
     */
    fun keepWidgetsUpdated() {
        if (widgetUpdates == null) widgetUpdates = container.applicationScope.launch { updateWidgetsOnChange() }
    }
}
