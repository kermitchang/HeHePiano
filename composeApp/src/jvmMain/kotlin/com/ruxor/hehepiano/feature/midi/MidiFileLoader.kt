package com.ruxor.hehepiano.feature.midi

import java.io.File

/** Reads a selected MIDI file without allocating more than the import limit. */
internal fun File.readMidiBytes(): ByteArray {
    require(isFile) { "The selected MIDI file no longer exists." }
    val maxBytes = MidiImportPolicy.MAX_FILE_BYTES
    require(length() <= maxBytes) {
        "MIDI file exceeds the ${maxBytes / (1024 * 1024)} MiB limit."
    }
    return inputStream().use { input ->
        input.readNBytes(maxBytes + 1).also { bytes ->
            require(bytes.size <= maxBytes) {
                "MIDI file exceeds the ${maxBytes / (1024 * 1024)} MiB limit."
            }
        }
    }
}
