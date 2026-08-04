package com.ruxor.kermitpiano.feature.song

import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class DemoSongRepositoryTest {
    @Test
    fun `provides the demo song and chromatic alignment test`() {
        val songs = DemoSongRepository().availableSongs()

        assertEquals(2, songs.size)
        assertEquals("demo-do-re-mi", songs.first().id)
        assertEquals("Do Re Mi", songs.first().title)
        assertEquals(SongTime(8.seconds), songs.first().duration)
    }

    @Test
    fun `provides Do Re Mi Fa Sol La Si Do in ascending order`() {
        val song = DemoSongRepository().availableSongs().first()

        assertEquals(
            listOf(60, 62, 64, 65, 67, 69, 71, 72),
            song.notes.map { note -> note.note.value },
        )
        assertEquals(
            listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5"),
            song.notes.map { note -> note.note.label },
        )
    }

    @Test
    fun `provides a chromatic C4 through C5 alignment test`() {
        val song = DemoSongRepository().availableSongs().single { it.id == "chromatic-alignment-test" }

        assertEquals((60..72).toList(), song.notes.map { note -> note.note.value })
    }
}
