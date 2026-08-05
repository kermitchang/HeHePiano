package com.ruxor.hehepiano.core.timeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class TempoMapTest {
    @Test
    fun `tempo changes convert musical beats into song time`() {
        val tempoMap = TempoMap(
            listOf(
                TempoChange(BeatPosition(0.0), Tempo(120.0)),
                TempoChange(BeatPosition(4.0), Tempo(60.0)),
            ),
        )

        assertEquals(SongTime(2.seconds), tempoMap.songTimeAt(BeatPosition(4.0)))
        assertEquals(SongTime(4.seconds), tempoMap.songTimeAt(BeatPosition(6.0)))
    }
}
