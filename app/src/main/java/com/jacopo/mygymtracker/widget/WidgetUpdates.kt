package com.jacopo.mygymtracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Redraws the widgets every time their data changes. Never returns. */
suspend fun Context.updateWidgetsOnChange() {
    observeWidgetData().collect {
        UpNextWidget().updateAll(this)
        WeekWidget().updateAll(this)
    }
}
