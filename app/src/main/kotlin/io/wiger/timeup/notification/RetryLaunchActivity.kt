package io.wiger.timeup.notification

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import io.wiger.timeup.MainActivity
import io.wiger.timeup.data.model.TimeUp

/**
 * Headless trampoline used by the finished notification. It restarts the countdown in the
 * background and then opens the selected target app, falling back to TimeUp when no target is
 * configured or the target is no longer installed.
 */
class RetryLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alert = IntentCompat.getParcelableExtra(
            intent,
            AlertService.EXTRA_TIME_ALERT,
            TimeUp::class.java,
        )

        if (alert == null) {
            openMainApp()
            finish()
            return
        }

        val serviceIntent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_RESTART_ALERT
            putExtra(AlertService.EXTRA_TIME_ALERT, alert)
        }
        runCatching {
            ContextCompat.startForegroundService(this, serviceIntent)
        }

        val targetIntent = alert.appName
            ?.takeIf { it.isNotBlank() }
            ?.let { packageName ->
                runCatching { packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
            }

        if (targetIntent != null) {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(targetIntent) }
                .onFailure { openMainApp() }
        } else {
            openMainApp()
        }

        finish()
    }

    private fun openMainApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }
}
