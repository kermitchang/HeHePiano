package com.ruxor.hehepiano.core.playerinput

import com.ruxor.hehepiano.core.music.MidiNote
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerInputTrackerTest {
    @Test
    fun `same note stays pressed while another source still holds it`() {
        val tracker = PlayerInputTracker()
        val note = MidiNote(60)

        tracker.noteOn(PlayerInputSource.ComputerKeyboard, note)
        tracker.noteOn(PlayerInputSource.UsbMidi, note)
        tracker.noteOff(PlayerInputSource.ComputerKeyboard, note)

        assertEquals(setOf(note), tracker.state.value.pressedNotes)
        assertEquals(setOf(note), tracker.state.value.notesBySource[PlayerInputSource.UsbMidi])
    }

    @Test
    fun `repeated note on is reference counted and unknown note off is ignored`() {
        val tracker = PlayerInputTracker()
        val note = MidiNote(64)

        tracker.noteOn(PlayerInputSource.UsbMidi, note)
        tracker.noteOn(PlayerInputSource.UsbMidi, note)
        tracker.noteOff(PlayerInputSource.UsbMidi, note)
        assertEquals(setOf(note), tracker.state.value.pressedNotes)

        tracker.noteOff(PlayerInputSource.UsbMidi, note)
        tracker.noteOff(PlayerInputSource.UsbMidi, note)

        assertEquals(emptySet(), tracker.state.value.pressedNotes)
    }

    @Test
    fun `release all can clear one source without affecting another`() {
        val tracker = PlayerInputTracker()
        val keyboardNote = MidiNote(60)
        val midiNote = MidiNote(67)
        tracker.noteOn(PlayerInputSource.ComputerKeyboard, keyboardNote)
        tracker.noteOn(PlayerInputSource.UsbMidi, midiNote)

        tracker.releaseAll(PlayerInputSource.ComputerKeyboard)

        assertEquals(setOf(midiNote), tracker.state.value.pressedNotes)
        tracker.releaseAll()
        assertEquals(emptySet(), tracker.state.value.pressedNotes)
    }
}
