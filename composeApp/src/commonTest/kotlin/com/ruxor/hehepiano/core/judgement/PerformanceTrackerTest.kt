package com.ruxor.hehepiano.core.judgement

import com.ruxor.hehepiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class PerformanceTrackerTest {
    @Test
    fun `returns perfect inside and on the perfect timing window`() {
        val tracker = tracker()

        val result = tracker.evaluate(timing(noteAt = 1_000, inputAt = 950))

        assertEquals(Judgement.Perfect, result.judgement)
        assertEquals((-50).milliseconds, result.timingOffset)
        assertEquals(ComboState(current = 1, best = 1), result.combo)
    }

    @Test
    fun `returns good after the perfect window and within the good window`() {
        val tracker = tracker()

        val result = tracker.evaluate(timing(noteAt = 1_000, inputAt = 1_120))

        assertEquals(Judgement.Good, result.judgement)
        assertEquals(120.milliseconds, result.timingOffset)
        assertEquals(ComboState(current = 1, best = 1), result.combo)
    }

    @Test
    fun `perfect and good both extend the combo`() {
        val tracker = tracker()
        tracker.evaluate(timing(noteAt = 1_000, inputAt = 1_000))

        val result = tracker.evaluate(timing(noteAt = 2_000, inputAt = 2_100))

        assertEquals(Judgement.Good, result.judgement)
        assertEquals(ComboState(current = 2, best = 2), result.combo)
    }

    @Test
    fun `returns miss outside the good window and resets combo`() {
        val tracker = tracker()
        tracker.evaluate(timing(noteAt = 1_000, inputAt = 1_000))
        tracker.evaluate(timing(noteAt = 2_000, inputAt = 2_100))

        val result = tracker.evaluate(timing(noteAt = 3_000, inputAt = 3_121))

        assertEquals(Judgement.Miss, result.judgement)
        assertEquals(121.milliseconds, result.timingOffset)
        assertEquals(ComboState(current = 0, best = 2), result.combo)
    }

    @Test
    fun `explicit missed note resets combo without a timing offset`() {
        val tracker = tracker()
        tracker.evaluate(timing(noteAt = 1_000, inputAt = 1_000))

        val result = tracker.recordMiss()

        assertEquals(Judgement.Miss, result.judgement)
        assertEquals(null, result.timingOffset)
        assertEquals(ComboState(current = 0, best = 1), result.combo)
    }

    private fun tracker() = PerformanceTracker(
        windows = JudgementWindows(
            perfect = 50.milliseconds,
            good = 120.milliseconds,
        ),
    )

    private fun timing(noteAt: Long, inputAt: Long) = NoteTiming(
        noteTime = SongTime(noteAt.milliseconds),
        inputTime = SongTime(inputAt.milliseconds),
    )
}
