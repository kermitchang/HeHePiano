package com.ruxor.hehepiano.feature.playback

import com.ruxor.hehepiano.core.timeline.GameClock
import com.ruxor.hehepiano.core.timeline.GameTime
import com.ruxor.hehepiano.core.timeline.PlaybackSpeed
import com.ruxor.hehepiano.core.timeline.PlaybackState
import com.ruxor.hehepiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PlaybackControllerTest {
    @Test
    fun `starts paused and play advances song time`() {
        val gameClock = FakeGameClock()
        val controller = PlaybackController(gameClock)

        assertEquals(PlaybackState.Paused, controller.state.value.playbackState)

        controller.dispatch(PlaybackAction.Play)
        gameClock.advanceBy(2.seconds)
        controller.onFrame()

        assertEquals(SongTime(2.seconds), controller.state.value.songTime)
    }

    @Test
    fun `pause freezes playback restart resets it and speed changes future advance`() {
        val gameClock = FakeGameClock()
        val controller = PlaybackController(gameClock)
        controller.dispatch(PlaybackAction.Play)
        gameClock.advanceBy(1.seconds)
        controller.onFrame()
        controller.dispatch(PlaybackAction.Pause)
        gameClock.advanceBy(2.seconds)
        controller.onFrame()

        assertEquals(SongTime(1.seconds), controller.state.value.songTime)

        controller.dispatch(PlaybackAction.Restart)
        controller.dispatch(PlaybackAction.SetSpeed(PlaybackSpeed(2.0)))
        controller.dispatch(PlaybackAction.Play)
        gameClock.advanceBy(1.seconds)
        controller.onFrame()

        assertEquals(SongTime(2.seconds), controller.state.value.songTime)
        assertEquals(PlaybackSpeed(2.0), controller.state.value.speed)
    }

    @Test
    fun `restart resets the playhead while preserving the paused state`() {
        val gameClock = FakeGameClock()
        val controller = PlaybackController(gameClock)
        controller.dispatch(PlaybackAction.Play)
        gameClock.advanceBy(3.seconds)
        controller.onFrame()
        controller.dispatch(PlaybackAction.Pause)

        controller.dispatch(PlaybackAction.Restart)

        assertEquals(SongTime.zero, controller.state.value.songTime)
        assertEquals(PlaybackState.Paused, controller.state.value.playbackState)
    }

    private class FakeGameClock : GameClock {
        private var current = GameTime(Duration.ZERO)

        override fun now(): GameTime = current

        fun advanceBy(duration: Duration) {
            current = GameTime(current.elapsed + duration)
        }
    }
}
