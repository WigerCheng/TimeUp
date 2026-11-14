package io.wiger.timeup.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import dagger.hilt.android.AndroidEntryPoint
import io.wiger.timeup.MainActivity
import io.wiger.timeup.R
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.data.repository.TimeUpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import javax.inject.Inject

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class AlertService : Service() {

    @Inject
    lateinit var timeUpRepository: TimeUpRepository

    private val activeAlerts = LinkedHashMap<Long, ActiveAlert>()
    private var foregroundAlertId: Long? = null
    private var liveUpdatesEnabled = false

    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dbScope = CoroutineScope(SupervisorJob() + dbExecutor.asCoroutineDispatcher())
    private val dbMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        liveUpdatesEnabled = NotificationUtil.isLiveNotificationEnabled(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alert = intent?.let {
            IntentCompat.getParcelableExtra(it, EXTRA_TIME_ALERT, TimeUp::class.java)
        }

        when (intent?.action) {
            ACTION_STOP_ALERT -> if (alert?.id != null) stopAlert(alert.id) else stopServiceIfIdle()
            ACTION_RESTART_ALERT -> if (alert?.id != null) restartAlert(alert) else stopServiceIfIdle()
            ACTION_DISMISS_ALERT -> if (alert != null) dismissAlert(alert) else stopServiceIfIdle()
            ACTION_START_ALERT, null -> if (alert?.id != null) addAlert(alert) else stopServiceIfIdle()
            else -> if (alert?.id != null) addAlert(alert) else stopServiceIfIdle()
        }

        ensureForeground()
        return START_NOT_STICKY
    }

    private fun addAlert(alert: TimeUp) {
        val id = alert.id ?: return
        if (activeAlerts.containsKey(id)) {
            ensureForeground()
            return
        }

        liveUpdatesEnabled = NotificationUtil.isLiveNotificationEnabled(this)

        val nowWall = System.currentTimeMillis()
        val endWall = alert.endsAtMillis
            ?.takeIf { it > 0L }
            ?: (nowWall + alert.alertTime.coerceAtLeast(0L))
        val remaining = (endWall - nowWall).coerceAtLeast(0L)
        val sessionTimeUp = if (alert.endsAtMillis == endWall) {
            alert
        } else {
            alert.copy(endsAtMillis = endWall)
        }

        if (remaining <= 0L) {
            enqueueEnabledUpdate(sessionTimeUp, false) {
                if (activeAlerts.isEmpty()) {
                    stopServiceNow()
                }
            }
            return
        }

        if (alert.endsAtMillis == null) {
            enqueueTimeUpUpdate(sessionTimeUp.copy(isEnabled = true))
        }

        val session = ActiveAlert(
            timeUp = sessionTimeUp,
            endElapsedRealtime = SystemClock.elapsedRealtime() + remaining,
            displayName = alert.alertName.ifBlank { getString(R.string.untitled_countdown) },
            appLabel = resolveAppLabel(alert.appName),
            largeIcon = resolveAppIcon(alert.appName),
        )
        activeAlerts[id] = session

        val notification = buildProgressNotification(session)
        if (foregroundAlertId == null) {
            promoteToForeground(id, notification)
            if (foregroundAlertId == null) {
                // Keep a visible notification even if the foreground promotion failed; the
                // ticker retries ensureForeground().
                notificationManager.notify(notificationId(id), notification)
            }
        } else {
            ensureForeground()
            notificationManager.notify(notificationId(id), notification)
        }

        scheduleTicking()
    }

    private fun stopAlert(id: Long) {
        val session = activeAlerts.remove(id)
        if (session == null) {
            notificationManager.cancel(notificationId(id))
            stopServiceIfIdle()
            return
        }

        if (foregroundAlertId == id) {
            foregroundAlertId = null
            activeAlerts.entries.firstOrNull()?.let { (nextId, nextSession) ->
                promoteToForeground(nextId, buildProgressNotification(nextSession))
            }
        }

        notificationManager.cancel(notificationId(id))
        if (activeAlerts.isEmpty()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }

        enqueueEnabledUpdate(session.timeUp, false) {
            if (activeAlerts.isEmpty()) {
                stopServiceNow()
            }
        }
    }

    private fun finishAlert(id: Long) {
        val session = activeAlerts.remove(id) ?: return

        if (foregroundAlertId == id) {
            foregroundAlertId = null
            activeAlerts.entries.firstOrNull()?.let { (nextId, nextSession) ->
                promoteToForeground(nextId, buildProgressNotification(nextSession))
            }
        }

        notificationManager.notify(notificationId(id), buildFinishedNotification(session))

        enqueueEnabledUpdate(session.timeUp, false) {
            if (activeAlerts.isEmpty()) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun restartAlert(alert: TimeUp) {
        val id = alert.id ?: run {
            stopServiceIfIdle()
            return
        }
        if (activeAlerts.containsKey(id)) return
        if (alert.alertTime <= 0L) {
            enqueueEnabledUpdate(alert, false)
            stopServiceIfIdle()
            return
        }

        val now = System.currentTimeMillis()
        val restarted = alert.copy(
            isEnabled = true,
            endsAtMillis = now + alert.alertTime
        )
        addAlert(restarted)
        enqueueEnabledUpdate(restarted, true)
    }

    private fun dismissAlert(alert: TimeUp) {
        val id = alert.id ?: return
        if (activeAlerts.containsKey(id)) return

        notificationManager.cancel(notificationId(id))

        // Covers the edge case where "ignore" is tapped before the finish handler
        // has persisted the disabled state.
        enqueueEnabledUpdate(alert, false) {
            if (activeAlerts.isEmpty()) {
                stopServiceNow()
            }
        }
    }

    private fun promoteToForeground(id: Long, notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    notificationId(id),
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
                )
            } else {
                startForeground(notificationId(id), notification)
            }
        }.onSuccess {
            foregroundAlertId = id
        }.onFailure {
            foregroundAlertId = null
            Log.w(TAG, "Unable to promote alert $id to the foreground", it)
        }
    }

    /**
     * Guarantees that the service is in the foreground while at least one countdown is active.
     * Called on every new alert, command and timer tick; handles a previously failed
     * startForeground() or a stale foreground id.
     */
    private fun ensureForeground() {
        if (activeAlerts.isEmpty()) return

        val nowElapsed = SystemClock.elapsedRealtime()
        activeAlerts.keys
            .filter { id ->
                activeAlerts[id]?.let { nowElapsed >= it.endElapsedRealtime } == true
            }
            .forEach(::finishAlert)

        if (activeAlerts.isEmpty()) return

        val currentId = foregroundAlertId
        if (currentId != null && activeAlerts.containsKey(currentId)) return

        foregroundAlertId = null
        val (id, session) = activeAlerts.entries.first()
        promoteToForeground(id, buildProgressNotification(session))
    }

    private fun stopServiceIfIdle() {
        if (activeAlerts.isEmpty()) {
            stopServiceNow()
        }
    }

    private fun stopServiceNow() {
        foregroundAlertId = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleTicking() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (activeAlerts.isEmpty()) return

            liveUpdatesEnabled = NotificationUtil.isLiveNotificationEnabled(this@AlertService)
            val nowElapsed = SystemClock.elapsedRealtime()

            // Finish expired alerts before rebuilding a foreground progress notification.
            for (id in activeAlerts.keys.toList()) {
                val session = activeAlerts[id] ?: continue
                if (CountdownTiming.remainingMillis(session.endElapsedRealtime, nowElapsed) == 0L) {
                    finishAlert(id)
                }
            }

            if (activeAlerts.isEmpty()) return

            ensureForeground()

            for (id in activeAlerts.keys.toList()) {
                val session = activeAlerts[id] ?: continue
                notificationManager.notify(
                    notificationId(id),
                    buildProgressNotification(session, nowElapsed),
                )
            }

            if (activeAlerts.isNotEmpty()) {
                val nextEnd = activeAlerts.values.minOfOrNull { it.endElapsedRealtime }
                val delay = CountdownTiming.nextTickDelay(
                    nowElapsed = SystemClock.elapsedRealtime(),
                    nextEndElapsed = nextEnd,
                )
                handler.postDelayed(this, delay)
            }
        }
    }

    private fun buildProgressNotification(
        session: ActiveAlert,
        nowElapsed: Long = SystemClock.elapsedRealtime(),
    ): Notification {
        val alert = session.timeUp
        val totalDuration = alert.alertTime.coerceAtLeast(0L)
        val remaining = CountdownTiming.remainingMillis(
            endElapsed = session.endElapsedRealtime,
            nowElapsed = nowElapsed,
        )
        val progress = CountdownTiming.progress(totalDuration, remaining)

        val contentText = session.appLabel
            ?.let { getString(R.string.notification_running_target_text, it) }
            ?: getString(R.string.notification_running_text)

        val builder = NotificationUtil.baseNotification(this)
            .setContentTitle(session.displayName)
            .setContentText(contentText)
            .setContentIntent(openMainAppPendingIntent(alert))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setRequestPromotedOngoing(liveUpdatesEnabled)
            .setWhen(System.currentTimeMillis() + remaining)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setProgress(progress.maxSeconds, progress.currentSeconds, false)
            .addAction(stopAction(alert))

        session.largeIcon?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    private fun buildFinishedNotification(session: ActiveAlert): Notification {
        val alert = session.timeUp
        val title = if (alert.alertName.isBlank()) {
            getString(R.string.notification_finished_title_no_name)
        } else {
            getString(R.string.notification_finished_title, session.displayName)
        }

        val builder = NotificationUtil.baseNotification(this)
            .setContentTitle(title)
            .setContentText(
                getString(
                    if (alert.appName.isNullOrBlank()) {
                        R.string.notification_finished_text
                    } else {
                        R.string.notification_finished_text_target
                    }
                )
            )
            .setContentIntent(openRetryPendingIntent(alert))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setRequestPromotedOngoing(false)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setProgress(0, 0, false)
            .addAction(dismissAction(alert))

        session.largeIcon?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    private fun stopAction(alert: TimeUp): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            IconCompat.createWithResource(this, R.drawable.stop),
            getString(R.string.notification_action_stop),
            servicePendingIntent(alert, ACTION_STOP_ALERT, REQ_STOP),
        ).build()

    private fun dismissAction(alert: TimeUp): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            IconCompat.createWithResource(this, R.drawable.close),
            getString(R.string.notification_action_dismiss),
            servicePendingIntent(alert, ACTION_DISMISS_ALERT, REQ_DISMISS),
        ).build()

    private fun servicePendingIntent(
        alert: TimeUp,
        action: String,
        offset: Int,
    ): PendingIntent {
        val id = alert.id ?: 0L
        val intent = Intent(this, AlertService::class.java).apply {
            this.action = action
            putExtra(EXTRA_TIME_ALERT, alert)
        }
        return requireNotNull(
            PendingIntentCompat.getService(
                this,
                requestCode(id, offset),
                intent,
                PI_FLAGS,
                false,
            )
        )
    }

    private fun openMainAppPendingIntent(alert: TimeUp): PendingIntent {
        val id = alert.id ?: 0L
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return requireNotNull(
            PendingIntentCompat.getActivity(
                this,
                requestCode(id, REQ_CONTENT),
                intent,
                PI_FLAGS,
                false,
            )
        )
    }

    private fun openRetryPendingIntent(alert: TimeUp): PendingIntent {
        val id = alert.id ?: 0L
        val intent = Intent(this, RetryLaunchActivity::class.java).apply {
            putExtra(EXTRA_TIME_ALERT, alert)
        }
        return requireNotNull(
            PendingIntentCompat.getActivity(
                this,
                requestCode(id, REQ_CONTENT),
                intent,
                PI_FLAGS,
                false,
            )
        )
    }

    private fun resolveAppLabel(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrNull()
    }

    private fun resolveAppIcon(packageName: String?): Bitmap? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            packageManager.getApplicationIcon(packageName).toBitmapOrNull()
        }.getOrNull()
    }

    private fun enqueueEnabledUpdate(
        timeUp: TimeUp,
        enabled: Boolean,
        onDone: (() -> Unit)? = null,
    ) {
        val updated = if (enabled) {
            timeUp.copy(isEnabled = true)
        } else {
            timeUp.copy(isEnabled = false, endsAtMillis = null)
        }
        enqueueTimeUpUpdate(updated, onDone)
    }

    private fun enqueueTimeUpUpdate(
        timeUp: TimeUp,
        onDone: (() -> Unit)? = null,
    ) {
        if (timeUp.id == null) {
            onDone?.let { callback -> handler.post { callback() } }
            return
        }

        dbScope.launch {
            runCatching {
                withTimeoutOrNull(DB_WRITE_TIMEOUT_MS) {
                    dbMutex.withLock {
                        timeUpRepository.updateTimeUp(timeUp)
                    }
                }
            }.onFailure {
                Log.w(TAG, "Unable to persist alert ${timeUp.id}", it)
            }

            onDone?.let { callback -> handler.post { callback() } }
        }
    }

    private fun notificationId(id: Long): Int = NOTIFY_ID_BASE + id.toInt()

    private fun requestCode(id: Long, offset: Int): Int = (id.toInt() * 10) + offset

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        activeAlerts.clear()
        foregroundAlertId = null
        dbScope.cancel()
        dbExecutor.shutdown()
        super.onDestroy()
    }

    private data class ActiveAlert(
        val timeUp: TimeUp,
        val endElapsedRealtime: Long,
        val displayName: String,
        val appLabel: String?,
        val largeIcon: Bitmap?,
    )

    companion object {
        const val EXTRA_TIME_ALERT = "extra_time_alert"
        const val ACTION_START_ALERT = "action_start_alert"
        const val ACTION_STOP_ALERT = "action_stop_alert"
        const val ACTION_RESTART_ALERT = "action_restart_alert"
        const val ACTION_DISMISS_ALERT = "action_dismiss_alert"

        private const val TAG = "AlertService"
        private const val NOTIFY_ID_BASE = 2_000
        private const val REQ_CONTENT = 0
        private const val REQ_STOP = 1
        private const val REQ_DISMISS = 3
        private const val DB_WRITE_TIMEOUT_MS = 5_000L

        private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT
    }
}
