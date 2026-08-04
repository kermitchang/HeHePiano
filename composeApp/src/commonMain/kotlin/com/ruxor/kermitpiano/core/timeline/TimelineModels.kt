package com.ruxor.kermitpiano.core.timeline

import kotlin.jvm.JvmInline
import kotlin.time.Duration

@JvmInline
internal value class GameTime(val elapsed: Duration) : Comparable<GameTime> {
    init {
        require(elapsed.isFinite() && elapsed >= Duration.ZERO) {
            "Game time must be finite and non-negative."
        }
    }

    override fun compareTo(other: GameTime): Int = elapsed.compareTo(other.elapsed)
}

@JvmInline
internal value class SongTime(val elapsed: Duration) : Comparable<SongTime> {
    init {
        require(elapsed.isFinite() && elapsed >= Duration.ZERO) {
            "Song time must be finite and non-negative."
        }
    }

    override fun compareTo(other: SongTime): Int = elapsed.compareTo(other.elapsed)

    internal companion object {
        val zero = SongTime(Duration.ZERO)
    }
}

@JvmInline
internal value class PlaybackSpeed(val multiplier: Double) {
    init {
        require(multiplier.isFinite() && multiplier > 0.0) {
            "Playback speed must be finite and greater than zero."
        }
    }

    internal companion object {
        val normal = PlaybackSpeed(1.0)
    }
}

internal data class SongLoop(
    val start: SongTime,
    val endExclusive: SongTime,
) {
    init {
        require(start < endExclusive) { "A loop must have a positive duration." }
    }
}

internal enum class PlaybackState {
    Playing,
    Paused,
}

internal data class TimelineSnapshot(
    val songTime: SongTime,
    val playbackState: PlaybackState,
    val speed: PlaybackSpeed,
    val loop: SongLoop?,
)
