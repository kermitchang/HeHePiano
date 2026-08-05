package com.ruxor.hehepiano.feature.midi

import com.ruxor.hehepiano.core.music.MidiNote

internal interface MidiInput {
    fun start(
        onNoteOn: (MidiNote, Int) -> Unit,
        onNoteOff: (MidiNote) -> Unit,
        onPitchBend: (Int) -> Unit = {},
        onControlChange: (Int, Int) -> Unit = { _, _ -> },
    )

    fun stop()
}
