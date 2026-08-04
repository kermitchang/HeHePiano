package com.ruxor.kermitpiano.core.song

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.timeline.SongTime

internal data class Song(
    val id: String,
    val title: String,
    val duration: SongTime,
    val notes: List<SongNote>,
) {
    init {
        require(id.isNotBlank()) { "Song id must not be blank." }
        require(title.isNotBlank()) { "Song title must not be blank." }
        require(notes.zipWithNext().all { (first, second) -> first.songTime <= second.songTime }) {
            "Song notes must be sorted by song time."
        }
        require(notes.all { note -> note.songTime <= duration }) {
            "Song notes must not start after the song duration."
        }
    }
}

internal data class SongNote(
    val note: MidiNote,
    val songTime: SongTime,
)
