package com.jacopo.mygymtracker

import android.app.Application
import com.jacopo.mygymtracker.di.AppContainer

class MyApplication : Application() {

    /** Centralized dependency container, shared across the whole app. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
