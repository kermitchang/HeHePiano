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

/** A single interactive FluidSynth process; it is never created from a note event. */
internal class FluidSynthPianoAudioEngine(
    private val executable: String = "fluidsynth",
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : PianoAudioEngine {
    private val mutableState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    override val state: StateFlow<AudioEngineState> = mutableState.asStateFlow()
    private var process: Process? = null
    private var commandWriter: BufferedWriter? = null
    private val activeNotes = mutableSetOf<Pair<Int, Int>>()

    override suspend fun initialize(config: PianoAudioConfig) = withContext(Dispatchers.IO) {
        config.validationError()?.let { error -> mutableState.value = AudioEngineState.Error(error); return@withContext }
        val soundFont = config.soundFontPath?.let(::File)
        if (soundFont == null || !soundFont.isFile || !soundFont.extension.equals("sf2", true)) {
            mutableState.value = AudioEngineState.Error("SoundFont Missing")
            return@withContext
        }
        mutableState.value = AudioEngineState.Initializing
        try {
            val started = ProcessBuilder(
                executable, "-i", "-g", config.gain.toString(), "-r", config.sampleRate.toString(), soundFont.absolutePath,
            ).start()
            process = started
            commandWriter = BufferedWriter(OutputStreamWriter(started.outputStream))
            sendCommand("select 0 1 ${config.bank} ${config.program}")
            ioScope.launch { watchForExit(started) }
            mutableState.value = AudioEngineState.Ready("FluidSynth process", soundFont.absolutePath)
        } catch (exception: java.io.IOException) {
            mutableState.value = AudioEngineState.Error("FluidSynth Missing: ${exception.message}")
        } catch (exception: Exception) {
            mutableState.value = AudioEngineState.Error("Audio Error: ${exception.message}")
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
            }.onFailure { mutableState.value = AudioEngineState.Error("Audio command failed: ${it.message}") }
        }
    }

    private fun watchForExit(started: Process) {
        val exitCode = started.waitFor()
        if (process === started && mutableState.value is AudioEngineState.Ready) {
            mutableState.value = AudioEngineState.Error("FluidSynth exited unexpectedly ($exitCode)")
        }
    }
}
