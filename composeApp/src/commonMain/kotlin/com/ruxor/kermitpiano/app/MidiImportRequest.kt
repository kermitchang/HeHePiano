package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile

internal fun requestMidiImport(
    openMidiFile: () -> SelectedMidiFile?,
    onFileSelected: (SelectedMidiFile) -> Unit,
) {
    openMidiFile()?.let(onFileSelected)
}
