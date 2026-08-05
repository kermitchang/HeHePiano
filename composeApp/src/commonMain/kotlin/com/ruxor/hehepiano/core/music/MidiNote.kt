package com.ruxor.hehepiano.core.music

internal data class MidiNote(val value: Int) {
    init {
        require(value in midiNoteRange) { "MIDI note must be between 0 and 127." }
    }

    val label: String
        get() = "${noteNames[value % NOTES_PER_OCTAVE]}${value / NOTES_PER_OCTAVE - 1}"

    private companion object {
        const val NOTES_PER_OCTAVE = 12
        val midiNoteRange = 0..127
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
}
