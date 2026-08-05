package com.ruxor.hehepiano.feature.midi

internal object MidiImportPolicy {
    const val MAX_FILE_BYTES: Int = 16 * 1024 * 1024

    fun validationError(file: SelectedMidiFile): String? = when {
        file.bytes.isEmpty() -> "MIDI file is empty."
        file.bytes.size > MAX_FILE_BYTES -> "MIDI file exceeds the ${MAX_FILE_BYTES / (1024 * 1024)} MiB limit."
        else -> null
    }
}
