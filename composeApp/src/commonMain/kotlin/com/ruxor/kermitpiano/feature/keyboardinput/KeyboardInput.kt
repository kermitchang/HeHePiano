package com.ruxor.kermitpiano.feature.keyboardinput

import com.ruxor.kermitpiano.core.music.MidiNote
import com.ruxor.kermitpiano.core.playerinput.PlayerInputSource
import com.ruxor.kermitpiano.core.playerinput.PlayerInputTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class KeyboardInput(
    val playerInputTracker: PlayerInputTracker = PlayerInputTracker(),
) {
    private val pressedKeys = linkedMapOf<PianoKeyboardKey, MidiNote>()
    private var octave = DEFAULT_KEYBOARD_OCTAVE
    private val mutableState = MutableStateFlow(KeyboardInputState())

    val state: StateFlow<KeyboardInputState> = mutableState.asStateFlow()
    private var eventListener: (KeyboardEvent) -> Unit = {}

    var enabled: Boolean = true
        set(value) {
            if (field && !value) releaseAll()
            field = value
        }

    fun setEventListener(listener: (KeyboardEvent) -> Unit) {
        eventListener = listener
    }

    fun clearEventListener() {
        eventListener = {}
    }

    fun onKeyDown(key: PianoKeyboardKey) {
        if (!enabled) return
        if (key in pressedKeys) return
        val note = MidiNote(key.midiValueAt(octave))
        pressedKeys[key] = note
        playerInputTracker.noteOn(PlayerInputSource.ComputerKeyboard, note)

        publish(note, KeyboardEventType.KeyDown)
    }

    fun onKeyUp(key: PianoKeyboardKey) {
        if (!enabled) return
        val note = pressedKeys.remove(key) ?: return
        playerInputTracker.noteOff(PlayerInputSource.ComputerKeyboard, note)

        publish(note, KeyboardEventType.KeyUp)
    }

    fun releaseAll() {
        val notes = pressedKeys.values.toList()
        pressedKeys.clear()
        notes.forEach { note ->
            playerInputTracker.noteOff(PlayerInputSource.ComputerKeyboard, note)
            eventListener(KeyboardEvent(note = note, type = KeyboardEventType.KeyUp))
        }
        mutableState.value = mutableState.value.copy(
            pressedNotes = emptySet(),
            lastEvent = notes.lastOrNull()?.let { note -> KeyboardEvent(note, KeyboardEventType.KeyUp) },
            midiRange = midiRange(),
        )
    }

    fun octaveDown() {
        if (octave == MIN_KEYBOARD_OCTAVE) return
        octave -= 1
        publishState()
    }

    fun octaveUp() {
        if (octave == MAX_KEYBOARD_OCTAVE) return
        octave += 1
        publishState()
    }

    private fun publish(note: MidiNote, type: KeyboardEventType) {
        val event = KeyboardEvent(note = note, type = type)
        mutableState.value = KeyboardInputState(
            pressedNotes = pressedKeys.values.toSet(),
            lastEvent = event,
            octave = octave,
            midiRange = midiRange(),
        )
        eventListener(event)
    }

    private fun publishState() {
        mutableState.value = mutableState.value.copy(
            octave = octave,
            midiRange = midiRange(),
        )
    }

    private fun midiRange(): IntRange =
        PianoKeyboardKey.entries.minOf { key -> key.midiValueAt(octave) }..
            PianoKeyboardKey.entries.maxOf { key -> key.midiValueAt(octave) }

    private companion object {
        const val DEFAULT_KEYBOARD_OCTAVE = 4
        const val MIN_KEYBOARD_OCTAVE = 1
        const val MAX_KEYBOARD_OCTAVE = 6
    }
}

internal data class KeyboardInputState(
    val pressedNotes: Set<MidiNote> = emptySet(),
    val lastEvent: KeyboardEvent? = null,
    val octave: Int = 4,
    val midiRange: IntRange = 60..76,
)

internal data class KeyboardEvent(
    val note: MidiNote,
    val type: KeyboardEventType,
)

internal enum class KeyboardEventType {
    KeyDown,
    KeyUp,
}

internal enum class PianoKeyboardKey(private val semitoneOffset: Int) {
    A(0),
    W(1),
    S(2),
    E(3),
    D(4),
    F(5),
    T(6),
    G(7),
    Y(8),
    H(9),
    U(10),
    J(11),
    K(12),
    L(14),
    Semicolon(16);

    fun midiValueAt(octave: Int): Int = (octave + 1) * 12 + semitoneOffset
}
