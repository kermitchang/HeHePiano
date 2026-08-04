package com.ruxor.kermitpiano.feature.playback

import com.ruxor.kermitpiano.core.timeline.PlaybackSpeed

internal sealed interface PlaybackAction {
    data object Play : PlaybackAction

    data object Pause : PlaybackAction

    data object Restart : PlaybackAction

    data class SetSpeed(val speed: PlaybackSpeed) : PlaybackAction
}
