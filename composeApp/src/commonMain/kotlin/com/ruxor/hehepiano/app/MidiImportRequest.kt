package com.ruxor.hehepiano.app

import com.ruxor.hehepiano.feature.midi.MidiFileSelection

internal fun requestMidiImport(
    openMidiFile: () -> MidiFileSelection?,
    onFileSelected: (MidiFileSelection) -> Unit,
) {
    openMidiFile()?.let(onFileSelected)
}
