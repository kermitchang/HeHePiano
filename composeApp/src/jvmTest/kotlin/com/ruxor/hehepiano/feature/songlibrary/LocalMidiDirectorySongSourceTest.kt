package com.ruxor.hehepiano.feature.songlibrary

import com.ruxor.hehepiano.feature.midi.MidiImportPolicy
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalMidiDirectorySongSourceTest {
    @Test
    fun `local source scans only midi extensions`(): Unit = runBlocking {
        val directory = Files.createTempDirectory("kermit-midi-library").toFile()
        Files.write(directory.toPath().resolve("a.mid"), byteArrayOf(1))
        Files.write(directory.toPath().resolve("b.midi"), byteArrayOf(2))
        Files.write(directory.toPath().resolve("ignore.txt"), byteArrayOf(3))

        val songs = LocalMidiDirectorySongSource(directory).listSongs()

        assertEquals(listOf("a.mid", "b.midi"), songs.map { it.name })
        check(directory.deleteRecursively())
    }

    @Test
    fun `local source rejects files over the import limit before reading`(): Unit = runBlocking {
        val directory = Files.createTempDirectory("kermit-midi-library-limit").toFile()
        val file = directory.resolve("large.mid")
        RandomAccessFile(file, "rw").use { it.setLength(MidiImportPolicy.MAX_FILE_BYTES.toLong() + 1) }

        try {
            assertFailsWith<IllegalArgumentException> {
                LocalMidiDirectorySongSource(directory).load(
                    SongFile(file.name, file.name, file.length(), file.lastModified()),
                )
            }
        } finally {
            check(directory.deleteRecursively())
        }
    }
}
