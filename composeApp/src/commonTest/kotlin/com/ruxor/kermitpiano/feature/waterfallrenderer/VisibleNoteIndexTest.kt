package com.ruxor.kermitpiano.feature.waterfallrenderer

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongNote
import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class VisibleNoteIndexTest {
    @Test
    fun `returns only note indices within the visible song time range`() {
        val noteIndex = VisibleNoteIndex(song())

        val visibleIndices = noteIndex.indicesBetween(
            from = SongTime(2.seconds),
            through = SongTime(4.seconds),
        )

        assertEquals(listOf(2, 3, 4), visibleIndices.toList())
    }

    @Test
    fun `returns no indices when no notes are visible`() {
        val noteIndex = VisibleNoteIndex(song())

        val visibleIndices = noteIndex.indicesBetween(
            from = SongTime(6.seconds),
            through = SongTime(7.seconds),
        )

        assertEquals(emptyList(), visibleIndices.toList())
    }

    private fun song() = Song(
        id = "visible-notes",
        title = "Visible notes",
        duration = SongTime(5.seconds),
        notes = (0..5).map { index ->
            SongNote(MidiNote(60 + index), SongTime(index.toDouble().seconds))
        },
    )
}
