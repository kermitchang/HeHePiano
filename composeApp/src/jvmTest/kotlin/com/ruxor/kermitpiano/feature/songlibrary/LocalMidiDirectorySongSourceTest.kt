package com.ruxor.kermitpiano.feature.songlibrary

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
