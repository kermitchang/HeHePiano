package com.ruxor.hehepiano.feature.playback

import com.ruxor.hehepiano.core.timeline.GameClock
import com.ruxor.hehepiano.core.timeline.SongLoop
import com.ruxor.hehepiano.core.timeline.TimelineEngine
import com.ruxor.hehepiano.core.timeline.TimelineSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PlaybackController(
    private val gameClock: GameClock,
    loop: SongLoop? = null,
) {
    private val timeline: TimelineEngine
    private val mutableState: MutableStateFlow<TimelineSnapshot>

    init {
        val initialGameTime = gameClock.now()
        timeline = TimelineEngine(
            initialGameTime = initialGameTime,
            initialLoop = loop,
        )
        mutableState = MutableStateFlow(timeline.pause(initialGameTime))
    }

    val state: StateFlow<TimelineSnapshot> = mutableState.asStateFlow()

    fun onFrame() {
        mutableState.value = timeline.advanceTo(gameClock.now())
    }

    fun dispatch(action: PlaybackAction) {
        val now = gameClock.now()
        mutableState.value = when (action) {
            PlaybackAction.Play -> timeline.resume(now)
            PlaybackAction.Pause -> timeline.pause(now)
            PlaybackAction.Restart -> timeline.restart(now)
            is PlaybackAction.SetSpeed -> timeline.setSpeed(action.speed, now)
        }
    }
}
