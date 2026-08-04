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
}
