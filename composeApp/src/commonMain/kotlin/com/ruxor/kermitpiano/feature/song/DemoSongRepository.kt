package com.ruxor.kermitpiano.feature.song

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.song.Song
import com.ruxor.kermitpiano.core.song.SongNote
import com.ruxor.kermitpiano.core.song.SongRepository
import com.ruxor.kermitpiano.core.song.PianoHand
import com.ruxor.kermitpiano.core.timeline.SongTime
import kotlin.time.Duration.Companion.seconds

internal class DemoSongRepository : SongRepository {
    override fun availableSongs(): List<Song> = listOf(demoSong, chromaticAlignmentTest)

    private companion object {
        val demoSong = Song(
            id = "demo-do-re-mi",
            title = "Do Re Mi",
            duration = SongTime(8.seconds),
            notes = listOf(
                SongNote(MidiNote(60), SongTime(0.seconds), hand = PianoHand.Left),
                SongNote(MidiNote(62), SongTime(1.seconds), hand = PianoHand.Left),
                SongNote(MidiNote(64), SongTime(2.seconds), hand = PianoHand.Left),
                SongNote(MidiNote(65), SongTime(3.seconds), hand = PianoHand.Right),
                SongNote(MidiNote(67), SongTime(4.seconds), hand = PianoHand.Right),
                SongNote(MidiNote(69), SongTime(5.seconds), hand = PianoHand.Right),
                SongNote(MidiNote(71), SongTime(6.seconds), hand = PianoHand.Right),
                SongNote(MidiNote(72), SongTime(7.seconds), hand = PianoHand.Right),
            ),
        )

        val chromaticAlignmentTest = Song(
            id = "chromatic-alignment-test",
            title = "Chromatic Alignment Test",
            duration = SongTime(13.seconds),
            notes = (60..72).map { midiValue ->
                SongNote(
                    MidiNote(midiValue),
                    SongTime((midiValue - 60).seconds),
                    hand = if (midiValue < 66) PianoHand.Left else PianoHand.Right,
                )
            },
        )
    }
}
