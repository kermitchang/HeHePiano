package com.ruxor.kermitpiano.feature.waterfallrenderer

import com.ruxor.kermitpiano.core.timeline.SongTime

@JvmInline
internal value class WaterfallPosition(val y: Float)

internal class WaterfallProjector(private val pixelsPerSecond: Float) {
    init {
        require(pixelsPerSecond.isFinite() && pixelsPerSecond > 0f) {
            "Pixels per second must be finite and greater than zero."
        }
    }

    fun positionAt(noteTime: SongTime, songTime: SongTime): WaterfallPosition {
        val secondsFromNote = (songTime.elapsed - noteTime.elapsed).inWholeNanoseconds / NANOS_PER_SECOND
        return WaterfallPosition(y = (secondsFromNote * pixelsPerSecond).toFloat())
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
