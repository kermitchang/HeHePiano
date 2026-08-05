package com.ruxor.hehepiano.feature.autoplay

import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.song.Song
import com.ruxor.hehepiano.core.song.SongNote
import com.ruxor.hehepiano.core.song.PianoHand
import com.ruxor.hehepiano.core.timeline.SongTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AutoPlaySchedulerTest {
    @Test
    fun `start emits notes at the current time and advance emits crossed events`() {
        val scheduler = AutoPlayScheduler()
        scheduler.load(
            song(
                SongNote(MidiNote(60), SongTime.zero, duration = 1.seconds, velocity = 80),
                SongNote(MidiNote(64), SongTime(500.milliseconds), duration = 1.seconds, velocity = 90),
            ),
        )

        assertEquals(
            listOf(AutoPlayEffect.NoteOn(MidiNote(60), 80, 0)),
            scheduler.startAt(SongTime.zero),
        )
        assertEquals(
            listOf(AutoPlayEffect.NoteOn(MidiNote(64), 90, 0)),
            scheduler.advance(SongTime.zero, SongTime(500.milliseconds)).effects,
        )
        assertEquals(
            listOf(AutoPlayEffect.NoteOff(MidiNote(60), 0)),
            scheduler.advance(SongTime(500.milliseconds), SongTime(1.seconds)).effects,
        )
    }

    @Test
    fun `overlapping same pitch waits for the final note off`() {
        val scheduler = AutoPlayScheduler()
        scheduler.load(
            song(
                SongNote(MidiNote(60), SongTime.zero, duration = 2.seconds),
                SongNote(MidiNote(60), SongTime(1.seconds), duration = 2.seconds),
            ),
        )

        scheduler.startAt(SongTime.zero)
        assertEquals(
            listOf(AutoPlayEffect.NoteOn(MidiNote(60), 96, 0)),
            scheduler.advance(SongTime.zero, SongTime(1.seconds)).effects,
        )
        assertEquals(emptyList(), scheduler.advance(SongTime(1.seconds), SongTime(2.seconds)).effects)
        assertEquals(
            listOf(AutoPlayEffect.NoteOff(MidiNote(60), 0)),
            scheduler.advance(SongTime(2.seconds), SongTime(3.seconds)).effects,
        )
    }

    @Test
    fun `start in the middle retriggers notes that are currently held`() {
        val scheduler = AutoPlayScheduler()
        scheduler.load(song(SongNote(MidiNote(67), SongTime.zero, duration = 3.seconds, velocity = 70)))

        assertEquals(
            listOf(AutoPlayEffect.NoteOn(MidiNote(67), 70, 0)),
            scheduler.startAt(SongTime(1.seconds)),
        )
        assertEquals(listOf(AutoPlayEffect.AllNotesOff), scheduler.pause())
    }

    @Test
    fun `timeline wrap completes playback and releases all notes`() {
        val scheduler = AutoPlayScheduler()
        scheduler.load(
            song(
                SongNote(
                    MidiNote(72),
                    SongTime(3.seconds + 500.milliseconds),
                    duration = 500.milliseconds,
                ),
            ),
        )
        scheduler.startAt(SongTime.zero)

        val result = scheduler.advance(SongTime(3.seconds), SongTime(100.milliseconds))

        assertTrue(result.completed)
        assertEquals(
            listOf(
                AutoPlayEffect.NoteOn(MidiNote(72), 96, 0),
                AutoPlayEffect.NoteOff(MidiNote(72), 0),
                AutoPlayEffect.AllNotesOff,
            ),
            result.effects,
        )
    }

    @Test
    fun `scheduler only emits notes from included hands`() {
        val scheduler = AutoPlayScheduler()
        scheduler.load(
            song(
                SongNote(MidiNote(48), SongTime.zero, hand = PianoHand.Left),
                SongNote(MidiNote(72), SongTime.zero, hand = PianoHand.Right),
            ),
            includedHands = setOf(PianoHand.Right),
        )

        assertEquals(
            listOf(AutoPlayEffect.NoteOn(MidiNote(72), 96, 0)),
            scheduler.startAt(SongTime.zero),
        )
    }

    private fun song(vararg notes: SongNote): Song = Song(
        id = "auto-play-test",
        title = "Auto Play Test",
        duration = SongTime(4.seconds),
        notes = notes.sortedBy { it.songTime },
    )
}
