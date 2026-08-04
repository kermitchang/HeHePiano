package com.ruxor.kermitpiano.feature.audio

import kotlinx.coroutines.flow.StateFlow

internal interface PianoAudioEngine : AudioDiagnosticsProvider {
    val state: StateFlow<AudioEngineState>

    suspend fun initialize(config: PianoAudioConfig)
    fun noteOn(note: Int, velocity: Int, channel: Int = 0)
    fun noteOff(note: Int, channel: Int = 0)
    fun controlChange(controller: Int, value: Int, channel: Int = 0)
    fun pitchBend(value: Int, channel: Int = 0)
    fun allNotesOff()
    suspend fun close()
}

internal sealed interface AudioEngineState {
    data object Uninitialized : AudioEngineState
    data object Initializing : AudioEngineState
    data class Ready(val backend: String, val soundFontPath: String) : AudioEngineState
    data class Error(val message: String) : AudioEngineState
}

internal data class PianoAudioConfig(
    val soundFontPath: String? = null,
    val bank: Int = 0,
    val program: Int = 0,
    val gain: Float = 0.8f,
    val sampleRate: Int = 44_100,
    val bufferSize: Int = 256,
) {
    fun validationError(): String? = when {
        bank !in 0..16_383 -> "Bank must be between 0 and 16383."
        program !in 0..127 -> "Program must be between 0 and 127."
        gain !in 0f..2f -> "Gain must be between 0 and 2."
        sampleRate !in 8_000..192_000 -> "Sample rate is outside the supported range."
        bufferSize !in 32..4_096 -> "Buffer size is outside the supported range."
        else -> null
    }
}
