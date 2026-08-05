package com.ruxor.kermitpiano.feature.audio

import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A single interactive FluidSynth process; it is never created from a note event. */
internal class FluidSynthPianoAudioEngine(
    private val executable: String = "fluidsynth",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val executableLocator: (String) -> String? = { command -> FluidSynthExecutableLocator.locate(command) },
    private val processLauncher: (List<String>) -> Process = { command -> ProcessBuilder(command).start() },
) : PianoAudioEngine {
    private val mutableState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    override val state: StateFlow<AudioEngineState> = mutableState.asStateFlow()
    private val mutableDiagnostics = MutableStateFlow(AudioEngineDiagnostics(backend = "FluidSynth process"))
    override val diagnostics: StateFlow<AudioEngineDiagnostics> = mutableDiagnostics.asStateFlow()
    private var process: Process? = null
    private var commandWriter: BufferedWriter? = null
    private var backgroundScope: CoroutineScope? = null
    private var closing = false
    private val activeNotes = mutableSetOf<Pair<Int, Int>>()
    private val lock = Any()

    override suspend fun initialize(config: PianoAudioConfig) = withContext(ioDispatcher) {
        close()
        config.validationError()?.let { error -> mutableState.value = AudioEngineState.Error(error); return@withContext }
        val soundFont = config.soundFontPath?.let(::File)?.absoluteFile
        if (soundFont == null || !soundFont.isFile || !soundFont.canRead() || !soundFont.extension.equals("sf2", true) || soundFont.length() <= 0) {
            fail("SoundFont Missing", soundFont?.absolutePath)
            return@withContext
        }
        val executablePath = executableLocator(executable)
        if (executablePath == null) {
            fail("FluidSynth Missing", soundFont.absolutePath)
            return@withContext
        }
        mutableState.value = AudioEngineState.Initializing
        var startedProcess: Process? = null
        try {
            val command = buildList {
                add(executablePath)
                when {
                    System.getProperty("os.name").contains("mac", ignoreCase = true) -> {
                        add("-a"); add("coreaudio")
                    }
                    System.getProperty("os.name").contains("linux", ignoreCase = true) -> {
                        // Linux: prefer PipeWire if the pulse server is present, else plain ALSA.
                        add("-a")
                        add(if (detectPipeWireAvailable()) "pipewire" else "alsa")
                    }
                }
                add("-g")
                add(config.gain.toString())
                add("-r")
                add(config.sampleRate.toString())
                add(soundFont.absolutePath)
            }
            val started = processLauncher(command)
            startedProcess = started
            synchronized(lock) {
                process = started
                commandWriter = BufferedWriter(OutputStreamWriter(started.outputStream))
                backgroundScope = CoroutineScope(SupervisorJob() + ioDispatcher)
            }
            mutableDiagnostics.value = AudioEngineDiagnostics(
                backend = "FluidSynth process",
                executablePath = executablePath,
                soundFontPath = soundFont.absolutePath,
                command = command,
                processId = started.pid(),
            )
            val scope = synchronized(lock) { checkNotNull(backgroundScope) }
            scope.launch { captureStderr(started) }
            sendCommand("select 0 1 ${config.bank} ${config.program}")
            delay(150)
            if (!started.isAlive) {
                fail("FluidSynth exited during initialization (${started.exitValue()})", soundFont.absolutePath, started.exitValue())
                discardProcessAfterFailure(started)
                return@withContext
            }
            scope.launch { watchForExit(started) }
            synchronized(lock) {
                if (process !== started || closing) return@withContext
                mutableState.value = AudioEngineState.Ready("FluidSynth process", soundFont.absolutePath)
            }
        } catch (exception: java.io.IOException) {
            fail("FluidSynth Missing: ${exception.message}", soundFont.absolutePath)
            discardProcessAfterFailure(startedProcess)
        } catch (exception: Exception) {
            fail("Audio Error: ${exception.message}", soundFont.absolutePath)
            discardProcessAfterFailure(startedProcess)
        }
    }

    override fun noteOn(note: Int, velocity: Int, channel: Int) {
        synchronized(lock) {
            if (mutableState.value !is AudioEngineState.Ready || closing) return
            activeNotes += channel to note
        }
        sendCommand("noteon $channel $note ${velocity.coerceIn(0, 127)}")
    }

    override fun noteOff(note: Int, channel: Int) {
        synchronized(lock) {
            activeNotes -= channel to note
        }
        sendCommand("noteoff $channel $note")
    }

    override fun controlChange(controller: Int, value: Int, channel: Int) {
        sendCommand("cc $channel ${controller.coerceIn(0, 127)} ${value.coerceIn(0, 127)}")
    }

    override fun pitchBend(value: Int, channel: Int) {
        // FluidSynth bend expects a 14-bit value in 0..16383 (8192 = centre).
        sendCommand("bend $channel ${value.coerceIn(0, 16_383)}")
    }

    override fun allNotesOff() {
        val notes = synchronized(lock) {
            if (process?.isAlive != true) {
                activeNotes.clear()
                return
            }
            activeNotes.toList().also { activeNotes.clear() }
        }
        notes.forEach { (channel, note) -> sendCommand("noteoff $channel $note") }
        (0..15).forEach { channel -> sendCommand("cc $channel 123 0") }
    }

    override suspend fun close() = withContext(ioDispatcher) {
        val started = synchronized(lock) {
            closing = true
            process
        }
        allNotesOff()
        runCatching { sendCommand("quit") }

        val writer = synchronized(lock) {
            val currentWriter = commandWriter
            commandWriter = null
            process = null
            activeNotes.clear()
            currentWriter
        }
        runCatching { writer?.close() }
        started?.let(::terminate)

        synchronized(lock) {
            backgroundScope?.cancel()
            backgroundScope = null
            mutableState.value = AudioEngineState.Uninitialized
            closing = false
        }
    }

    private fun sendCommand(command: String) {
        synchronized(lock) {
            runCatching {
                commandWriter?.apply { write(command); newLine(); flush() }
            }.onFailure { fail("Audio command failed: ${it.message}", mutableDiagnostics.value.soundFontPath) }
        }
    }

    private fun watchForExit(started: Process) {
        val exitCode = started.waitFor()
        val shouldReport = synchronized(lock) {
            process === started && !closing && mutableState.value is AudioEngineState.Ready
        }
        if (shouldReport) {
            fail("FluidSynth exited unexpectedly ($exitCode)", mutableDiagnostics.value.soundFontPath, exitCode)
            discardProcessAfterFailure(started)
        }
    }

    private fun detectPipeWireAvailable(): Boolean =
        runCatching {
            val runtimeDir = System.getenv("XDG_RUNTIME_DIR")
            if (runtimeDir.isNullOrBlank()) return@runCatching false
            val socket = File(runtimeDir, "pipewire-0")
            socket.exists()
        }.getOrDefault(false)

    private fun captureStderr(started: Process) {
        val stderr = started.errorStream.bufferedReader().use { it.readText().trim() }
        if (stderr.isNotBlank()) {
            synchronized(lock) {
                if (process === started && !closing) {
                    mutableDiagnostics.value = mutableDiagnostics.value.copy(stderr = stderr)
                }
            }
        }
    }

    private fun fail(message: String, soundFontPath: String?, exitCode: Int? = null) {
        synchronized(lock) {
            if (closing) return
            println("KermitPiano FluidSynth failure: message=$message soundFont=$soundFontPath exitCode=$exitCode stderr=${mutableDiagnostics.value.stderr}")
            mutableState.value = AudioEngineState.Error(message)
            mutableDiagnostics.value = mutableDiagnostics.value.copy(
                soundFontPath = soundFontPath ?: mutableDiagnostics.value.soundFontPath,
                processExitCode = exitCode,
                lastError = message,
            )
        }
    }

    private fun terminate(started: Process) {
        if (!started.isAlive) return
        runCatching {
            started.destroy()
            if (!started.waitFor(500, TimeUnit.MILLISECONDS)) {
                started.destroyForcibly()
                started.waitFor(500, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun discardProcessAfterFailure(expected: Process?) {
        val writer = synchronized(lock) {
            if (expected != null && process !== expected) return
            closing = true
            val currentWriter = commandWriter
            commandWriter = null
            process = null
            activeNotes.clear()
            backgroundScope?.cancel()
            backgroundScope = null
            currentWriter
        }
        runCatching { writer?.close() }
        expected?.let(::terminate)
        synchronized(lock) { closing = false }
    }
}
