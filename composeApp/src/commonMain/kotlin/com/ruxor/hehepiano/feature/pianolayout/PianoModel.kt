package com.ruxor.hehepiano.feature.pianolayout

import com.ruxor.hehepiano.core.music.MidiNote

internal object PianoModel {
    const val firstMidi = 21
    const val lastMidi = 108
    private const val NOTES_PER_OCTAVE = 12
    private val blackSemitoneOffsets = setOf(1, 3, 6, 8, 10)

    val keys = (firstMidi..lastMidi).map { midiValue ->
        val kind = if (midiValue % NOTES_PER_OCTAVE in blackSemitoneOffsets) {
            PianoKeyKind.Black
        } else {
            PianoKeyKind.White
        }
        PianoKey(MidiNote(midiValue), kind)
    }

    val whiteKeys = keys.filter { key -> key.kind == PianoKeyKind.White }
    val blackKeys = keys.filter { key -> key.kind == PianoKeyKind.Black }

    private val keysByMidi = keys.associateBy { key -> key.note.value }

    fun keyFor(midiValue: Int): PianoKey? = keysByMidi[midiValue]

}

internal data class PianoKey(
    val note: MidiNote,
    val kind: PianoKeyKind,
)

internal enum class PianoKeyKind {
    White,
    Black,
}
