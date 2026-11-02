package com.jacopo.mygymtracker

import android.app.Application
import com.jacopo.mygymtracker.di.AppContainer
import com.jacopo.mygymtracker.resttimer.RestTimerNotifications

class MyApplication : Application() {

    /** Centralized dependency container, shared across the whole app. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RestTimerNotifications.createChannels(this)
    }
}
