package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.MidiFileSelection

internal fun requestMidiImport(
    openMidiFile: () -> MidiFileSelection?,
    onFileSelected: (MidiFileSelection) -> Unit,
) {
    openMidiFile()?.let(onFileSelected)
}
