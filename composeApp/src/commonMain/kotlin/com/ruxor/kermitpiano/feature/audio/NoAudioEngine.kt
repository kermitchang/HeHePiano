package com.ruxor.kermitpiano.feature.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NoAudioEngine(private val reason: String = "Audio is unavailable.") : PianoAudioEngine {
    private val mutableState = MutableStateFlow<AudioEngineState>(AudioEngineState.Uninitialized)
    override val state: StateFlow<AudioEngineState> = mutableState.asStateFlow()

    override suspend fun initialize(config: PianoAudioConfig) {
        mutableState.value = config.validationError()?.let(AudioEngineState::Error) ?: AudioEngineState.Error(reason)
    }

    override fun noteOn(note: Int, velocity: Int, channel: Int) = Unit
    override fun noteOff(note: Int, channel: Int) = Unit
    override fun controlChange(controller: Int, value: Int, channel: Int) = Unit
    override fun allNotesOff() = Unit
    override suspend fun close() { mutableState.value = AudioEngineState.Uninitialized }
}
