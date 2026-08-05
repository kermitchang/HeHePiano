package com.ruxor.hehepiano.feature.playback

import com.ruxor.hehepiano.core.timeline.PlaybackSpeed

internal sealed interface PlaybackAction {
    data object Play : PlaybackAction

    data object Pause : PlaybackAction

    data object Restart : PlaybackAction

    data class SetSpeed(val speed: PlaybackSpeed) : PlaybackAction
}
