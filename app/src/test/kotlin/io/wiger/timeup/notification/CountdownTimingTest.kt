package io.wiger.timeup.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTimingTest {

    @Test
    fun remainingIsNeverNegative() {
        assertEquals(1_000L, CountdownTiming.remainingMillis(2_000L, 1_000L))
        assertEquals(0L, CountdownTiming.remainingMillis(2_000L, 2_000L))
        assertEquals(0L, CountdownTiming.remainingMillis(2_000L, 3_000L))
    }

    @Test
    fun nextTickCanTargetAlertEndBeforeNextSecond() {
        assertEquals(1_000L, CountdownTiming.nextTickDelay(12_000L, 13_000L))
        assertEquals(800L, CountdownTiming.nextTickDelay(12_200L, 13_200L))
        assertEquals(200L, CountdownTiming.nextTickDelay(13_000L, 13_200L))
    }

    @Test
    fun nextTickIsBoundedAfterExpiry() {
        assertEquals(10L, CountdownTiming.nextTickDelay(13_500L, 13_200L))
    }

    @Test
    fun progressIsClamped() {
        val start = CountdownTiming.progress(3_000L, 3_000L)
        assertEquals(3, start.maxSeconds)
        assertEquals(0, start.currentSeconds)

        val finished = CountdownTiming.progress(3_000L, 0L)
        assertEquals(3, finished.maxSeconds)
        assertEquals(3, finished.currentSeconds)

        val oversizedRemaining = CountdownTiming.progress(3_000L, 5_000L)
        assertEquals(0, oversizedRemaining.currentSeconds)

        val finalSubSecond = CountdownTiming.progress(8_000L, 500L)
        assertEquals(finalSubSecond.maxSeconds, finalSubSecond.currentSeconds)
    }
}
