package com.jacopo.mygymtracker.resttimer

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.jacopo.mygymtracker.MyApplication
import com.jacopo.mygymtracker.domain.model.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the CPU awake a bit past the rest's end, in case the expiry runs late. */
private const val WAKE_LOCK_MARGIN_MS = 10_000L

/**
 * Shows the running rest as a notification and keeps the CPU awake until it ends, so the
 * countdown stays on time with the screen off. [RestTimerController] owns the countdown;
 * this service only mirrors it and stops itself when the rest is over.
 */
class RestTimerService : Service() {

    private val controller by lazy { (application as MyApplication).container.restTimer }
    private val scope = MainScope()
    private var renderJob: Job? = null
    private var lastStartId = 0
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        wakeLock = checkNotNull(getSystemService<PowerManager>())
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyGymTracker:RestTimer")
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        when (intent?.action) {
            ACTION_ADD_TIME -> controller.adjust(UserSettings.REST_STEP_SECONDS)
            ACTION_SKIP -> controller.skip()
            // startForegroundService() requires this on every start, even if the rest is already over.
            else -> ServiceCompat.startForeground(
                this,
                RestTimerNotifications.COUNTDOWN_ID,
                RestTimerNotifications.countdown(this, controller.state.value),
                // ServiceCompat drops this API 34 type on older releases.
                //noinspection InlinedApi
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        }
        if (renderJob == null) renderJob = scope.launch { controller.state.collect(::render) }
        return START_NOT_STICKY
    }

    private fun render(timer: RestTimer?) {
        if (timer == null) {
            if (wakeLock.isHeld) wakeLock.release()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            // Keeps running if a newer start is already queued.
            stopSelf(lastStartId)
            return
        }
        wakeLock.acquire(timer.remainingMillis(System.currentTimeMillis()) + WAKE_LOCK_MARGIN_MS)
        RestTimerNotifications.cancelRestOver(this)
        getSystemService<NotificationManager>()
            ?.notify(RestTimerNotifications.COUNTDOWN_ID, RestTimerNotifications.countdown(this, timer))
    }

    override fun onDestroy() {
        scope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_ADD_TIME = "com.jacopo.mygymtracker.resttimer.ADD_TIME"
        const val ACTION_SKIP = "com.jacopo.mygymtracker.resttimer.SKIP"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java))
            } catch (_: IllegalStateException) {
                // Android 12+ refuses it from the background. The in-app countdown still runs.
            }
        }

        fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, RestTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
