package com.jacopo.mygymtracker

import android.app.Application
import com.jacopo.mygymtracker.data.db.AppDatabase

class MyApplication : Application() {

    // Container centralizzato delle dipendenze
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        appContainer = AppContainer(database)
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }

    init {
        instance = this
    }
}
