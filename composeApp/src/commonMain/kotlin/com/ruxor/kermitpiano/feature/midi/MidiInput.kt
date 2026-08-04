package com.ruxor.kermitpiano.feature.midi

import com.ruxor.kermitpiano.core.music.MidiNote

internal interface MidiInput {
    fun start(
        onNoteOn: (MidiNote, Int) -> Unit,
        onNoteOff: (MidiNote) -> Unit,
        onPitchBend: (Int) -> Unit = {},
        onControlChange: (Int, Int) -> Unit = { _, _ -> },
    )

    fun stop()
}
