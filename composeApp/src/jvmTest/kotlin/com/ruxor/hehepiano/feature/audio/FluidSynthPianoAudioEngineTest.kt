package com.ruxor.hehepiano.feature.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FluidSynthPianoAudioEngineTest {
    @Test
    fun `close terminates the process and prevents stale background work`() = runBlocking {
        val soundFont = Files.createTempFile("kermit", ".sf2").also { it.writeBytes(byteArrayOf(1)) }
        val process = RecordingProcess()
        val engine = FluidSynthPianoAudioEngine(
            ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            executableLocator = { "fake-fluidsynth" },
            processLauncher = { process },
        )

        try {
            engine.initialize(PianoAudioConfig(soundFontPath = soundFont.toString()))
            engine.noteOn(60, 73)
            engine.close()
            engine.close()

            assertTrue(process.destroyed)
            assertFalse(process.isAlive)
            assertTrue(process.commands().contains("noteon 0 60 73"))
            assertTrue(process.commands().contains("quit"))
            assertEquals(AudioEngineState.Uninitialized, engine.state.value)
        } finally {
            Files.deleteIfExists(soundFont)
        }
    }
}

private class RecordingProcess : Process() {
    private val terminated = CountDownLatch(1)
    private val output = ByteArrayOutputStream()
    var destroyed: Boolean = false
        private set
    private var alive: Boolean = true

    override fun getOutputStream(): OutputStream = output
    override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun waitFor(): Int {
        terminated.await()
        return 0
    }
    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = terminated.await(timeout, unit)
    override fun exitValue(): Int {
        check(!alive) { "Process is still running" }
        return 0
    }
    override fun destroy() {
        destroyed = true
        alive = false
        terminated.countDown()
    }
    override fun destroyForcibly(): Process {
        destroy()
        return this
    }
    override fun pid(): Long = 42L
    override fun isAlive(): Boolean = alive

    fun commands(): List<String> = output.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.toList()
}
