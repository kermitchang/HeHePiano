package com.ruxor.kermitpiano.core.song

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    val duration: Duration = DEFAULT_DURATION,
    val velocity: Int = DEFAULT_VELOCITY,
    val channel: Int = DEFAULT_CHANNEL,
    val hand: PianoHand = PianoHand.Right,
) {
    init {
        require(duration.isFinite() && duration > Duration.ZERO) {
            "Song note duration must be finite and positive."
        }
        require(velocity in 1..127) { "Song note velocity must be between 1 and 127." }
        require(channel in 0..15) { "Song note channel must be between 0 and 15." }
    }

    val endTime: SongTime
        get() = SongTime(songTime.elapsed + duration)

    private companion object {
        val DEFAULT_DURATION = 250.milliseconds
        const val DEFAULT_VELOCITY = 96
        const val DEFAULT_CHANNEL = 0
    }
}
