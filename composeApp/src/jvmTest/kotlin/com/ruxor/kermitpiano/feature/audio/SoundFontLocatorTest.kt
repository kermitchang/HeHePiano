package com.ruxor.kermitpiano.feature.audio

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoundFontLocatorTest {
    @Test
    fun `finds project default from project root`() {
        withProject { root, _ ->
            val soundFont = root.resolve("source/soundfonts/piano.sf2").also { it.parent.createDirectories(); it.writeBytes(byteArrayOf(1)) }

            val result = SoundFontLocator.locate(null, root)

            assertEquals(soundFont.absolutePathString(), result.selectedSoundFontPath)
        }
    }

    @Test
    fun `finds project root by walking upward from compose app directory`() {
        withProject { root, appDirectory ->
            val soundFont = root.resolve("source/soundfonts/piano.sf2").also { it.parent.createDirectories(); it.writeBytes(byteArrayOf(1)) }

            val result = SoundFontLocator.locate(null, appDirectory)

            assertEquals(root.absolutePathString(), result.projectRoot)
            assertEquals(soundFont.absolutePathString(), result.selectedSoundFontPath)
        }
    }

    @Test
    fun `configured absolute path takes priority`() {
        withProject { root, _ ->
            val configured = root.resolve("configured.sf2").also { it.writeBytes(byteArrayOf(1)) }
            root.resolve("source/soundfonts/piano.sf2").also { it.parent.createDirectories(); it.writeBytes(byteArrayOf(2)) }

            assertEquals(configured.absolutePathString(), SoundFontLocator.locate(configured.absolutePathString(), root).selectedSoundFontPath)
        }
    }

    @Test
    fun `system property takes priority over environment and project candidates`() {
        withProject { root, _ ->
            val property = root.resolve("property.sf2").also { it.writeBytes(byteArrayOf(1)) }
            val environment = root.resolve("environment.sf2").also { it.writeBytes(byteArrayOf(2)) }

            val result = SoundFontLocator.locate(null, root, systemPropertyPath = property.absolutePathString(), environmentPath = environment.absolutePathString())

            assertEquals(property.absolutePathString(), result.selectedSoundFontPath)
        }
    }

    @Test
    fun `environment path takes priority over project candidate`() {
        withProject { root, _ ->
            val environment = root.resolve("environment.sf2").also { it.writeBytes(byteArrayOf(1)) }
            root.resolve("source/soundfonts/piano.sf2").also { it.parent.createDirectories(); it.writeBytes(byteArrayOf(2)) }

            assertEquals(environment.absolutePathString(), SoundFontLocator.locate(null, root, systemPropertyPath = null, environmentPath = environment.absolutePathString()).selectedSoundFontPath)
        }
    }

    @Test
    fun `invalid and empty files are rejected before first valid fallback`() {
        withProject { root, _ ->
            val directory = root.resolve("source/soundfonts").also { it.createDirectories() }
            directory.resolve("piano.sf2").writeBytes(byteArrayOf())
            val fallback = directory.resolve("valid.sf2").also { it.writeBytes(byteArrayOf(1)) }

            val result = SoundFontLocator.locate(null, root)

            assertEquals(fallback.absolutePathString(), result.selectedSoundFontPath)
            assertFalse(result.candidates.first().valid)
        }
    }

    @Test
    fun `missing soundfont returns a readable failure reason`() {
        withProject { root, _ ->
            val result = SoundFontLocator.locate(null, root)

            assertNull(result.selectedSoundFontPath)
            assertTrue(result.discoveryFailureReason!!.isNotBlank())
        }
    }

    private fun withProject(block: (Path, Path) -> Unit) {
        val root = Files.createTempDirectory("kermit-project")
        val appDirectory = root.resolve("composeApp").also { it.createDirectories() }
        root.resolve("settings.gradle.kts").writeBytes(byteArrayOf())
        try {
            block(root, appDirectory)
        } finally {
            check(root.toFile().deleteRecursively())
        }
    }
}
