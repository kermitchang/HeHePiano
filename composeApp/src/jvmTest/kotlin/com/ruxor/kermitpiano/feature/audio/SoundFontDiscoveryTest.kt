package com.ruxor.kermitpiano.feature.audio

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundFontDiscoveryTest {
    @Test
    fun `configured soundfont takes priority over default and directory entries`(): Unit {
        val directory = Files.createTempDirectory("kermit-soundfonts").toFile()
        val default = File(directory, "piano.sf2").apply { writeBytes(byteArrayOf(1)) }
        val configured = File(directory, "chosen.sf2").apply { writeBytes(byteArrayOf(2)) }

        assertEquals(configured.absolutePath, SoundFontDiscovery.find(configured.absolutePath, directory))
        assertEquals(default.absolutePath, SoundFontDiscovery.find(null, directory))
        check(directory.deleteRecursively())
    }
}
