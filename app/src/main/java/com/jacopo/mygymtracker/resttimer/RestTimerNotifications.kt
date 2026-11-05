package com.jacopo.mygymtracker.resttimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.jacopo.mygymtracker.MainActivity
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.UserSettings

/** Channels and notifications of the rest timer. */
object RestTimerNotifications {

    const val COUNTDOWN_ID = 1
    private const val REST_OVER_ID = 2
    private const val COUNTDOWN_CHANNEL = "rest_countdown"
    private const val REST_OVER_CHANNEL = "rest_over"

    fun createChannels(context: Context) {
        context.getSystemService<NotificationManager>()?.createNotificationChannels(
            listOf(
                NotificationChannel(
                    COUNTDOWN_CHANNEL,
                    context.getString(R.string.rest_countdown_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    REST_OVER_CHANNEL,
                    context.getString(R.string.rest_over_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { enableVibration(true) },
            ),
        )
    }

    /** The system settings of the "Rest over" channel, where the user picks its sound and vibration. */
    fun restOverSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, REST_OVER_CHANNEL)

    /** The ongoing countdown. The system chronometer ticks it, so it's re-posted only when [timer] changes. */
    fun countdown(context: Context, timer: RestTimer?): Notification =
        NotificationCompat.Builder(context, COUNTDOWN_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.rest_countdown_title))
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (timer != null) {
                    setWhen(timer.endsAtMillis)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                }
            }
            .addAction(
                0,
                context.getString(R.string.rest_increase, UserSettings.REST_STEP_SECONDS),
                RestTimerService.actionIntent(context, RestTimerService.ACTION_ADD_TIME),
            )
            .addAction(
                0,
                context.getString(R.string.rest_skip),
                RestTimerService.actionIntent(context, RestTimerService.ACTION_SKIP),
            )
            .build()

    /** Heads-up alert with vibration. Posting is a no-op if notifications aren't allowed. */
    fun notifyRestOver(context: Context) {
        val notification = NotificationCompat.Builder(context, REST_OVER_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.rest_over_title))
            .setContentText(context.getString(R.string.rest_over_text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService<NotificationManager>()?.notify(REST_OVER_ID, notification)
    }

    fun cancelRestOver(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(REST_OVER_ID)
    }

    /** Same intent as the launcher icon: brings the running app back as it was. */
    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
