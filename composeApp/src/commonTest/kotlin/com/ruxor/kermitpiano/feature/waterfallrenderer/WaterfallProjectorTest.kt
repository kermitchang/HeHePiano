package com.ruxor.kermitpiano.feature.waterfallrenderer

import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WaterfallProjectorTest {
    @Test
    fun `waterfall position is projected from song time without game time`() {
        val projector = WaterfallProjector(pixelsPerSecond = 80f)

        val position = projector.positionAt(
            noteTime = SongTime(3.seconds),
            songTime = SongTime(5.seconds),
        )

        assertEquals(WaterfallPosition(160f), position)
    }

    @Test
    fun `note span height follows MIDI duration`() {
        val projector = WaterfallProjector(pixelsPerSecond = 80f)

        val span = projector.spanAt(
            noteTime = SongTime(3.seconds),
            duration = 2.seconds,
            songTime = SongTime(5.seconds),
        )

        assertEquals(WaterfallNoteSpan(topY = 0f, bottomY = 160f), span)
    }
}
