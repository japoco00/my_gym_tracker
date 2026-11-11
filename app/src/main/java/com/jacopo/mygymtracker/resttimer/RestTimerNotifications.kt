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
import androidx.core.graphics.drawable.IconCompat
import com.jacopo.mygymtracker.MainActivity
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.labelRes
import kotlin.math.ceil

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

    /**
     * The ongoing countdown, with the next set and a bar filling up over the rest. Android 16
     * promotes it to a Live Update: a countdown chip in the status bar and a pinned spot in the
     * shade. The chronometer ticks by itself; the bar moves only when re-posted with a new [nowMillis].
     */
    fun countdown(context: Context, timer: RestTimer?, nowMillis: Long): Notification {
        val next = timer?.nextSet
        return NotificationCompat.Builder(context, COUNTDOWN_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(
                when {
                    next == null -> context.getString(R.string.rest_countdown_title)
                    next.isLast -> context.getString(R.string.rest_last_set, next.exercise.name)
                    else -> context.getString(R.string.rest_up_next, next.exercise.name)
                },
            )
            .setContentText(next?.let { describe(context, it) })
            .setSubText(next?.routineName)
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setSilent(true)
            .setRequestPromotedOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (timer != null) {
                    setWhen(timer.endsAtMillis)
                    setShowWhen(true)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setStyle(restBar(context, timer, nowMillis))
                }
            }
            .addAction(
                0,
                context.getString(R.string.rest_decrease, UserSettings.REST_STEP_SECONDS),
                RestTimerService.actionIntent(context, RestTimerService.ACTION_REMOVE_TIME),
            )
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
    }

    /** One segment as long as the rest, filled by the elapsed seconds. Below Android 16 it's a plain bar. */
    private fun restBar(context: Context, timer: RestTimer, nowMillis: Long): NotificationCompat.ProgressStyle {
        val total = timer.totalSeconds.coerceAtLeast(1)
        val remaining = ceil(timer.remainingMillis(nowMillis) / 1_000.0).toInt()
        return NotificationCompat.ProgressStyle()
            .addProgressSegment(NotificationCompat.ProgressStyle.Segment(total))
            .setProgress((total - remaining).coerceIn(0, total))
            .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_rest_tracker))
    }

    /**
     * Heads-up alert with vibration, naming [next] if known. Posting is a no-op if notifications
     * aren't allowed.
     */
    fun notifyRestOver(context: Context, next: NextSet?) {
        val notification = NotificationCompat.Builder(context, REST_OVER_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(
                if (next == null) {
                    context.getString(R.string.rest_over_title)
                } else {
                    context.getString(R.string.rest_go, next.exercise.name)
                },
            )
            .setContentText(next?.let { describe(context, it) } ?: context.getString(R.string.rest_over_text))
            .setSubText(next?.routineName)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService<NotificationManager>()?.notify(REST_OVER_ID, notification)
    }

    /** E.g. `Set 3 of 4 · 40 kg × 8`, or `Warm-up · 20 kg × 10`. */
    private fun describe(context: Context, next: NextSet): String {
        val res = context.resources
        val name = if (next.number != null) {
            res.getString(R.string.rest_set_of, next.number, next.normalSets)
        } else {
            res.getString(next.set.type.labelRes)
        }
        val target = res.formatSet(
            trackingType = next.exercise.trackingType,
            weightUnit = next.weightUnit,
            reps = next.set.reps,
            weightKg = next.set.weightKg,
            durationSeconds = next.set.durationSeconds,
        )
        return res.getString(R.string.rest_next_set, name, target)
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
