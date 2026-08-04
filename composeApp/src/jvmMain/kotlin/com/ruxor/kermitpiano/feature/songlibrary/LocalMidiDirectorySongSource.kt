package com.ruxor.kermitpiano.feature.songlibrary

import com.ruxor.kermitpiano.feature.midi.SelectedMidiFile
import java.io.File

internal class LocalMidiDirectorySongSource(private val directory: File) : LoadableSongSource {
    override suspend fun listSongs(): List<SongFile> {
        directory.mkdirs()
        return directory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && MidiLibraryPolicy.isMidiFile(it.name) }
            .sortedBy { it.name.lowercase() }
            .map(::songFile)
            .toList()
    }

    override suspend fun load(songFile: SongFile): SelectedMidiFile {
        val file = File(directory, songFile.id)
        require(file.isFile) { "The selected local MIDI file no longer exists." }
        return SelectedMidiFile(file.name, file.readBytes())
    }

    suspend fun copyToLibrary(selected: SelectedMidiFile): SongFile {
        directory.mkdirs()
        val uniqueName = MidiLibraryPolicy.uniqueCopyName(selected.name, directory.list().orEmpty().toSet())
        val target = File(directory, uniqueName)
        target.writeBytes(selected.bytes)
        return songFile(target)
    }

    private fun songFile(file: File): SongFile = SongFile(
        id = file.name,
        name = file.name,
        byteSize = file.length(),
        modifiedEpochMillis = file.lastModified(),
    )
}

internal class FilePickerSongSource(private val picker: () -> SelectedMidiFile?) : SongSource {
    override suspend fun listSongs(): List<SongFile> = emptyList()

    fun pick(): SelectedMidiFile? = picker()
}
