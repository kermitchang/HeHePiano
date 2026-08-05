package com.ruxor.kermitpiano.app

import com.ruxor.kermitpiano.feature.midi.MidiAnalysis

internal sealed interface MidiImportState {
    data object Idle : MidiImportState

    data class Analyzing(val fileName: String) : MidiImportState

    data class Ready(val analysis: MidiAnalysis) : MidiImportState

    data class Failure(val fileName: String, val message: String) : MidiImportState
}
