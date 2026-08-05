package com.ruxor.hehepiano.feature.keyboardinput

import com.ruxor.hehepiano.core.music.MidiNote
import com.ruxor.hehepiano.core.playerinput.PlayerInputSource
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardInputTest {
    @Test
    fun `maps A to C4 and MIDI 60 on key down`() {
        val input = KeyboardInput()

        input.onKeyDown(PianoKeyboardKey.A)

        assertEquals(setOf(MidiNote(60)), input.state.value.pressedNotes)
        assertEquals(MidiNote(60), input.state.value.lastEvent?.note)
        assertEquals("C4", input.state.value.lastEvent?.note?.label)
        assertEquals(KeyboardEventType.KeyDown, input.state.value.lastEvent?.type)
    }

    @Test
    fun `maps the A through semicolon layout to MIDI notes at the default octave`() {
        val expectedNotes = mapOf(
            PianoKeyboardKey.A to 60,
            PianoKeyboardKey.W to 61,
            PianoKeyboardKey.S to 62,
            PianoKeyboardKey.E to 63,
            PianoKeyboardKey.D to 64,
            PianoKeyboardKey.F to 65,
            PianoKeyboardKey.T to 66,
            PianoKeyboardKey.G to 67,
            PianoKeyboardKey.Y to 68,
            PianoKeyboardKey.H to 69,
            PianoKeyboardKey.U to 70,
            PianoKeyboardKey.J to 71,
            PianoKeyboardKey.K to 72,
            PianoKeyboardKey.L to 74,
            PianoKeyboardKey.Semicolon to 76,
        )

        assertEquals(expectedNotes, PianoKeyboardKey.entries.associateWith { key -> key.midiValueAt(4) })
    }

    @Test
    fun `ignores a repeated key down until key up`() {
        val input = KeyboardInput()

        input.onKeyDown(PianoKeyboardKey.A)
        val firstKeyDown = input.state.value
        input.onKeyDown(PianoKeyboardKey.A)

        assertEquals(firstKeyDown, input.state.value)

        input.onKeyUp(PianoKeyboardKey.A)

        assertEquals(emptySet(), input.state.value.pressedNotes)
        assertEquals(KeyboardEventType.KeyUp, input.state.value.lastEvent?.type)
        assertEquals(MidiNote(60), input.state.value.lastEvent?.note)
    }

    @Test
    fun `releasing one key keeps other pressed notes`() {
        val input = KeyboardInput()
        input.onKeyDown(PianoKeyboardKey.A)
        input.onKeyDown(PianoKeyboardKey.S)

        input.onKeyUp(PianoKeyboardKey.A)

        assertEquals(setOf(MidiNote(62)), input.state.value.pressedNotes)
        assertEquals(KeyboardEvent(MidiNote(60), KeyboardEventType.KeyUp), input.state.value.lastEvent)
    }

    @Test
    fun `clearing event listener prevents callbacks after disposal`() {
        val input = KeyboardInput()
        var callbackCount = 0
        input.setEventListener { callbackCount += 1 }

        input.onKeyDown(PianoKeyboardKey.A)
        input.clearEventListener()
        input.onKeyUp(PianoKeyboardKey.A)

        assertEquals(1, callbackCount)
    }

    @Test
    fun `release all clears tracker and emits note offs to the audio listener`() {
        val input = KeyboardInput()
        val events = mutableListOf<KeyboardEvent>()
        input.setEventListener(events::add)
        input.onKeyDown(PianoKeyboardKey.A)

        input.releaseAll()

        assertEquals(listOf(KeyboardEvent(MidiNote(60), KeyboardEventType.KeyDown), KeyboardEvent(MidiNote(60), KeyboardEventType.KeyUp)), events)
        assertEquals(emptySet(), input.playerInputTracker.state.value.pressedNotes)
        assertEquals(emptySet(), input.playerInputTracker.state.value.notesBySource[PlayerInputSource.ComputerKeyboard])
    }

    @Test
    fun `disabled keyboard input ignores new keys and releases existing keys`() {
        val input = KeyboardInput()
        input.onKeyDown(PianoKeyboardKey.A)

        input.enabled = false
        input.onKeyDown(PianoKeyboardKey.S)
        input.onKeyUp(PianoKeyboardKey.A)

        assertEquals(emptySet(), input.state.value.pressedNotes)
        assertEquals(emptySet(), input.playerInputTracker.state.value.pressedNotes)
        input.enabled = true
        input.onKeyDown(PianoKeyboardKey.S)
        assertEquals(setOf(MidiNote(62)), input.state.value.pressedNotes)
    }

    @Test
    fun `octave controls shift keyboard notes by twelve semitones within piano bounds`() {
        val input = KeyboardInput()

        input.octaveUp()
        input.onKeyDown(PianoKeyboardKey.A)

        assertEquals(5, input.state.value.octave)
        assertEquals(72..88, input.state.value.midiRange)
        assertEquals(setOf(MidiNote(72)), input.state.value.pressedNotes)

        input.onKeyUp(PianoKeyboardKey.A)
        repeat(10) { input.octaveDown() }
        input.onKeyDown(PianoKeyboardKey.A)

        assertEquals(1, input.state.value.octave)
        assertEquals(24..40, input.state.value.midiRange)
        assertEquals(setOf(MidiNote(24)), input.state.value.pressedNotes)
    }
}
