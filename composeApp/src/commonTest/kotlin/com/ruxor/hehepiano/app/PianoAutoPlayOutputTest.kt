package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.playerinput.PlayerInputSource
import com.ruxor.hehepiano.core.playerinput.PlayerInputTracker
import com.ruxor.hehepiano.feature.audio.AudioEvent
import com.ruxor.hehepiano.feature.audio.FakePianoAudioEngine
import com.ruxor.hehepiano.feature.autoplay.AutoPlayEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class PianoAutoPlayOutputTest {
    @Test
    fun `routes autoplay effects to audio and visual input state`() {
        val engine = FakePianoAudioEngine()
        val tracker = PlayerInputTracker()
        val output = PianoAutoPlayOutput(engine, tracker)

        output.submit(
            listOf(
                AutoPlayEffect.NoteOn(MidiNote(60), velocity = 84, channel = 2),
                AutoPlayEffect.NoteOff(MidiNote(60), channel = 2),
            ),
        )

        assertEquals(
            listOf(
                AudioEvent.NoteOn(60, 84, 2),
                AudioEvent.NoteOff(60, 2),
            ),
            engine.events,
        )
        assertEquals(emptySet(), tracker.state.value.notesBySource[PlayerInputSource.AutoPlay])
    }

    @Test
    fun `all notes off clears autoplay notes`() {
        val engine = FakePianoAudioEngine()
        val tracker = PlayerInputTracker()
        val output = PianoAutoPlayOutput(engine, tracker)

        output.submit(listOf(AutoPlayEffect.NoteOn(MidiNote(60), 96, 0)))
        output.submit(listOf(AutoPlayEffect.AllNotesOff))

        assertEquals(
            listOf(AudioEvent.NoteOn(60, 96, 0), AudioEvent.NoteOff(60, 0)),
            engine.events,
        )
        assertEquals(emptySet(), tracker.state.value.pressedNotes)
    }
}
