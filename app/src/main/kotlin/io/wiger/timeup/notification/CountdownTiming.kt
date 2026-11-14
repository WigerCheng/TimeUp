package io.wiger.timeup.notification

/**
 * Pure timing helpers for notification countdowns. Keeping these calculations separate makes
 * the boundary conditions (finished alerts, sub-second ends, multiple alerts) testable.
 */
internal object CountdownTiming {

    data class Progress(val maxSeconds: Int, val currentSeconds: Int)

    fun remainingMillis(endElapsed: Long, nowElapsed: Long): Long =
        (endElapsed - nowElapsed).coerceAtLeast(0L)

    fun progress(totalDuration: Long, remaining: Long): Progress {
        val total = totalDuration.coerceAtLeast(0L)
        val elapsed = (total - remaining).coerceIn(0L, total)
        val maxSeconds = (total / 1_000L)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        // The chronometer shows 00:00 during the final sub-second. Keep the progress bar at
        // the end at the same moment instead of leaving one more visible segment behind.
        val currentSeconds = if (remaining < 1_000L) {
            maxSeconds
        } else {
            (elapsed / 1_000L)
                .coerceIn(0L, maxSeconds.toLong())
                .toInt()
        }
        return Progress(maxSeconds = maxSeconds, currentSeconds = currentSeconds)
    }

    /**
     * Returns the delay until the next visual second boundary, or until the nearest alert ends,
     * whichever comes first. This prevents a countdown from running past zero while waiting for
     * the next whole-second tick.
     */
    fun nextTickDelay(nowElapsed: Long, nextEndElapsed: Long?): Long {
        val nextSecond = (1_000L - (nowElapsed % 1_000L)).coerceIn(1L, 1_000L)
        val untilEnd = nextEndElapsed?.let { (it - nowElapsed).coerceAtLeast(0L) }
        val delay = if (untilEnd != null) minOf(nextSecond, untilEnd) else nextSecond
        return delay.coerceAtLeast(10L)
    }
}
