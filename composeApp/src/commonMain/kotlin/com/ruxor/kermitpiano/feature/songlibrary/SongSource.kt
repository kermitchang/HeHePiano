package com.ruxor.kermitpiano.feature.songlibrary

import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile

internal data class SongFile(
    val id: String,
    val name: String,
    val byteSize: Long,
    val modifiedEpochMillis: Long,
)

internal interface SongSource {
    suspend fun listSongs(): List<SongFile>
}

internal interface LoadableSongSource : SongSource {
    suspend fun load(songFile: SongFile): SelectedMidiFile
}

internal object MidiLibraryPolicy {
    fun isMidiFile(name: String): Boolean = name.endsWith(".mid", ignoreCase = true) ||
        name.endsWith(".midi", ignoreCase = true)

    fun uniqueCopyName(requestedName: String, existingNames: Set<String>): String {
        if (requestedName !in existingNames) return requestedName
        val extensionIndex = requestedName.lastIndexOf('.')
        val stem = if (extensionIndex > 0) requestedName.substring(0, extensionIndex) else requestedName
        val extension = if (extensionIndex > 0) requestedName.substring(extensionIndex) else ""
        var copyNumber = 2
        while ("$stem ($copyNumber)$extension" in existingNames) copyNumber += 1
        return "$stem ($copyNumber)$extension"
    }
}
