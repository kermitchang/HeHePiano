package com.ruxor.kermitpiano.feature.audio

import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

/** A single interactive FluidSynth process; it is never created from a note event. */
internal class FluidSynthPianoAudioEngine(
    private val executable: String = "fluidsynth",
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : PianoAudioEngine {
    private val mutableState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    override val state: StateFlow<AudioEngineState> = mutableState.asStateFlow()
    private val mutableDiagnostics = MutableStateFlow(AudioEngineDiagnostics(backend = "FluidSynth process"))
    override val diagnostics: StateFlow<AudioEngineDiagnostics> = mutableDiagnostics.asStateFlow()
    private var process: Process? = null
    private var commandWriter: BufferedWriter? = null
    private val activeNotes = mutableSetOf<Pair<Int, Int>>()

    override suspend fun initialize(config: PianoAudioConfig) = withContext(Dispatchers.IO) {
        close()
        config.validationError()?.let { error -> mutableState.value = AudioEngineState.Error(error); return@withContext }
        val soundFont = config.soundFontPath?.let(::File)?.absoluteFile
        if (soundFont == null || !soundFont.isFile || !soundFont.canRead() || !soundFont.extension.equals("sf2", true) || soundFont.length() <= 0) {
            fail("SoundFont Missing", soundFont?.absolutePath)
            return@withContext
        }
        val executablePath = FluidSynthExecutableLocator.locate(executable)
        if (executablePath == null) {
            fail("FluidSynth Missing", soundFont.absolutePath)
            return@withContext
        }
        mutableState.value = AudioEngineState.Initializing
        try {
            val command = buildList {
                add(executablePath)
                if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
                    add("-a")
                    add("coreaudio")
                }
                add("-g")
                add(config.gain.toString())
                add("-r")
                add(config.sampleRate.toString())
                add(soundFont.absolutePath)
            }
            val started = ProcessBuilder(command).start()
            process = started
            commandWriter = BufferedWriter(OutputStreamWriter(started.outputStream))
            mutableDiagnostics.value = AudioEngineDiagnostics(
                backend = "FluidSynth process",
                executablePath = executablePath,
                soundFontPath = soundFont.absolutePath,
                command = command,
                processId = started.pid(),
            )
            ioScope.launch { captureStderr(started) }
            sendCommand("select 0 1 ${config.bank} ${config.program}")
            delay(150)
            if (!started.isAlive) {
                fail("FluidSynth exited during initialization (${started.exitValue()})", soundFont.absolutePath, started.exitValue())
                return@withContext
            }
            ioScope.launch { watchForExit(started) }
            mutableState.value = AudioEngineState.Ready("FluidSynth process", soundFont.absolutePath)
        } catch (exception: java.io.IOException) {
            fail("FluidSynth Missing: ${exception.message}", soundFont.absolutePath)
        } catch (exception: Exception) {
            fail("Audio Error: ${exception.message}", soundFont.absolutePath)
        }
    }

    override fun noteOn(note: Int, velocity: Int, channel: Int) {
        if (mutableState.value !is AudioEngineState.Ready) return
        activeNotes += channel to note
        sendCommand("noteon $channel $note ${velocity.coerceIn(0, 127)}")
    }

    override fun noteOff(note: Int, channel: Int) {
        activeNotes -= channel to note
        sendCommand("noteoff $channel $note")
    }

    override fun controlChange(controller: Int, value: Int, channel: Int) {
        sendCommand("cc $channel ${controller.coerceIn(0, 127)} ${value.coerceIn(0, 127)}")
    }

    override fun allNotesOff() {
        if (process?.isAlive != true) {
            activeNotes.clear()
            return
        }
        activeNotes.toList().forEach { (channel, note) -> sendCommand("noteoff $channel $note") }
        activeNotes.clear()
        (0..15).forEach { channel -> sendCommand("cc $channel 123 0") }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        allNotesOff()
        runCatching { sendCommand("quit") }
        commandWriter?.close()
        process?.destroy()
        commandWriter = null
        process = null
        mutableState.value = AudioEngineState.Uninitialized
    }

    private fun sendCommand(command: String) {
        synchronized(this) {
            runCatching {
                commandWriter?.apply { write(command); newLine(); flush() }
            }.onFailure { fail("Audio command failed: ${it.message}", mutableDiagnostics.value.soundFontPath) }
        }
    }

    private fun watchForExit(started: Process) {
        val exitCode = started.waitFor()
        if (process === started && mutableState.value is AudioEngineState.Ready) {
            fail("FluidSynth exited unexpectedly ($exitCode)", mutableDiagnostics.value.soundFontPath, exitCode)
        }
    }

    private fun captureStderr(started: Process) {
        val stderr = started.errorStream.bufferedReader().use { it.readText().trim() }
        if (stderr.isNotBlank() && process === started) {
            mutableDiagnostics.value = mutableDiagnostics.value.copy(stderr = stderr)
        }
    }

    private fun fail(message: String, soundFontPath: String?, exitCode: Int? = null) {
        println("KermitPiano FluidSynth failure: message=$message soundFont=$soundFontPath exitCode=$exitCode stderr=${mutableDiagnostics.value.stderr}")
        mutableState.value = AudioEngineState.Error(message)
        mutableDiagnostics.value = mutableDiagnostics.value.copy(
            soundFontPath = soundFontPath ?: mutableDiagnostics.value.soundFontPath,
            processExitCode = exitCode,
            lastError = message,
        )
    }
}
