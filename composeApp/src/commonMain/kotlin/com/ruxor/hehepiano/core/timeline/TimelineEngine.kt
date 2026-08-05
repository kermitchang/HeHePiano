package com.ruxor.hehepiano.core.timeline

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

internal class TimelineEngine(
    initialGameTime: GameTime,
    initialSongTime: SongTime = SongTime.zero,
    initialSpeed: PlaybackSpeed = PlaybackSpeed.normal,
    initialLoop: SongLoop? = null,
) {
    private var lastGameTime = initialGameTime
    private var songTime = normalize(initialSongTime, initialLoop)
    private var playbackState = PlaybackState.Playing
    private var speed = initialSpeed
    private var loop = initialLoop

    fun snapshot(): TimelineSnapshot = TimelineSnapshot(
        songTime = songTime,
        playbackState = playbackState,
        speed = speed,
        loop = loop,
    )

    fun advanceTo(gameTime: GameTime): TimelineSnapshot {
        require(gameTime >= lastGameTime) { "Game time must be monotonic." }

        val gameDelta = gameTime.elapsed - lastGameTime.elapsed
        lastGameTime = gameTime

        if (playbackState == PlaybackState.Playing) {
            val songDelta = gameDelta * speed.multiplier
            songTime = advance(songTime, songDelta, loop)
        }

        return snapshot()
    }

    fun pause(at: GameTime): TimelineSnapshot {
        advanceTo(at)
        playbackState = PlaybackState.Paused
        return snapshot()
    }

    fun resume(at: GameTime): TimelineSnapshot {
        advanceTo(at)
        playbackState = PlaybackState.Playing
        return snapshot()
    }

    fun restart(at: GameTime): TimelineSnapshot {
        advanceTo(at)
        songTime = SongTime.zero
        return snapshot()
    }

    fun setSpeed(speed: PlaybackSpeed, at: GameTime): TimelineSnapshot {
        advanceTo(at)
        this.speed = speed
        return snapshot()
    }

    fun setLoop(loop: SongLoop?, at: GameTime): TimelineSnapshot {
        advanceTo(at)
        this.loop = loop
        songTime = normalize(songTime, loop)
        return snapshot()
    }

    private companion object {
        fun advance(current: SongTime, delta: Duration, loop: SongLoop?): SongTime {
            return normalize(SongTime(current.elapsed + delta), loop)
        }

        fun normalize(time: SongTime, loop: SongLoop?): SongTime {
            if (loop == null || time < loop.endExclusive) return time

            val loopDuration = loop.endExclusive.elapsed - loop.start.elapsed
            val elapsedInLoop = time.elapsed - loop.start.elapsed
            val wrappedNanoseconds = elapsedInLoop.inWholeNanoseconds % loopDuration.inWholeNanoseconds
            return SongTime(loop.start.elapsed + wrappedNanoseconds.nanoseconds)
        }
    }
}
