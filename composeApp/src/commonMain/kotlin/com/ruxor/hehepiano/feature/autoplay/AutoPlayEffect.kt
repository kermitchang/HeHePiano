package com.ruxor.hehepiano.feature.autoplay

import com.ruxor.hehepiano.core.music.MidiNote

internal sealed interface AutoPlayEffect {
    data class NoteOn(
        val note: MidiNote,
        val velocity: Int,
        val channel: Int,
    ) : AutoPlayEffect

    data class NoteOff(
        val note: MidiNote,
        val channel: Int,
    ) : AutoPlayEffect

    data object AllNotesOff : AutoPlayEffect
}

internal data class AutoPlayAdvanceResult(
    val effects: List<AutoPlayEffect> = emptyList(),
    val completed: Boolean = false,
)
