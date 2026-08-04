package com.ruxor.kermitpiano.core.timeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class TimelineEngineTest {
    @Test
    fun `song time follows game time while playing`() {
        val engine = engine()

        val snapshot = engine.advanceTo(gameTime(3.0))

        assertEquals(SongTime(3.seconds), snapshot.songTime)
    }

    @Test
    fun `pause freezes song time and resume does not include paused time`() {
        val engine = engine()

        engine.pause(at = gameTime(2.0))
        assertEquals(SongTime(2.seconds), engine.advanceTo(gameTime(9.0)).songTime)
        engine.resume(at = gameTime(10.0))

        assertEquals(SongTime(3.seconds), engine.advanceTo(gameTime(11.0)).songTime)
    }

    @Test
    fun `restart returns to song start without changing playback state`() {
        val engine = engine()
        engine.advanceTo(gameTime(4.0))

        val snapshot = engine.restart(at = gameTime(5.0))

        assertEquals(SongTime.zero, snapshot.songTime)
        assertEquals(PlaybackState.Playing, snapshot.playbackState)
    }

    @Test
    fun `restart while paused preserves pause and excludes elapsed game time`() {
        val engine = engine()
        engine.advanceTo(gameTime(2.0))
        engine.pause(at = gameTime(2.0))

        val restarted = engine.restart(at = gameTime(8.0))
        val afterPausedTime = engine.advanceTo(gameTime(12.0))

        assertEquals(SongTime.zero, restarted.songTime)
        assertEquals(PlaybackState.Paused, restarted.playbackState)
        assertEquals(SongTime.zero, afterPausedTime.songTime)
    }

    @Test
    fun `speed change applies old speed before the change and new speed after it`() {
        val engine = engine()

        engine.setSpeed(PlaybackSpeed(2.0), at = gameTime(2.0))
        val snapshot = engine.advanceTo(gameTime(5.0))

        assertEquals(SongTime(8.seconds), snapshot.songTime)
    }

    @Test
    fun `loop wraps overshoot and supports crossing more than one loop`() {
        val engine = TimelineEngine(
            initialGameTime = gameTime(0.0),
            initialLoop = SongLoop(
                start = SongTime(2.seconds),
                endExclusive = SongTime(5.seconds),
            ),
        )

        val snapshot = engine.advanceTo(gameTime(12.0))

        assertEquals(SongTime(3.seconds), snapshot.songTime)
    }

    @Test
    fun `game time going backwards is rejected`() {
        val engine = engine()
        engine.advanceTo(gameTime(2.0))

        assertFailsWith<IllegalArgumentException> {
            engine.advanceTo(gameTime(1.0))
        }
    }

    private fun engine() = TimelineEngine(initialGameTime = gameTime(0.0))

    private fun gameTime(seconds: Double) = GameTime(seconds.seconds)
}
